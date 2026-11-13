// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Captures the log lines one class emits, so a test can assert on what was logged rather than only on what
 * was returned - the only evidence available where the behaviour under test is a warning or a swallowed
 * failure.
 * <p>
 * The level is forced rather than inherited and restored on {@link #detach()}: left inherited, an assertion
 * would pass or fail on whatever logging configuration happened to be in effect, and a configuration that
 * filtered the level out would make a "nothing was logged" assertion pass vacuously.
 */
public final class LogCapture {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Level previousLevel;

    public LogCapture(Class<?> loggerClass, Level captureLevel) {
        this.logger = (Logger) LoggerFactory.getLogger(loggerClass);
        this.previousLevel = logger.getLevel();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(captureLevel);
    }

    /** The formatted messages captured so far, oldest first. */
    public List<String> getMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    /** Stops capturing and puts the logger back the way it was found. */
    public void detach() {
        logger.setLevel(previousLevel);
        logger.detachAppender(appender);
    }

}
