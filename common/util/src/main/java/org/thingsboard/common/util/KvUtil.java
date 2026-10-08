// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static String getStringValue(Map<String, ? extends KvEntry> entries, String key, String defaultValue) {
        KvEntry entry = entries.get(key);
        if (entry == null) {
            return defaultValue;
        }
        return entry.getStrValue()
                .orElseGet(() -> entry.getJsonValue().orElse(entry.getValueAsString()));
    }

    public static Boolean getBoolValue(Map<String, ? extends KvEntry> entries, String key, Boolean defaultValue) {
        KvEntry entry = entries.get(key);
        if (entry == null) {
            return defaultValue;
        }
        return entry.getBooleanValue()
                .orElseGet(() -> {
                    String strValue = entry.getStrValue().orElse(null);
                    if (strValue != null) {
                        return Boolean.parseBoolean(strValue);
                    }
                    return defaultValue;
                });
    }

    public static Integer getIntValue(Map<String, ? extends KvEntry> entries, String key, Integer defaultValue) {
        KvEntry entry = entries.get(key);
        if (entry == null) {
            return defaultValue;
        }
        Long longValue = entry.getLongValue().orElse(null);
        if (longValue != null) {
            return longValue.intValue();
        }
        Double doubleValue = entry.getDoubleValue().orElse(null);
        if (doubleValue != null) {
            return doubleValue.intValue();
        }
        String strValue = entry.getStrValue().orElse(null);
        if (strValue != null) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static Double getDoubleValue(Map<String, ? extends KvEntry> entries, String key, Double defaultValue) {
        KvEntry entry = entries.get(key);
        if (entry == null) {
            return defaultValue;
        }
        Double doubleValue = entry.getDoubleValue().orElse(null);
        if (doubleValue != null) {
            return doubleValue;
        }
        Long longValue = entry.getLongValue().orElse(null);
        if (longValue != null) {
            return longValue.doubleValue();
        }
        String strValue = entry.getStrValue().orElse(null);
        if (strValue != null) {
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static Long getLongValue(Map<String, ? extends KvEntry> entries, String key, Long defaultValue) {
        KvEntry entry = entries.get(key);
        if (entry == null) {
            return defaultValue;
        }
        Long longValue = entry.getLongValue().orElse(null);
        if (longValue != null) {
            return longValue;
        }
        Double doubleValue = entry.getDoubleValue().orElse(null);
        if (doubleValue != null) {
            return doubleValue.longValue();
        }
        String strValue = entry.getStrValue().orElse(null);
        if (strValue != null) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static Map<String, AttributeKvEntry> toAttrMap(List<AttributeKvEntry> attributes) {
        return attributes.stream()
                .collect(Collectors.toMap(
                        AttributeKvEntry::getKey,
                        entry -> entry,
                        (existing, replacement) -> existing
                ));
    }

}
