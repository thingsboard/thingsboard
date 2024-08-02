/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Data
public class JsonPathProcessingTask {
    private final String[] tokens;
    private final Map<String, String> variables;
    private final JsonNode node;

    public JsonPathProcessingTask(String[] tokens, Map<String, String> variables, JsonNode node) {
        this.tokens = tokens;
        this.variables = variables;
        this.node = node;
    }

    public boolean isLast() {
        return tokens.length == 1;
    }

    public String currentToken() {
        return tokens[0];
    }

    public JsonPathProcessingTask next(JsonNode next) {
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    public JsonPathProcessingTask next(JsonNode next, String key, String value) {
        Map<String, String> variables = new HashMap<>(this.variables);
        variables.put(key, value);
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    @Override
    public String toString() {
        return "JsonPathProcessingTask{" +
                "tokens=" + Arrays.toString(tokens) +
                ", variables=" + variables +
                ", node=" + node.toString().substring(0, 20) +
                '}';
    }
}
