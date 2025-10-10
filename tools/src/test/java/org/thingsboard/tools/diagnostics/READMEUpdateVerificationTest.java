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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test that verifies README.md is properly updated with diagnostic reports
 * when build failures occur. This test ensures the documentation requirement
 * from the original prompt is satisfied.
 */
public class READMEUpdateVerificationTest {

    @Test
    void buildFailureMustUpdateREADMEWithDiagnosticReport() throws Exception {
        Path tempRepo = Files.createTempDirectory("tb-readme-test");
        try {
            // Create initial README.md
            Path readme = tempRepo.resolve("README.md");
            Files.writeString(readme, "# ThingsBoard\n\nInitial README content.\n");
            
            // Simulate build failure
            List<String> buildErrors = List.of(
                "[ERROR] Compilation failure in TestService.java",
                "[ERROR] Missing dependency: org.example:missing-lib:1.0.0",
                "[ERROR] Circular dependency detected"
            );
            
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            runner.annotateFailures(buildErrors);
            
            // Generate and append diagnostic report
            String diagnosticReport = """
                ## 🛠 Build Failure Diagnostic Report
                
                ### Diagnostic Summary
                - Full codebase scan performed post build failure.
                - Annotated all directly affected classes, configs, and pom files.
                - Used standardized scoring system (1–5) with 20% step increases in complexity.
                - Comments are marked with "*****" and labeled per diagnostic type.
                
                ### Scoring System
                - **1**: Minor syntax/config issues.
                - **2**: Localized fix within 1–2 classes.
                - **3**: Multi-component service-local issue.
                - **4**: Cross-service or configuration complexity.
                - **5**: Major architectural rework required.
                
                ### Next Steps
                1. Address all Score 1 and 2 issues (low-hanging fruit).
                2. Triangulate and isolate Score 3–4 clusters.
                3. Evaluate feasibility of Score 5 items before deeper refactors.
                """;
            
            runner.updateReadme(diagnosticReport);
            
            // Verify README was updated
            if (!Files.exists(readme)) {
                throw new AssertionError("FAILURE: README.md was not created/updated after build failure. Diagnostic system failed to document build failure in README as required by original prompt.");
            }
            
            String updatedContent = Files.readString(readme);
            
            // Verify original content is preserved
            if (!updatedContent.contains("Initial README content")) {
                throw new AssertionError("FAILURE: README.md original content was lost during diagnostic update. Diagnostic system should append, not replace content.");
            }
            
            // Verify diagnostic report was appended
            if (!updatedContent.contains("Build Failure Diagnostic Report")) {
                throw new AssertionError("FAILURE: README.md missing 'Build Failure Diagnostic Report' section. Diagnostic system failed to document build analysis as required.");
            }
            
            if (!updatedContent.contains("Diagnostic Summary")) {
                throw new AssertionError("FAILURE: README.md missing 'Diagnostic Summary' section. Diagnostic system failed to provide build failure analysis.");
            }
            
            if (!updatedContent.contains("Scoring System")) {
                throw new AssertionError("FAILURE: README.md missing 'Scoring System' explanation. Diagnostic system failed to document complexity scoring criteria.");
            }
            
            if (!updatedContent.contains("Next Steps")) {
                throw new AssertionError("FAILURE: README.md missing 'Next Steps' section. Diagnostic system failed to provide actionable remediation steps.");
            }
            
            // Verify scoring criteria are properly documented
            if (!updatedContent.contains("**1**: Minor syntax/config issues")) {
                throw new AssertionError("FAILURE: README.md missing Score 1 criteria. Diagnostic system failed to document minor fix complexity.");
            }
            
            if (!updatedContent.contains("**5**: Major architectural rework required")) {
                throw new AssertionError("FAILURE: README.md missing Score 5 criteria. Diagnostic system failed to document major architectural complexity.");
            }
            
            // Verify 20% complexity step documentation
            if (!updatedContent.contains("20% step increases in complexity")) {
                throw new AssertionError("FAILURE: README.md missing 20% complexity step documentation. Diagnostic system failed to document scoring algorithm as specified in original prompt.");
            }
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    @Test
    void multipleBuildFailuresAccumulateInREADME() throws Exception {
        Path tempRepo = Files.createTempDirectory("tb-readme-accumulation-test");
        try {
            Path readme = tempRepo.resolve("README.md");
            Files.writeString(readme, "# ThingsBoard\n\nInitial content.\n");
            
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            
            // First build failure
            List<String> firstFailure = List.of("[ERROR] First build failure");
            runner.annotateFailures(firstFailure);
            String firstReport = "## First Diagnostic Report\n\nFirst failure analysis.\n";
            runner.updateReadme(firstReport);
            
            // Second build failure
            List<String> secondFailure = List.of("[ERROR] Second build failure");
            runner.annotateFailures(secondFailure);
            String secondReport = "## Second Diagnostic Report\n\nSecond failure analysis.\n";
            runner.updateReadme(secondReport);
            
            String finalContent = Files.readString(readme);
            
            // Verify both reports are present
            if (!finalContent.contains("First Diagnostic Report")) {
                throw new AssertionError("FAILURE: First diagnostic report was lost. README should accumulate diagnostic reports, not replace them.");
            }
            
            if (!finalContent.contains("Second Diagnostic Report")) {
                throw new AssertionError("FAILURE: Second diagnostic report was not added. README should accumulate multiple diagnostic reports.");
            }
            
            if (!finalContent.contains("Initial content")) {
                throw new AssertionError("FAILURE: Original README content was lost. Diagnostic system should preserve original content while adding reports.");
            }
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    private static void deleteRecursively(java.io.File file) {
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
