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
package org.thingsboard.script.api.js;

import java.util.regex.Pattern;

public class JsValidator {

    static final Pattern ASYNC_PATTERN = Pattern.compile("\\basync\\b");
    static final Pattern AWAIT_PATTERN = Pattern.compile("\\bawait\\b");
    static final Pattern PROMISE_PATTERN = Pattern.compile("\\bPromise\\b");
    static final Pattern SET_TIMEOUT_PATTERN = Pattern.compile("\\bsetTimeout\\b");

    public static String validate(String scriptBody) {
        if (scriptBody == null || scriptBody.trim().isEmpty()) {
            return "Script body is empty";
        }

        //Quick check
        if (!ASYNC_PATTERN.matcher(scriptBody).find()
                && !AWAIT_PATTERN.matcher(scriptBody).find()
                && !PROMISE_PATTERN.matcher(scriptBody).find()
                && !SET_TIMEOUT_PATTERN.matcher(scriptBody).find()) {
            return null;
        }

        //Recheck if quick check failed. Ignoring comments and strings
        String[] lines = scriptBody.split("\\r?\\n");
        boolean insideMultilineComment = false;

        for (String line : lines) {
            String stripped = line;

            // Handle multiline comments
            if (insideMultilineComment) {
                if (line.contains("*/")) {
                    insideMultilineComment = false;
                    stripped = line.substring(line.indexOf("*/") + 2); // continue after comment
                } else {
                    continue; // skip line inside multiline comment
                }
            }

            // Check for start of multiline comment
            if (stripped.contains("/*")) {
                int start = stripped.indexOf("/*");
                int end = stripped.indexOf("*/", start + 2);

                if (end != -1) {
                    // Inline multiline comment
                    stripped = stripped.substring(0, start) + stripped.substring(end + 2);
                } else {
                    // Starts a block comment, continues on next lines
                    insideMultilineComment = true;
                    stripped = stripped.substring(0, start);
                }
            }

            stripped = stripInlineComment(stripped);
            stripped = stripStringLiterals(stripped);

            if (ASYNC_PATTERN.matcher(stripped).find()) {
                return "Script must not contain 'async' keyword.";
            }
            if (AWAIT_PATTERN.matcher(stripped).find()) {
                return "Script must not contain 'await' keyword.";
            }
            if (PROMISE_PATTERN.matcher(stripped).find()) {
                return "Script must not use 'Promise'.";
            }
            if (SET_TIMEOUT_PATTERN.matcher(stripped).find()) {
                return "Script must not use 'setTimeout' method.";
            }
        }
        return null;
    }

    private static String stripInlineComment(String line) {
        int index = line.indexOf("//");
        return index >= 0 ? line.substring(0, index) : line;
    }

    private static String stripStringLiterals(String line) {
        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            } else if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

}
