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
package org.thingsboard.tools.diagnostics;

import java.time.Instant;

/**
 * Utility to build standardized diagnostic annotations for build failures.
 * An annotation is a comment-only block that may be embedded into source/config files.
 */
public final class DiagnosticCommentUtil {

    private DiagnosticCommentUtil() {}

    /**
     * Builds a portable diagnostic annotation comment block enclosed with ***** and labeled.
     * The output is safe to embed inside XML/HTML as an HTML comment, or inside code as a block/line comment
     * because the content does not include unclosed comment tokens.
     */
    public static String buildAnnotation(String reason, int score, String recommendedFix) {
        String normalizedReason = reason == null ? "" : reason.trim();
        String normalizedFix = recommendedFix == null ? "" : recommendedFix.trim();
        String timestamp = Instant.now().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("<!-- ***** [BUILD-FAILURE-ANNOTATION] Score: ").append(score).append("\n");
        if (!normalizedReason.isEmpty()) {
            sb.append("Reason: ").append(normalizedReason).append("\n");
        }
        if (!normalizedFix.isEmpty()) {
            sb.append("Recommended Fix: ").append(normalizedFix).append("\n");
        }
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("***** -->");
        return sb.toString();
    }
}


