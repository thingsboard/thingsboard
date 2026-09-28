// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbCountUniqueIntervalState extends TbBaseIntervalState {

    private Set<String> items = ConcurrentHashMap.newKeySet();

    public TbCountUniqueIntervalState(JsonElement stateJson) {
        for (JsonElement jsonElement : stateJson.getAsJsonArray()) {
            String elementAsString = jsonElement.getAsString();
            try {
                // try to fix bug with incorrect conversion JsonElement to text value
                elementAsString = normalizeJsonString(elementAsString);
                JsonNode jsonNode = JacksonUtil.toJsonNode(elementAsString);
                items.add(JacksonUtil.writeValueAsString(jsonNode));
            } catch (Exception e) {
                items.add(elementAsString);
            }
        }
    }

    private String normalizeJsonString(String jsonString) {
        String unescapedJson = StringEscapeUtils.unescapeJson(jsonString);
        while (unescapedJson.contains("\\\"")) {
            unescapedJson = unescapedJson.replace("\\\"", "\""); // Unescape backslash and quote
        }
        return unescapedJson.replaceAll("^\"+|\"+$", "")         // Remove surrounding quotes
                .replace("\\\\", "\\");                          // Unescape backslashes
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        return items.add(data.getAsString());
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, items.size());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonArray array = new JsonArray();
        items.forEach(item -> array.add(new JsonPrimitive(item)));
        return gson.toJson(array);
    }
}
