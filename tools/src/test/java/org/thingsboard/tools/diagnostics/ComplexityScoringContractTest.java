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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Contract test defining the scoring API and 20% incremental complexity expectation.
 * Expected API:
 *  - class org.thingsboard.tools.diagnostics.ComplexityScorer
 *  - static int scoreForIssueType(String issueType)
 *  - static double complexityForScore(int score)
 */
public class ComplexityScoringContractTest {

    @Test
    void scoringApiAndComplexityGaps() throws Exception {
        Class<?> scorerClass;
        try {
            scorerClass = Class.forName("org.thingsboard.tools.diagnostics.ComplexityScorer");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing class: org.thingsboard.tools.diagnostics.ComplexityScorer with required static methods.");
           
        }

        Method scoreForIssueType = null;
        Method complexityForScore = null;
        for (Method m : scorerClass.getDeclaredMethods()) {
            if (m.getName().equals("scoreForIssueType") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                scoreForIssueType = m;
            }
            if (m.getName().equals("complexityForScore") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == int.class) {
                complexityForScore = m;
            }
        }
        if (scoreForIssueType == null || complexityForScore == null) {
            throw new AssertionError("Missing required static methods scoreForIssueType(String) and complexityForScore(int)");
        }

        // Validate monotonic increasing complexity with approx 20% gaps
        double c1 = ((Number) complexityForScore.invoke(null, 1)).doubleValue();
        double c2 = ((Number) complexityForScore.invoke(null, 2)).doubleValue();
        double c3 = ((Number) complexityForScore.invoke(null, 3)).doubleValue();
        double c4 = ((Number) complexityForScore.invoke(null, 4)).doubleValue();
        double c5 = ((Number) complexityForScore.invoke(null, 5)).doubleValue();

        if (!(c1 > 0)) throw new AssertionError("Complexity baseline must be > 0");
        if (!(c2 > c1 && c3 > c2 && c4 > c3 && c5 > c4)) throw new AssertionError("Complexities must be strictly increasing");

        // Each step should be around 20% increase (allow tolerance: 10%-35%)
        assertBetween(percentIncrease(c1, c2), 0.10, 0.35);
        assertBetween(percentIncrease(c2, c3), 0.10, 0.35);
        assertBetween(percentIncrease(c3, c4), 0.10, 0.35);
        assertBetween(percentIncrease(c4, c5), 0.10, 0.35);

        // Spot-check scores for named issues (implementations may map differently but should be in 1..5)
        Object scoreMissingImport = scoreForIssueType.invoke(null, "unused-import");
        Object scoreMissingDependency = scoreForIssueType.invoke(null, "missing-dependency");
        Object scoreCircularDependency = scoreForIssueType.invoke(null, "circular-dependency");
        Object scoreCrossService = scoreForIssueType.invoke(null, "cross-service-overhaul");
        Object scoreArchitectural = scoreForIssueType.invoke(null, "architectural-revision");

        for (Object s : new Object[]{scoreMissingImport, scoreMissingDependency, scoreCircularDependency, scoreCrossService, scoreArchitectural}) {
            int v = ((Number) s).intValue();
            if (!(v >= 1 && v <= 5)) throw new AssertionError("Scores must be between 1 and 5 inclusive");
        }
    }

    private static double percentIncrease(double from, double to) {
        return (to - from) / from;
    }

    private static void assertBetween(double value, double lowInclusive, double highInclusive) {
        if (!(value >= lowInclusive && value <= highInclusive)) throw new AssertionError("Value " + value + " not in [" + lowInclusive + ", " + highInclusive + "]");
    }
}


