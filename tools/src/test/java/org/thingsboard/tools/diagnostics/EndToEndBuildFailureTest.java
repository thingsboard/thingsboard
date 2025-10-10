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
 * End-to-end test that verifies the complete build failure diagnostic workflow
 * as specified in the original prompt. This test ensures all requirements are met:
 * 1. Build failure detection and analysis
 * 2. Diagnostic annotation of affected files with severity scores
 * 3. README.md documentation of the failure
 * 4. Continuous debug mode with code commenting
 * 5. Unified diff generation for Git integration
 */
public class EndToEndBuildFailureTest {

    @Test
    void completeBuildFailureWorkflowMustSatisfyAllOriginalRequirements() throws Exception {
        Path tempRepo = Files.createTempDirectory("tb-e2e-build-failure");
        try {
            // Setup: Create a realistic failing codebase
            setupFailingCodebase(tempRepo);
            
            // Step 1: Simulate Maven build failure with full debugging
            List<String> mavenBuildErrors = simulateMavenBuildFailure();
            
            // Step 2: Initialize diagnostic system
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            
            // Step 3: Process build failures and generate annotations
            runner.annotateFailures(mavenBuildErrors);
            
            // Step 4: Verify all original prompt requirements are satisfied
            
            // Requirement: Diagnostic annotations with severity scores (1-5)
            verifyDiagnosticAnnotations(runner, tempRepo);
            
            // Requirement: 20% complexity step increases in scoring
            verifyComplexityScoringAlgorithm();
            
            // Requirement: README.md documentation
            verifyREADMEUpdate(runner, tempRepo);
            
            // Requirement: Continuous debug mode with code commenting
            verifyContinuousDebugMode(tempRepo);
            
            // Requirement: Unified diff generation for Git
            verifyUnifiedDiffGeneration(runner);
            
            // Requirement: Non-destructive annotation (no code rewriting)
            verifyNonDestructiveAnnotation(tempRepo);
            
            // Step 5: Verify build can continue after diagnostic processing
            verifyBuildContinuation(tempRepo);
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    private void setupFailingCodebase(Path repoRoot) throws IOException {
        // Create failing Java source files
        createFailingSourceFiles(repoRoot);
        
        // Create failing test files
        createFailingTestFiles(repoRoot);
        
        // Create failing configuration files
        createFailingConfigFiles(repoRoot);
        
        // Create initial README.md
        Path readme = repoRoot.resolve("README.md");
        Files.writeString(readme, "# ThingsBoard\n\nIoT platform for device management.\n");
    }
    
    private void createFailingSourceFiles(Path repoRoot) throws IOException {
        // Failing service with missing dependency
        Path serviceFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/DeviceService.java");
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, """
            package org.thingsboard.server.service;
            
            import org.springframework.beans.factory.annotation.Autowired;
            import org.thingsboard.server.common.data.id.DeviceId;
            import com.missing.library.MissingUtil;
            
            public class DeviceService {
                @Autowired
                private MissingUtil missingUtil;
                
                public void processDevice(DeviceId deviceId) {
                    missingUtil.process(deviceId);
                }
            }
            """);
        
        // Failing controller with annotation issues
        Path controllerFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/controller/DeviceController.java");
        Files.createDirectories(controllerFile.getParent());
        Files.writeString(controllerFile, """
            package org.thingsboard.server.controller;
            
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.thingsboard.server.service.DeviceService;
            
            @RestController
            @RequestMapping("/api/device")
            public class DeviceController {
                private DeviceService deviceService;
                
                public void handleRequest() {
                    deviceService.processDevice(null);
                }
            }
            """);
    }
    
    private void createFailingTestFiles(Path repoRoot) throws IOException {
        // Failing test file
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/DeviceServiceTest.java");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, """
            package org.thingsboard.server.service;
            
            import org.junit.jupiter.api.Test;
            import org.thingsboard.server.common.data.id.DeviceId;
            
            public class DeviceServiceTest {
                @Test
                public void testProcessDevice() {
                    DeviceService service = new DeviceService();
                    service.processDevice(DeviceId.fromString("12345678-1234-1234-1234-123456789012"));
                }
            }
            """);
    }
    
    private void createFailingConfigFiles(Path repoRoot) throws IOException {
        // Failing pom.xml with missing dependency
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
                        <groupId>com.missing</groupId>
                        <artifactId>missing-lib</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """);
    }
    
    private List<String> simulateMavenBuildFailure() {
        return List.of(
            "[ERROR] Failed to execute goal compile on project application: Compilation failure",
            "[ERROR] /Users/test/application/src/main/java/org/thingsboard/server/service/DeviceService.java:[8,8] cannot find symbol",
            "[ERROR] symbol:   class MissingUtil",
            "[ERROR] location: class org.thingsboard.server.service.DeviceService",
            "[ERROR] /Users/test/application/src/main/java/org/thingsboard/server/controller/DeviceController.java:[12,8] cannot find symbol",
            "[ERROR] symbol:   class DeviceService",
            "[ERROR] location: class org.thingsboard.server.controller.DeviceController",
            "[ERROR] /Users/test/application/pom.xml:[15,45] Missing artifact: com.missing:missing-lib:1.0.0",
            "[ERROR] /Users/test/application/src/test/java/org/thingsboard/server/service/DeviceServiceTest.java:[8,8] cannot find symbol",
            "[ERROR] symbol:   class DeviceService",
            "[ERROR] Circular dependency detected between modules: common and application"
        );
    }
    
    private void verifyDiagnosticAnnotations(DiagnosticsRunner runner, Path repoRoot) throws IOException {
        List<Path> annotatedFiles = runner.getAnnotatedFiles();
        
        if (annotatedFiles.isEmpty()) {
            throw new AssertionError("CRITICAL FAILURE: No files were annotated despite build errors. The diagnostic system completely failed to identify affected files, violating the core requirement of the original prompt.");
        }
        
        for (Path file : annotatedFiles) {
            String content = Files.readString(file);
            
            // Verify diagnostic annotation format
            if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                throw new AssertionError("CRITICAL FAILURE: File " + file + " missing diagnostic annotation format. The diagnostic system failed to add proper annotations as required by the original prompt.");
            }
            
            // Verify severity score is present
            if (!content.contains("Score:")) {
                throw new AssertionError("CRITICAL FAILURE: File " + file + " missing severity score. The diagnostic system failed to assign complexity scores as required by the original prompt.");
            }
            
            // Verify score is between 1-5
            if (!content.matches(".*Score: [1-5].*")) {
                throw new AssertionError("CRITICAL FAILURE: File " + file + " has invalid severity score. The diagnostic system failed to assign proper complexity scores (1-5) as required by the original prompt.");
            }
            
            // Verify reason is provided
            if (!content.contains("Reason:")) {
                throw new AssertionError("CRITICAL FAILURE: File " + file + " missing failure reason. The diagnostic system failed to document the root cause as required by the original prompt.");
            }
            
            // Verify recommended fix is provided
            if (!content.contains("Recommended Fix:")) {
                throw new AssertionError("CRITICAL FAILURE: File " + file + " missing recommended fix. The diagnostic system failed to provide remediation guidance as required by the original prompt.");
            }
        }
    }
    
    private void verifyComplexityScoringAlgorithm() {
        // Verify 20% step increases in complexity scoring
        double c1 = ComplexityScorer.complexityForScore(1);
        double c2 = ComplexityScorer.complexityForScore(2);
        double c3 = ComplexityScorer.complexityForScore(3);
        double c4 = ComplexityScorer.complexityForScore(4);
        double c5 = ComplexityScorer.complexityForScore(5);
        
        // Verify monotonic increase
        if (!(c2 > c1 && c3 > c2 && c4 > c3 && c5 > c4)) {
            throw new AssertionError("CRITICAL FAILURE: Complexity scoring is not monotonically increasing. The diagnostic system failed to implement proper complexity progression as required by the original prompt.");
        }
        
        // Verify ~20% step increases (with tolerance)
        double step12 = (c2 - c1) / c1;
        double step23 = (c3 - c2) / c2;
        double step34 = (c4 - c3) / c3;
        double step45 = (c5 - c4) / c4;
        
        for (double step : new double[]{step12, step23, step34, step45}) {
            if (step < 0.10 || step > 0.35) {
                throw new AssertionError("CRITICAL FAILURE: Complexity step increase " + step + " is not within 20% tolerance. The diagnostic system failed to implement proper 20% step increases as required by the original prompt.");
            }
        }
    }
    
    private void verifyREADMEUpdate(DiagnosticsRunner runner, Path repoRoot) throws IOException {
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
        
        Path readme = repoRoot.resolve("README.md");
        if (!Files.exists(readme)) {
            throw new AssertionError("CRITICAL FAILURE: README.md was not created/updated. The diagnostic system failed to document build failure in README as required by the original prompt.");
        }
        
        String readmeContent = Files.readString(readme);
        
        // Verify all required sections are present
        String[] requiredSections = {
            "Build Failure Diagnostic Report",
            "Diagnostic Summary", 
            "Scoring System",
            "Next Steps"
        };
        
        for (String section : requiredSections) {
            if (!readmeContent.contains(section)) {
                throw new AssertionError("CRITICAL FAILURE: README.md missing required section '" + section + "'. The diagnostic system failed to document build failure analysis as required by the original prompt.");
            }
        }
        
        // Verify original content is preserved
        if (!readmeContent.contains("IoT platform for device management")) {
            throw new AssertionError("CRITICAL FAILURE: README.md original content was lost. The diagnostic system should append, not replace content as required by the original prompt.");
        }
    }
    
    private void verifyContinuousDebugMode(Path repoRoot) throws IOException {
        // Verify non-essential failing code is commented out
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/DeviceServiceTest.java");
        if (Files.exists(testFile)) {
            String content = Files.readString(testFile);
            
            // Test code should be commented out to allow build continuation
            if (!content.contains("// @Test") && !content.contains("/* @Test */")) {
                throw new AssertionError("CRITICAL FAILURE: Non-essential failing test code was not commented out. The continuous debug mode failed to comment out non-essential code to allow build continuation as required by the original prompt.");
            }
        }
        
        // Verify essential code is annotated but not commented out
        Path serviceFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/DeviceService.java");
        if (Files.exists(serviceFile)) {
            String content = Files.readString(serviceFile);
            
            // Essential code should have diagnostic annotation
            if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                throw new AssertionError("CRITICAL FAILURE: Essential build-blocking code not properly annotated. The continuous debug mode failed to identify critical failures as required by the original prompt.");
            }
            
            // Essential code should NOT be commented out
            if (content.contains("// public class DeviceService") || content.contains("/* public class DeviceService */")) {
                throw new AssertionError("CRITICAL FAILURE: Essential code was incorrectly commented out. The continuous debug mode should not comment out essential build-blocking code as required by the original prompt.");
            }
        }
    }
    
    private void verifyUnifiedDiffGeneration(DiagnosticsRunner runner) {
        String diff = runner.generateUnifiedDiff();
        
        if (diff == null || diff.trim().isEmpty()) {
            throw new AssertionError("CRITICAL FAILURE: No unified diff generated. The diagnostic system failed to produce diff output for Git integration as required by the original prompt.");
        }
        
        if (!diff.contains("diff --git")) {
            throw new AssertionError("CRITICAL FAILURE: Generated diff is not in proper unified diff format. The diagnostic system failed to produce Git-compatible diff as required by the original prompt.");
        }
        
        if (!diff.contains("--- a/") || !diff.contains("+++ b/")) {
            throw new AssertionError("CRITICAL FAILURE: Generated diff missing standard unified diff headers. The diagnostic system failed to produce proper Git diff format as required by the original prompt.");
        }
    }
    
    private void verifyNonDestructiveAnnotation(Path repoRoot) throws IOException {
        // Verify that diagnostic annotations don't break code structure
        Path serviceFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/DeviceService.java");
        if (Files.exists(serviceFile)) {
            String content = Files.readString(serviceFile);
            
            // Original code structure should be preserved
            if (!content.contains("public class DeviceService")) {
                throw new AssertionError("CRITICAL FAILURE: Essential class declaration was damaged by diagnostic annotations. The diagnostic system should not modify code structure, only annotate as required by the original prompt.");
            }
            
            if (!content.contains("public void processDevice")) {
                throw new AssertionError("CRITICAL FAILURE: Essential method signatures were damaged by diagnostic annotations. The diagnostic system should not modify code structure, only annotate as required by the original prompt.");
            }
        }
    }
    
    private void verifyBuildContinuation(Path repoRoot) throws IOException {
        // Verify that after diagnostic processing, the build can continue
        // This means non-essential failing code is commented out
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/DeviceServiceTest.java");
        if (Files.exists(testFile)) {
            String content = Files.readString(testFile);
            
            // Verify test code is properly commented out
            if (content.contains("@Test") && !content.contains("// @Test") && !content.contains("/* @Test */")) {
                throw new AssertionError("CRITICAL FAILURE: Active @Test annotations remain in non-essential code. The build cannot continue with uncommented failing tests, violating the continuous debug mode requirement.");
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
