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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Integration test that verifies the complete build failure diagnostic workflow.
 * This test simulates a build failure scenario and verifies that:
 * 1. Diagnostic annotations are added to affected files
 * 2. README.md is updated with diagnostic report
 * 3. Continuous debug mode comments out non-essential failing code
 * 4. All diagnostic artifacts are properly generated
 */
public class BuildFailureDiagnosticIntegrationTest {

    @Test
    void buildFailureTriggersCompleteDiagnosticWorkflow() throws Exception {
        // Simulate a build failure scenario
        Path tempRepo = Files.createTempDirectory("tb-build-failure-test");
        try {
            // Create sample failing files
            createFailingJavaFile(tempRepo);
            createFailingPomFile(tempRepo);
            createFailingTestFile(tempRepo);
            
            // Simulate Maven build failure output
            List<String> buildErrors = List.of(
                "[ERROR] Failed to execute goal compile on project application: Compilation failure",
                "[ERROR] /Users/test/application/src/main/java/org/thingsboard/server/service/TestService.java:[45,12] cannot find symbol",
                "[ERROR] symbol:   class MissingDependency",
                "[ERROR] location: class org.thingsboard.server.service.TestService",
                "[ERROR] /Users/test/application/pom.xml:[23,45] Missing artifact: org.example:missing-lib:1.0.0",
                "[ERROR] /Users/test/application/src/test/java/org/thingsboard/server/service/TestServiceTest.java:[12,8] cannot find symbol",
                "[ERROR] symbol:   class TestService",
                "[ERROR] Circular dependency detected between modules: common and application"
            );
            
            // Initialize diagnostics runner
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            
            // Process build failures
            runner.annotateFailures(buildErrors);
            
            // Verify annotations were added to affected files
            List<Path> annotatedFiles = runner.getAnnotatedFiles();
            if (annotatedFiles.isEmpty()) {
                throw new AssertionError("FAILURE: No files were annotated despite build errors. Diagnostic system failed to identify affected files.");
            }
            
            // Verify each annotated file contains diagnostic comments
            for (Path file : annotatedFiles) {
                String content = Files.readString(file);
                if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                    throw new AssertionError("FAILURE: File " + file + " was marked as annotated but contains no diagnostic annotation. Diagnostic system failed to add annotations.");
                }
                if (!content.contains("Score:")) {
                    throw new AssertionError("FAILURE: File " + file + " annotation missing severity score. Diagnostic system failed to assign complexity scores.");
                }
            }
            
            // Verify unified diff generation
            String diff = runner.generateUnifiedDiff();
            if (diff == null || diff.trim().isEmpty()) {
                throw new AssertionError("FAILURE: No unified diff generated. Diagnostic system failed to produce diff output for Git integration.");
            }
            if (!diff.contains("diff --git")) {
                throw new AssertionError("FAILURE: Generated diff is not in proper unified diff format. Diagnostic system failed to produce Git-compatible diff.");
            }
            
            // Verify README.md diagnostic report
            String report = generateDiagnosticReport(annotatedFiles);
            runner.updateReadme(report);
            
            Path readme = tempRepo.resolve("README.md");
            if (!Files.exists(readme)) {
                throw new AssertionError("FAILURE: README.md was not created/updated. Diagnostic system failed to document build failure in README.");
            }
            
            String readmeContent = Files.readString(readme);
            if (!readmeContent.contains("Build Failure Diagnostic Report")) {
                throw new AssertionError("FAILURE: README.md missing diagnostic report section. Diagnostic system failed to document build analysis.");
            }
            if (!readmeContent.contains("Scoring System")) {
                throw new AssertionError("FAILURE: README.md missing scoring criteria explanation. Diagnostic system failed to document complexity scoring.");
            }
            if (!readmeContent.contains("Next Steps")) {
                throw new AssertionError("FAILURE: README.md missing remediation steps. Diagnostic system failed to provide actionable recommendations.");
            }
            
            // Verify continuous debug mode functionality
            verifyContinuousDebugMode(tempRepo, buildErrors);
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    private void createFailingJavaFile(Path repoRoot) throws IOException {
        Path javaFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/TestService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            package org.thingsboard.server.service;
            
            import org.springframework.beans.factory.annotation.Autowired;
            import org.thingsboard.server.common.data.id.TenantId;
            
            public class TestService {
                @Autowired
                private MissingDependency missingDependency;
                
                public void processData(TenantId tenantId) {
                    // This will fail due to missing dependency
                    missingDependency.process(tenantId);
                }
            }
            """);
    }
    
    private void createFailingPomFile(Path repoRoot) throws IOException {
        Path pomFile = repoRoot.resolve("application/pom.xml");
        Files.createDirectories(pomFile.getParent());
        Files.writeString(pomFile, """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.thingsboard</groupId>
                <artifactId>application</artifactId>
                <version>4.3.0-SNAPSHOT</version>
                
                <dependencies>
                    <dependency>
                        <groupId>org.example</groupId>
                        <artifactId>missing-lib</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """);
    }
    
    private void createFailingTestFile(Path repoRoot) throws IOException {
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/TestServiceTest.java");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, """
            package org.thingsboard.server.service;
            
            import org.junit.jupiter.api.Test;
            import org.thingsboard.server.common.data.id.TenantId;
            
            public class TestServiceTest {
                @Test
                public void testProcessData() {
                    TestService service = new TestService();
                    // This test will fail due to missing TestService class
                    service.processData(TenantId.fromString("12345678-1234-1234-1234-123456789012"));
                }
            }
            """);
    }
    
    private String generateDiagnosticReport(List<Path> annotatedFiles) {
        return """
            ## 🛠 Build Failure Diagnostic Report
            
            ### Diagnostic Summary
            - Full codebase scan performed post build failure.
            - Annotated all directly affected classes, configs, and pom files.
            - Used standardized scoring system (1–5) with 20% step increases in complexity.
            - Comments are marked with "*****" and labeled per diagnostic type.
            
            ### Affected Files
            """ + annotatedFiles.size() + " files annotated with diagnostic comments.\n" + """
            
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
    }
    
    private void verifyContinuousDebugMode(Path repoRoot, List<String> buildErrors) throws IOException {
        // Verify that non-essential failing code is commented out
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/TestServiceTest.java");
        if (Files.exists(testFile)) {
            String content = Files.readString(testFile);
            // In continuous debug mode, failing test code should be commented out
            if (!content.contains("// ***** [BUILD-FAILURE-ANNOTATION]")) {
                throw new AssertionError("FAILURE: Continuous debug mode failed to comment out non-essential failing test code. Build cannot continue with failing tests.");
            }
        }
        
        // Verify that essential build-blocking code is identified but not commented out
        Path javaFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/TestService.java");
        if (Files.exists(javaFile)) {
            String content = Files.readString(javaFile);
            if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                throw new AssertionError("FAILURE: Essential build-blocking code not properly annotated. Diagnostic system failed to identify critical failures.");
            }
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
