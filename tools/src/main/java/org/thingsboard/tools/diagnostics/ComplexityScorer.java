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

import java.util.Locale;

/**
 * Consistent scoring for diagnostics. Scores 1..5 with ~20% step complexity increases.
 */
public final class ComplexityScorer {

    private ComplexityScorer() {}

    /**
     * Returns a relative complexity metric for a given score, where each increment increases
     * effort/complexity by roughly 20% (within tolerance). The absolute scale is arbitrary,
     * only ratios matter across 1..5.
     */
    public static double complexityForScore(int score) {
        if (score < 1) score = 1;
        if (score > 5) score = 5;
        double base = 100.0; // arbitrary baseline unit
        double step = 1.2;   // ~20% increase per step
        return base * Math.pow(step, score - 1);
    }

    /**
     * Map issue type names to a score 1..5. Unknown types default to 3.
     */
    public static int scoreForIssueType(String issueType) {
        if (issueType == null) return 3;
        String key = issueType.toLowerCase(Locale.ROOT).trim();
        switch (key) {
            case "unused-import":
            case "minor-syntax":
                return 1;
            case "missing-dependency":
            case "faulty-annotation":
                return 2;
            case "circular-dependency":
            case "multi-component-local":
                return 3;
            case "cross-service-overhaul":
            case "dependency-graph-refactor":
                return 4;
            case "architectural-revision":
                return 5;
            default:
                return 3;
        }
    }
}


