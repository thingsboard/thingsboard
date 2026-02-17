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

import org.thingsboard.server.common.data.kv.KvEntry;

public class KvUtil {

    public static String getStringValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(String::valueOf).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(String::valueOf).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(String::valueOf).orElse(null);
            case STRING:
                return entry.getStrValue().orElse("");
            case JSON:
                return entry.getJsonValue().orElse("");
            default:
                return null;
        }
    }

    public static Double getDoubleValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(Long::doubleValue).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(e -> e ? 1.0 : 0).orElse(null);
            case STRING:
                try {
                    return Double.parseDouble(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    public static Long getLongValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG -> {
                return entry.getLongValue().orElse(null);
            }
            case DOUBLE -> {
                return entry.getDoubleValue().map(Double::longValue).orElse(null);
            }
            case BOOLEAN -> {
                return entry.getBooleanValue().map(b -> b ? 1L : 0L).orElse(null);
            }
            case STRING -> {
                try {
                    return Long.parseLong(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            }
            case JSON -> {
                try {
                    return Long.parseLong(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            }
            default -> {
                return null;
            }
        }
    }

    public static Boolean getBoolValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(e -> e != 0).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(e -> e != 0).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().orElse(null);
            case STRING:
                try {
                    return Boolean.parseBoolean(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

}
