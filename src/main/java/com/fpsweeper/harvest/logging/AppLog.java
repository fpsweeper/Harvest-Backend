package com.fpsweeper.harvest.logging;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted log entry written by DatabaseLogAppender.
 *
 * Table: app_logs
 * Queried by GET /api/admin/logs for the admin panel.
 */
@Entity
@Table(name = "app_logs", indexes = {
        @Index(name = "idx_app_logs_level",      columnList = "level"),
        @Index(name = "idx_app_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_app_logs_logger",     columnList = "logger_name"),
})
public class AppLog {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Log level: TRACE, DEBUG, INFO, WARN, ERROR */
    @Column(name = "level", nullable = false, length = 10)
    private String level;

    /** Short logger name, e.g. "BotExecutionScheduler" */
    @Column(name = "logger_name", nullable = false, length = 200)
    private String loggerName;

    /** Full formatted log message */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Stack trace if an exception was logged, otherwise null */
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    /** Thread name that produced the log */
    @Column(name = "thread_name", length = 100)
    private String threadName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ── Constructors ───────────────────────────────────────────────────────

    public AppLog() {}

    public AppLog(String level, String loggerName, String message,
                  String stackTrace, String threadName) {
        this.level      = level;
        this.loggerName = loggerName;
        this.message    = message;
        this.stackTrace = stackTrace;
        this.threadName = threadName;
        this.createdAt  = Instant.now();
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public UUID    getId()          { return id; }
    public String  getLevel()       { return level; }
    public String  getLoggerName()  { return loggerName; }
    public String  getMessage()     { return message; }
    public String  getStackTrace()  { return stackTrace; }
    public String  getThreadName()  { return threadName; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setLevel(String level)            { this.level = level; }
    public void setLoggerName(String loggerName)  { this.loggerName = loggerName; }
    public void setMessage(String message)        { this.message = message; }
    public void setStackTrace(String stackTrace)  { this.stackTrace = stackTrace; }
    public void setThreadName(String threadName)  { this.threadName = threadName; }
    public void setCreatedAt(Instant createdAt)   { this.createdAt = createdAt; }
}