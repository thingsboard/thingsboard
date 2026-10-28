// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DedicatedConverterUtil {

    public static final String TELEMETRY = "telemetry";
    public static final String VALUES = "values";
    public static final String TS = "ts";
    public static final String ATTRIBUTES = "attributes";
    public static final String DEFAULT_PROFILE = "default";

    private static final Gson GSON = new Gson();

    private DedicatedConverterUtil() {
    }

    public static DedicatedUplinkData parseUplinkData(DedicatedConverterConfig config, JsonObject src, UplinkMetaData metadata) {
        Map<String, Object> kvMap = new HashMap<>(metadata.getKvMap());

        JsonElement telemetry;
        if (src.has(TELEMETRY)) {
            telemetry = src.get(TELEMETRY);

            if (telemetry.isJsonArray()) {
                addMetadataTelemetry(telemetry.getAsJsonArray(), kvMap, config.getTelemetry());
            } else if (telemetry.isJsonObject()) {
                var obj = telemetry.getAsJsonObject();
                if (obj.has(VALUES)) {
                    var values = obj.get(VALUES).getAsJsonObject();
                    addKvs(values, kvMap, config.getTelemetry());
                } else {
                    addKvs(obj, kvMap, config.getTelemetry());
                }
            }
        } else {
            telemetry = addKvs(new JsonObject(), kvMap, config.getTelemetry());
        }

        JsonObject attributes;
        if (src.has(ATTRIBUTES)) {
            attributes = src.get(ATTRIBUTES).getAsJsonObject();
        } else {
            attributes = new JsonObject();
        }

        addKvs(attributes, kvMap, config.getAttributes());

        EntityType entityType = getProperty(src, "type", EntityType::valueOf, config::getType);
        String entityName = getProperty(src, "name", () -> processTemplate(config.getName(), kvMap));
        String profile = getProperty(src, "profile", () -> processTemplate(config.getProfile(), kvMap));
        if (profile == null) {
            profile = DEFAULT_PROFILE;
        }
        String label = getProperty(src, "label", () -> processTemplate(config.getLabel(), kvMap));
        String customer = getProperty(src, "customer", () -> processTemplate(config.getCustomer(), kvMap));
        String group = getProperty(src, "group", () -> processTemplate(config.getGroup(), kvMap));

        return DedicatedUplinkData.builder()
                .entityType(entityType)
                .name(entityName)
                .profile(profile)
                .label(label)
                .customer(customer)
                .group(group)
                .telemetry(telemetry)
                .attributes(attributes)
                .build();
    }

    private static String getProperty(JsonObject src, String key, Supplier<String> defaultValue) {
        return getProperty(src, key, Function.identity(), defaultValue);
    }

    private static <T> T getProperty(JsonObject src, String key, Function<String, T> mapper, Supplier<T> defaultValue) {
        JsonElement jsonValue = src.get(key);
        if (jsonValue != null && !jsonValue.isJsonNull()) {
            String value = src.get(key).getAsString();
            if (StringUtils.isNotEmpty(value)) {
                return mapper.apply(value);
            }
        }
        return defaultValue.get();
    }

    // The metadata keys join the script's element at the uplink ts, where the script's values win as in an object result.
    // Without such an element they get their own one at the uplink ts: an element without ts is saved at the server time.
    private static void addMetadataTelemetry(JsonArray telemetry, Map<String, Object> kvMap, Set<String> keys) {
        Long uplinkTs = getUplinkTs(kvMap);
        if (uplinkTs != null) {
            for (JsonElement element : telemetry) {
                if (element.isJsonObject() && isValuesAtTs(element.getAsJsonObject(), uplinkTs)) {
                    addKvs(element.getAsJsonObject().getAsJsonObject(VALUES), kvMap, keys);
                    return;
                }
            }
        }
        JsonObject values = addKvs(new JsonObject(), kvMap, keys);
        if (values.isEmpty()) {
            return;
        }
        if (uplinkTs == null) {
            telemetry.add(values);
            return;
        }
        JsonObject element = new JsonObject();
        element.addProperty(TS, uplinkTs);
        element.add(VALUES, values);
        telemetry.add(element);
    }

    private static boolean isValuesAtTs(JsonObject element, long ts) {
        JsonElement elementTs = element.get(TS);
        return element.has(VALUES) && element.get(VALUES).isJsonObject()
                && elementTs != null && elementTs.isJsonPrimitive() && elementTs.getAsJsonPrimitive().isNumber()
                && elementTs.getAsLong() == ts;
    }

    private static Long getUplinkTs(Map<String, Object> kvMap) {
        Object ts = kvMap.get(TS);
        if (ts instanceof Number number) {
            return number.longValue();
        }
        if (ts instanceof String str && StringUtils.isNumeric(str)) {
            return Long.parseLong(str);
        }
        return null;
    }

    private static JsonObject addKvs(JsonObject kvsObj, Map<String, Object> kvMap, Set<String> keys) {
        if (CollectionsUtil.isNotEmpty(keys) && !kvMap.isEmpty()) {
            kvMap.entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()) && !kvsObj.has(e.getKey()))
                    .forEach(e -> kvsObj.add(e.getKey(), GSON.toJsonTree(e.getValue())));
        }
        return kvsObj;
    }

    private static String processTemplate(String template, Map<String, Object> data) {
        if (StringUtils.isNotEmpty(template)) {
            String result = template;
            for (Map.Entry<String, Object> kv : data.entrySet()) {
                result = processVar(result, kv.getKey(), kv.getValue());
            }
            return result;
        }
        return null;
    }

    private static String processVar(String pattern, String key, Object val) {
        String stringValue;
        if (val instanceof String) {
            stringValue = (String) val;
        } else {
            stringValue = JacksonUtil.toString(val);
        }
        return pattern.replace(formatVarTemplate(key), stringValue);
    }

    private static String formatVarTemplate(String key) {
        return "$" + key;
    }
}
