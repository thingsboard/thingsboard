/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
