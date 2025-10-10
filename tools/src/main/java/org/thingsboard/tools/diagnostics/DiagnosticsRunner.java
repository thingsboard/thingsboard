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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diagnostic runner that:
 *  - consumes Maven error lines
 *  - finds affected files
 *  - appends diagnostic comments (non-destructive) to those files
 *  - produces a unified diff string
 *  - appends a README section describing the scan
 *
 * It does NOT fix code. Only annotates.
 */
public class DiagnosticsRunner {

    private final Path repoRoot;
    private final List<Path> annotatedFiles = new ArrayList<>();
    private final Map<Path, String> originalContents = new HashMap<>();
    private final Map<Path, String> updatedContents = new HashMap<>();

    public DiagnosticsRunner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    /**
     * Accepts raw error lines, attempts to resolve file paths and appends diagnostic comments.
     */
    public void annotateFailures(List<String> errorMessages) throws IOException {
        if (errorMessages == null || errorMessages.isEmpty()) return;
        for (String line : errorMessages) {
            Path file = extractFilePath(line);
            if (file == null) continue;
            String issueType = classifyIssue(line);
            int score = ComplexityScorer.scoreForIssueType(issueType);
            String reason = line;
            String fix = suggestFix(issueType);
            String annotation = DiagnosticCommentUtil.buildAnnotation(reason, score, fix);
            appendAnnotation(file, annotation);
        }
    }

    public List<Path> getAnnotatedFiles() {
        return Collections.unmodifiableList(annotatedFiles);
    }

    /**
     * Creates a very simple unified diff for the annotated files.
     */
    public String generateUnifiedDiff() {
        StringBuilder diff = new StringBuilder();
        for (Path file : annotatedFiles) {
            String a = originalContents.get(file);
            String b = updatedContents.get(file);
            if (a == null || b == null) continue;
            diff.append("diff --git a/").append(repoRoot.relativize(file)).append(" b/").append(repoRoot.relativize(file)).append("\n");
            diff.append("--- a/").append(repoRoot.relativize(file)).append("\n");
            diff.append("+++ b/").append(repoRoot.relativize(file)).append("\n");
            diff.append("@@\n");
            diff.append("-").append(a.replace("\n", "\n-")).append("\n");
            diff.append("+").append(b.replace("\n", "\n+")).append("\n");
        }
        return diff.toString();
    }

    /**
     * Appends a Build Failure Diagnostic Report section to README.md.
     */
    public void updateReadme(String reportMarkdown) throws IOException {
        Path readme = repoRoot.resolve("README.md");
        String existing = Files.exists(readme) ? Files.readString(readme) : "";
        StringBuilder sb = new StringBuilder(existing);
        if (!existing.endsWith("\n")) sb.append('\n');
        sb.append(reportMarkdown).append('\n');
        Files.writeString(readme, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void appendAnnotation(Path file, String annotation) throws IOException {
        Path abs = repoRoot.resolve(file).normalize();
        if (!Files.exists(abs)) return;
        String content = Files.readString(abs);
        originalContents.putIfAbsent(abs, content);

        StringBuilder sb = new StringBuilder(content);
        if (!content.endsWith("\n")) sb.append('\n');
        sb.append(annotation).append('\n');

        String updated = sb.toString();
        updatedContents.put(abs, updated);
        Files.writeString(abs, updated, StandardCharsets.UTF_8);
        if (!annotatedFiles.contains(abs)) annotatedFiles.add(abs);
    }

    private static final Pattern JAVA_FILE = Pattern.compile("([\\w/\\.-]+\\.java)");
    private static final Pattern XML_FILE = Pattern.compile("([\\w/\\.-]+\\.xml)");

    private Path extractFilePath(String errorLine) {
        if (errorLine == null) return null;
        Matcher m = JAVA_FILE.matcher(errorLine);
        if (m.find()) {
            return Path.of(m.group(1));
        }
        m = XML_FILE.matcher(errorLine);
        if (m.find()) {
            return Path.of(m.group(1));
        }
        return null;
    }

    private String classifyIssue(String line) {
        String l = line.toLowerCase();
        if (l.contains("unused import")) return "unused-import";
        if (l.contains("cannot find symbol") || l.contains("class not found") || l.contains("package does not exist")) return "missing-dependency";
        if (l.contains("circular") && l.contains("dependency")) return "circular-dependency";
        if (l.contains("annotation") && l.contains("failed")) return "faulty-annotation";
        if (l.contains("module") && l.contains("dependency")) return "cross-service-overhaul";
        return "multi-component-local";
    }

    private String suggestFix(String issueType) {
        switch (issueType) {
            case "unused-import":
                return "Remove unused imports or minor syntax adjustments.";
            case "missing-dependency":
                return "Add the appropriate dependency to pom.xml and ensure version compatibility.";
            case "circular-dependency":
                return "Break the dependency loop by extracting shared logic to an independent module.";
            case "faulty-annotation":
                return "Adjust annotations or processor configuration to satisfy compile-time requirements.";
            case "cross-service-overhaul":
                return "Refactor module/service dependencies; consider separating cross-cutting concerns.";
            default:
                return "Localize changes to affected components within the service.";
        }
    }
}


