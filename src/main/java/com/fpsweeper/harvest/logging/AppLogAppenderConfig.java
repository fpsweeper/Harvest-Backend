package com.fpsweeper.harvest.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * Bridges Spring's ApplicationContext into the DatabaseLogAppender.
 *
 * Logback initializes before Spring, so the appender can't use @Autowired.
 * This @Component runs after the full application context is ready and
 * injects the context so the appender can start writing to the DB.
 *
 * Also registers the appender programmatically so it doesn't need to be
 * in logback-spring.xml (avoiding the chicken-and-egg problem with DataSource).
 */
@Component
public class AppLogAppenderConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AppLogRepository appLogRepository;

    @PostConstruct
    public void init() {
        // Give the context to the appender so it can persist logs
        DatabaseLogAppender.setApplicationContext(applicationContext);

        // Register the appender programmatically on the root logger
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        // Only add if not already registered
        if (rootLogger.getAppender("DATABASE") == null) {
            DatabaseLogAppender appender = new DatabaseLogAppender();
            appender.setName("DATABASE");
            appender.setContext(loggerContext);
            appender.start();

            // Add threshold filter — only persist INFO and above
            ch.qos.logback.classic.filter.ThresholdFilter filter =
                    new ch.qos.logback.classic.filter.ThresholdFilter();
            filter.setLevel("INFO");
            filter.start();
            appender.addFilter(filter);

            rootLogger.addAppender(appender);
        }
    }
}