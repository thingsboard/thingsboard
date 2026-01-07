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
package org.thingsboard.server.common.transport.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonUtils {

    private static final Pattern BASE64_PATTERN =
            Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");

    public static JsonObject getJsonObject(List<KeyValueProto> tsKv) {
        JsonObject json = new JsonObject();
        for (KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
                case JSON_V:
                    json.add(kv.getKey(), JsonParser.parseString(kv.getJsonV()));
                    break;
            }
        }
        return json;
    }

    public static JsonElement parse(Object value) {
        if (value instanceof Integer) {
            return new JsonPrimitive((Integer) value);
        } else if (value instanceof Long) {
            return new JsonPrimitive((Long) value);
        } else if (value instanceof String) {
            try {
                return JsonParser.parseString((String) value);
            } catch (Exception e) {
                if (isBase64(value.toString())) {
                    value = "\"" + value + "\"";
                }
                return JsonParser.parseString((String) value);
            }
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Double) {
            return new JsonPrimitive((Double) value);
        } else if (value instanceof Float) {
            return new JsonPrimitive((Float) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass().getSimpleName());
        }
    }

    public static JsonObject convertToJsonObject(Map<String, ?> map) {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            jsonObject.add(entry.getKey(), parse(entry.getValue()));
        }

        return jsonObject;
    }

    public static boolean isBase64(String value) {
        return value.length() % 4 == 0 && BASE64_PATTERN.matcher(value).matches();
    }
}
