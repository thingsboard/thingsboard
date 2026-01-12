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
package org.thingsboard.client.tools.i18n;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.Separators.Spacing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class TranslationPruner {

    /**
     * Recursively collect all JSON keys in dot notation from the given node.
     */
    private static void collectKeys(JsonNode node, String prefix, Set<String> keys) {
        if (!node.isObject()) return;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            keys.add(fullKey);
            collectKeys(entry.getValue(), fullKey, keys);
        }
    }

    /**
     * Prune the translation ObjectNode, keeping only fields whose dot-keys are in the valid set.
     */
    private static ObjectNode pruneNode(ObjectNode node, Set<String> keys, String prefix, ObjectMapper mapper) {
        ObjectNode pruned = mapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (keys.contains(fullKey)) {
                if (value.isObject()) {
                    ObjectNode child = pruneNode((ObjectNode) value, keys, fullKey, mapper);
                    pruned.set(key, child);
                } else {
                    pruned.set(key, value);
                }
            }
        }
        return pruned;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: `java TranslationPruner <source folder> <dest folder>`, where dest folder must contain the locale.constant-en_US.json for reference structure.");
            System.exit(1);
        }
        try {
            File sourceFolder = new File(args[0]);
            File destFolder = new File(args[1]);

            File referenceFile = new File(destFolder, "locale.constant-en_US.json");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode usRoot = mapper.readTree(referenceFile);
            Set<String> validKeys = new HashSet<>();
            collectKeys(usRoot, "", validKeys);
            for (File sourceFile : sourceFolder.listFiles()) {
                File destFile = new File(destFolder, sourceFile.getName());
                JsonNode sourceRoot = mapper.readTree(sourceFile);
                if (!sourceRoot.isObject()) {
                    throw new IllegalArgumentException("Source JSON must be an object at root");
                }
                ObjectNode pruned = pruneNode((ObjectNode) sourceRoot, validKeys, "", mapper);
                Separators seps = Separators.createDefaultInstance()
                        .withObjectFieldValueSpacing(Spacing.AFTER);
                mapper.writer(new DefaultPrettyPrinter().withSeparators(seps)).writeValue(destFile, pruned);
                System.out.println("Pruned translation written to " + destFile.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

}
