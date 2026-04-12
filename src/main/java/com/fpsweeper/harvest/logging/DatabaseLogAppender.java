package com.fpsweeper.harvest.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Custom Logback appender that writes log events to the app_logs table
 * via Spring's ApplicationContext.
 *
 * Uses a non-blocking queue so logging never slows down the calling thread.
 * A background drainer thread flushes the queue to the DB every 500ms.
 *
 * Spring context is injected via AppLogAppenderConfig (a @Component that
 * calls AppLogAppender.setApplicationContext() after startup).
 */
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int QUEUE_CAPACITY = 1000;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_STACK_LENGTH   = 8000;

    // Shared across all instances (Logback creates one per appender config)
    private static volatile ApplicationContext applicationContext;
    private static volatile AppLogRepository   repository;

    private final BlockingQueue<AppLog> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private Thread drainerThread;

    // ── Called by AppLogAppenderConfig after Spring context is ready ───────

    public static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
        try {
            repository = ctx.getBean(AppLogRepository.class);
        } catch (Exception e) {
            // Not fatal — just means we can't persist logs yet
            System.err.println("[DatabaseLogAppender] Could not get AppLogRepository: " + e.getMessage());
        }
    }

    // ── Logback lifecycle ──────────────────────────────────────────────────

    @Override
    public void start() {
        super.start();
        drainerThread = new Thread(this::drain, "log-db-drainer");
        drainerThread.setDaemon(true);
        drainerThread.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (drainerThread != null) drainerThread.interrupt();
    }

    // ── Core append ────────────────────────────────────────────────────────

    @Override
    protected void append(ILoggingEvent event) {
        if (repository == null) return;

        // Skip our own logging package to prevent infinite recursion
        String loggerName = event.getLoggerName();
        if (loggerName != null && loggerName.startsWith("com.fpsweeper.harvest.logging")) return;

        try {
            AppLog log = new AppLog(
                    event.getLevel().toString(),
                    shortLoggerName(loggerName),
                    truncate(event.getFormattedMessage(), MAX_MESSAGE_LENGTH),
                    extractStackTrace(event),
                    event.getThreadName()
            );
            queue.offer(log); // non-blocking — drops if full
        } catch (Exception ignored) {
            // Never let logging break the app
        }
    }

    // ── Background drainer ─────────────────────────────────────────────────

    private void drain() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(500);
                if (repository == null || queue.isEmpty()) continue;

                java.util.List<AppLog> batch = new java.util.ArrayList<>(50);
                queue.drainTo(batch, 50);
                if (!batch.isEmpty()) {
                    repository.saveAll(batch);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[DatabaseLogAppender] Drain error: " + e.getMessage());
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String shortLoggerName(String full) {
        if (full == null) return "unknown";
        int dot = full.lastIndexOf('.');
        return dot >= 0 ? full.substring(dot + 1) : full;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private String extractStackTrace(ILoggingEvent event) {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(proxy.getClassName()).append(": ").append(proxy.getMessage()).append("\n");
        for (StackTraceElementProxy step : proxy.getStackTraceElementProxyArray()) {
            sb.append("  at ").append(step.getSTEAsString()).append("\n");
            if (sb.length() > MAX_STACK_LENGTH) {
                sb.append("  ... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}