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
 * Test that verifies continuous debug mode functionality.
 * This test ensures that when build failures occur, non-essential failing code
 * (like test files) is commented out to allow build continuation, while
 * essential code is annotated but not commented out.
 */
public class ContinuousDebugModeTest {

    @Test
    void continuousDebugModeCommentsOutNonEssentialFailingCode() throws Exception {
        Path tempRepo = Files.createTempDirectory("tb-continuous-debug-test");
        try {
            // Create essential failing code (should be annotated but not commented)
            createEssentialFailingCode(tempRepo);
            
            // Create non-essential failing code (should be commented out)
            createNonEssentialFailingCode(tempRepo);
            
            // Simulate build failure
            List<String> buildErrors = List.of(
                "[ERROR] Essential service compilation failed",
                "[ERROR] Test compilation failed - non-essential",
                "[ERROR] Missing dependency in test code"
            );
            
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            runner.annotateFailures(buildErrors);
            
            // Verify essential code is annotated but not commented out
            Path essentialFile = tempRepo.resolve("application/src/main/java/org/thingsboard/server/service/EssentialService.java");
            if (Files.exists(essentialFile)) {
                String content = Files.readString(essentialFile);
                
                // Essential code should have diagnostic annotation
                if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                    throw new AssertionError("FAILURE: Essential build-blocking code not properly annotated. Continuous debug mode failed to identify critical failures that must be addressed.");
                }
                
                // Essential code should NOT be commented out (build cannot continue without it)
                if (content.contains("// public class EssentialService") || content.contains("/* public class EssentialService */")) {
                    throw new AssertionError("FAILURE: Essential code was incorrectly commented out. Continuous debug mode should not comment out essential build-blocking code.");
                }
                
                // Verify the code is still functional (not commented)
                if (!content.contains("public class EssentialService")) {
                    throw new AssertionError("FAILURE: Essential code structure was damaged. Continuous debug mode should preserve essential code functionality.");
                }
            }
            
            // Verify non-essential code is commented out
            Path testFile = tempRepo.resolve("application/src/test/java/org/thingsboard/server/service/NonEssentialTest.java");
            if (Files.exists(testFile)) {
                String content = Files.readString(testFile);
                
                // Non-essential code should have diagnostic annotation
                if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                    throw new AssertionError("FAILURE: Non-essential failing code not properly annotated. Continuous debug mode failed to identify non-critical failures.");
                }
                
                // Non-essential code should be commented out to allow build continuation
                if (!content.contains("// @Test") && !content.contains("/* @Test */")) {
                    throw new AssertionError("FAILURE: Non-essential failing test code was not commented out. Continuous debug mode failed to comment out non-essential code to allow build continuation.");
                }
                
                // Verify the test method is commented out
                if (!content.contains("// public void testSomething()") && !content.contains("/* public void testSomething() */")) {
                    throw new AssertionError("FAILURE: Non-essential test methods were not commented out. Continuous debug mode failed to disable failing tests.");
                }
            }
            
            // Verify build can continue after commenting out non-essential code
            verifyBuildContinuation(tempRepo);
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    @Test
    void continuousDebugModePreservesEssentialCodeStructure() throws Exception {
        Path tempRepo = Files.createTempDirectory("tb-essential-code-test");
        try {
            // Create complex essential code with multiple failure points
            createComplexEssentialCode(tempRepo);
            
            List<String> buildErrors = List.of(
                "[ERROR] Multiple compilation failures in essential service",
                "[ERROR] Missing dependencies in core functionality"
            );
            
            DiagnosticsRunner runner = new DiagnosticsRunner(tempRepo);
            runner.annotateFailures(buildErrors);
            
            Path essentialFile = tempRepo.resolve("application/src/main/java/org/thingsboard/server/service/ComplexService.java");
            if (Files.exists(essentialFile)) {
                String content = Files.readString(essentialFile);
                
                // Verify essential code structure is preserved
                if (!content.contains("public class ComplexService")) {
                    throw new AssertionError("FAILURE: Essential class declaration was damaged. Continuous debug mode must preserve essential code structure.");
                }
                
                if (!content.contains("public void processData")) {
                    throw new AssertionError("FAILURE: Essential method signatures were damaged. Continuous debug mode must preserve essential method structure.");
                }
                
                if (!content.contains("@Autowired")) {
                    throw new AssertionError("FAILURE: Essential annotations were damaged. Continuous debug mode must preserve essential annotation structure.");
                }
                
                // Verify diagnostic annotations are present
                if (!content.contains("***** [BUILD-FAILURE-ANNOTATION]")) {
                    throw new AssertionError("FAILURE: Essential code missing diagnostic annotations. Continuous debug mode must annotate all failure points.");
                }
                
                // Verify code is not commented out
                if (content.contains("// public class ComplexService") || content.contains("/* public class ComplexService */")) {
                    throw new AssertionError("FAILURE: Essential code was incorrectly commented out. Continuous debug mode must not comment out essential build-blocking code.");
                }
            }
            
        } finally {
            deleteRecursively(tempRepo.toFile());
        }
    }
    
    private void createEssentialFailingCode(Path repoRoot) throws IOException {
        Path javaFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/EssentialService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            package org.thingsboard.server.service;
            
            import org.springframework.beans.factory.annotation.Autowired;
            import org.thingsboard.server.common.data.id.TenantId;
            
            public class EssentialService {
                @Autowired
                private MissingDependency missingDependency;
                
                public void processData(TenantId tenantId) {
                    // This is essential functionality that cannot be commented out
                    missingDependency.process(tenantId);
                }
                
                public void criticalOperation() {
                    // This method is required for the application to function
                    System.out.println("Critical operation");
                }
            }
            """);
    }
    
    private void createNonEssentialFailingCode(Path repoRoot) throws IOException {
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/NonEssentialTest.java");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, """
            package org.thingsboard.server.service;
            
            import org.junit.jupiter.api.Test;
            import org.thingsboard.server.common.data.id.TenantId;
            
            public class NonEssentialTest {
                @Test
                public void testSomething() {
                    // This test is non-essential and can be commented out
                    EssentialService service = new EssentialService();
                    service.processData(TenantId.fromString("12345678-1234-1234-1234-123456789012"));
                }
                
                @Test
                public void testAnotherThing() {
                    // Another non-essential test
                    System.out.println("Test output");
                }
            }
            """);
    }
    
    private void createComplexEssentialCode(Path repoRoot) throws IOException {
        Path javaFile = repoRoot.resolve("application/src/main/java/org/thingsboard/server/service/ComplexService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            package org.thingsboard.server.service;
            
            import org.springframework.beans.factory.annotation.Autowired;
            import org.thingsboard.server.common.data.id.TenantId;
            import org.thingsboard.server.common.data.id.UserId;
            
            public class ComplexService {
                @Autowired
                private MissingDependency missingDependency;
                
                @Autowired
                private AnotherMissingService anotherService;
                
                public void processData(TenantId tenantId) {
                    // Complex essential logic that must be preserved
                    missingDependency.process(tenantId);
                    anotherService.validate(tenantId);
                }
                
                public void processUser(UserId userId) {
                    // Another essential method
                    System.out.println("Processing user: " + userId);
                }
                
                private void internalMethod() {
                    // Private method that's part of essential functionality
                    System.out.println("Internal processing");
                }
            }
            """);
    }
    
    private void verifyBuildContinuation(Path repoRoot) throws IOException {
        // This method would simulate attempting to continue the build
        // after non-essential code has been commented out
        Path testFile = repoRoot.resolve("application/src/test/java/org/thingsboard/server/service/NonEssentialTest.java");
        if (Files.exists(testFile)) {
            String content = Files.readString(testFile);
            
            // Verify that commented out test code won't cause build failures
            if (content.contains("@Test") && !content.contains("// @Test") && !content.contains("/* @Test */")) {
                throw new AssertionError("FAILURE: Active @Test annotations remain in non-essential code. Build cannot continue with uncommented failing tests.");
            }
            
            if (content.contains("public void test") && !content.contains("// public void test") && !content.contains("/* public void test")) {
                throw new AssertionError("FAILURE: Active test methods remain in non-essential code. Build cannot continue with uncommented failing test methods.");
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
