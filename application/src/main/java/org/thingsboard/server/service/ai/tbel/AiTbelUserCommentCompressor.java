/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.ai.tbel;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiTbelUserCommentCompressor {

    private final Map<String, String> commentMap = new LinkedHashMap<>();
    private static final String regExpAiComment = "// AI";

    public String compressScript(String script) {
        StringBuilder result = new StringBuilder();
        String[] lines = script.split("\n", -1);
        boolean inMultiLine = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            // Remove AI service comments without storing
            if (line.contains(regExpAiComment)) {
                int aiIndex = line.indexOf(regExpAiComment);
                line = line.substring(0, aiIndex).stripTrailing();
            }

            if (inMultiLine) {
                String key = String.format("// UserMany_Line%03d;", lineNumber);
                commentMap.put(key, line);
                result.append(key);
                if (i != lines.length - 1) result.append("\n");
                if (line.contains("*/")) {
                    inMultiLine = false;
                }
                continue;
            }

            if (line.trim().startsWith("//")) {
                String key = String.format("// UserOneLine_%03d;", lineNumber);
                commentMap.put(key, line);
                result.append(key);
                if (i != lines.length - 1) result.append("\n");
            } else if (line.contains("/*")) {
                inMultiLine = true;
                String key = String.format("// UserMany_Line%03d;", lineNumber);
                commentMap.put(key, line);
                result.append(key);
                if (i != lines.length - 1) result.append("\n");
                if (line.contains("*/")) {
                    inMultiLine = false;
                }
            } else if (line.contains("//")) {
                int idx = line.indexOf("//");
                String code = line.substring(0, idx);
                String comment = line.substring(idx);
                String key = String.format("// UserOneLine_%03d;", lineNumber);
                commentMap.put(key, comment);
                result.append(code).append(key);
                if (i != lines.length - 1) result.append("\n");
            } else {
                result.append(line);
                if (i != lines.length - 1) result.append("\n");
            }
        }
        return result.toString();
    }

    public String decompressScript(String compressedScript) {
        StringBuilder result = new StringBuilder();
        String[] lines = compressedScript.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (commentMap.containsKey(line.trim())) {
                result.append(commentMap.get(line.trim()));
            } else {
                for (Map.Entry<String, String> entry : commentMap.entrySet()) {
                    if (line.contains(entry.getKey())) {
                        line = line.replace(entry.getKey(), entry.getValue());
                        break;
                    }
                }
                result.append(line);
            }
            if (i != lines.length - 1) result.append("\n");
        }
        return result.toString();
    }
}
