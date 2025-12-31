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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test for a diagnostics runner that can annotate files and produce a diff-ready output
 * and update README. The tests check behavior and contracts, not implementation details.
 *
 * Expected API (suggested, but not implementation-constraining):
 *  - class org.thingsboard.tools.diagnostics.DiagnosticsRunner
 *  - public DiagnosticsRunner(Path repoRoot)
 *  - public void annotateFailures(List<String> errorMessages)
 *  - public List<Path> getAnnotatedFiles()
 *  - public String generateUnifiedDiff()
 *  - public void appendReadmeReport(String summary, List<DiagnosticsEntry>) OR public void updateReadme(String report)
 */
public class DiagnosticsRunnerContractTest {

    @Test
    void runnerProducesDiffAndReadmeSection() throws Exception {
        Class<?> runnerClass;
        try {
            runnerClass = Class.forName("org.thingsboard.tools.diagnostics.DiagnosticsRunner");
        } catch (ClassNotFoundException e) {
            fail("Missing class: org.thingsboard.tools.diagnostics.DiagnosticsRunner with a constructor taking Path.");
            return;
        }

        Constructor<?> ctor = null;
        for (Constructor<?> c : runnerClass.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && Path.class.isAssignableFrom(p[0])) {
                ctor = c;
                break;
            }
        }
        if (ctor == null) {
            fail("DiagnosticsRunner must have a public constructor that accepts java.nio.file.Path");
            return;
        }

        Path tempRepo = Files.createTempDirectory("tb-diag-contract");
        try {
            // Seed a sample file to annotate
            Path sample = tempRepo.resolve("sample/Module.java");
            Files.createDirectories(sample.getParent());
            Files.writeString(sample, "public class Module {\n    // TODO: failing build example\n}\n");

            Object runner = ctor.newInstance(tempRepo);

            // Find methods we need but don't force exact names for README update, allow either updateReadme(String) or appendReadmeReport(...)
            Method annotateFailures = findMethod(runnerClass, "annotateFailures", List.class);
            assertNotNull(annotateFailures, "DiagnosticsRunner must expose annotateFailures(List<String>)");

            Method getAnnotatedFiles = findMethod(runnerClass, "getAnnotatedFiles");
            assertNotNull(getAnnotatedFiles, "DiagnosticsRunner must expose getAnnotatedFiles() -> List<Path>");

            Method generateUnifiedDiff = findMethod(runnerClass, "generateUnifiedDiff");
            assertNotNull(generateUnifiedDiff, "DiagnosticsRunner must expose generateUnifiedDiff() -> String");

            // README updater method: prefer updateReadme(String)
            Method updateReadme = findMethod(runnerClass, "updateReadme", String.class);
            Method appendReadmeReport = updateReadme == null ? findAnyByName(runnerClass, "appendReadmeReport") : null;
            assertTrue(updateReadme != null || appendReadmeReport != null, "DiagnosticsRunner must provide a method to update README with a diagnostic report");

            // Simulate errors
            @SuppressWarnings("unchecked")
            List<String> errors = List.of(
                    "[ERROR] Missing dependency: org.example:foo-bar:1.0",
                    "[ERROR] Annotation processing failed in com.example.Demo: Unresolved symbol",
                    "[ERROR] Module circular dependency detected"
            );

            annotateFailures.invoke(runner, errors);

            @SuppressWarnings("unchecked")
            List<Path> files = (List<Path>) getAnnotatedFiles.invoke(runner);
            assertNotNull(files);
            assertFalse(files.isEmpty(), "At least one file should be marked as affected/annotated");

            String diff = (String) generateUnifiedDiff.invoke(runner);
            assertNotNull(diff);
            assertTrue(diff.startsWith("diff") || diff.contains("@@"), "Unified diff output should look like a git-compatible diff");

            Path readme = tempRepo.resolve("README.md");
            String report = "## \uD83D\uDEE0 Build Failure Diagnostic Report\n\n- Summary here";
            if (updateReadme != null) {
                updateReadme.invoke(runner, report);
            } else {
                // Fallback to any append method with one String parameter
                Method appendWithString = findMethodByParamTypes(appendReadmeReport, String.class);
                if (appendWithString != null) {
                    appendWithString.invoke(runner, report);
                } else {
                    fail("appendReadmeReport should accept a String report payload");
                }
            }

            assertTrue(Files.exists(readme), "README.md must be created or updated by the diagnostics runner");
            String readmeContent = Files.readString(readme);
            assertTrue(readmeContent.contains("Build Failure Diagnostic Report"), "README must include a Build Failure Diagnostic Report section");
            assertTrue(readmeContent.toLowerCase().contains("scoring") || readmeContent.toLowerCase().contains("score"), "README section should explain scoring criteria");
        } finally {
            // Best effort cleanup
            deleteRecursively(tempRepo.toFile());
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findAnyByName(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private static Method findMethodByParamTypes(Method candidate, Class<?>... params) {
        if (candidate == null) return null;
        Class<?>[] p = candidate.getParameterTypes();
        if (p.length != params.length) return null;
        for (int i = 0; i < p.length; i++) {
            if (!p[i].isAssignableFrom(params[i])) return null;
        }
        return candidate;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}


