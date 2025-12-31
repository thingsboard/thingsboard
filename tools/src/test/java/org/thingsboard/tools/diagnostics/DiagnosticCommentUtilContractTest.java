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
 * Contract test specifying the required annotation comment format for build-failure diagnostics.
 * This test uses reflection so it compiles before the implementation exists.
 * Expected class: org.thingsboard.tools.diagnostics.DiagnosticCommentUtil
 * Expected API: public static String buildAnnotation(String reason, int score, String recommendedFix)
 */
public class DiagnosticCommentUtilContractTest {

    @Test
    void annotationIncludesStarsLabelsAndScore() throws Exception {
        Class<?> utilClass;
        try {
            utilClass = Class.forName("org.thingsboard.tools.diagnostics.DiagnosticCommentUtil");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing class: org.thingsboard.tools.diagnostics.DiagnosticCommentUtil. Provide a public class with a static buildAnnotation(reason, score, recommendedFix) method.");
        }

        Method method = null;
        for (Method m : utilClass.getDeclaredMethods()) {
            if (m.getName().equals("buildAnnotation") && m.getParameterCount() == 3) {
                Class<?>[] p = m.getParameterTypes();
                if (p[0] == String.class && p[1] == int.class && p[2] == String.class) {
                    method = m;
                    break;
                }
            }
        }
        if (method == null) {
            throw new AssertionError("Missing method: static String buildAnnotation(String reason, int score, String recommendedFix) in DiagnosticCommentUtil");
        }

        String reason = "Missing dependency in pom.xml";
        int score = 2;
        String fix = "Add appropriate dependency block and ensure version compatibility.";
        String annotation = (String) method.invoke(null, reason, score, fix);

        if (annotation == null) throw new AssertionError("Annotation text must not be null");
        if (!annotation.contains("*****")) throw new AssertionError("Annotation must be enclosed using \"*****\"");
        if (!annotation.toLowerCase().contains("build-failure-annotation")) throw new AssertionError("Annotation must include label [BUILD-FAILURE-ANNOTATION]");
        if (!annotation.contains("Score: " + score)) throw new AssertionError("Annotation must include the score number");
        if (!annotation.contains(reason)) throw new AssertionError("Annotation must include the reason text");
        if (!annotation.toLowerCase().contains("recommended fix")) throw new AssertionError("Annotation must include a Recommended Fix line");

        // Should be safe to embed as an HTML or block comment in source files
        boolean looksLikeComment = annotation.trim().startsWith("<!--") || annotation.trim().startsWith("/*") || annotation.contains("// Reason") || annotation.contains("Reason:");
        if (!looksLikeComment) throw new AssertionError("Annotation should be safely embeddable as a comment (HTML or language comment)");
    }
}


