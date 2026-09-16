// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;

import javax.script.ScriptException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
public class ExceptionUtil {

    @SuppressWarnings("unchecked")
    public static <T extends Exception> T lookupException(Throwable source, Class<T> clazz) {
        Exception e = lookupExceptionInCause(source, clazz);
        if (e != null) {
            return (T) e;
        } else {
            return null;
        }
    }

    @SafeVarargs
    public static Exception lookupExceptionInCause(Throwable source, Class<? extends Exception>... clazzes) {
        while (source != null) {
            for (Class<? extends Exception> clazz : clazzes) {
                if (clazz.isAssignableFrom(source.getClass())) {
                    return (Exception) source;
                }
            }
            source = source.getCause();
        }
        return null;
    }

    public static String toString(Exception e, EntityId componentId, boolean stackTraceEnabled) {
        Exception exception = lookupExceptionInCause(e, ScriptException.class, JsonParseException.class);
        if (exception != null && StringUtils.isNotEmpty(exception.getMessage())) {
            return exception.getMessage();
        } else {
            if (stackTraceEnabled) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                return sw.toString();
            } else {
                log.debug("[{}] Unknown error during message processing", componentId, e);
                return "Please contact system administrator";
            }
        }
    }

    public static String getMessage(Throwable t) {
        String message = t.getMessage();
        if (StringUtils.isNotEmpty(message)) {
            return message;
        } else {
            return t.getClass().getSimpleName();
        }
    }

}
