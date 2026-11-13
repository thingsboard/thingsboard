// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstallTestSupport {

    private static final Pattern TB_CLUSTER_DDL = Pattern.compile(
            "^\\s*(CREATE\\s+(?:TABLE|UNIQUE\\s+INDEX)[^;]*\\btb_cluster\\b[^;]*;)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private InstallTestSupport() {
    }

    // The working directory is either the repository root or the application module, depending on how tests are run.
    static Path dataDir() {
        String workDir = System.getProperty("user.dir");
        Path applicationDir = workDir.endsWith("application") ? Paths.get(workDir) : Paths.get(workDir, "application");
        return applicationDir.resolve(Paths.get("src", "main", "data"));
    }

    static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + path, e);
        }
    }

    static String readResource(String name) {
        try (InputStream in = InstallTestSupport.class.getClassLoader().getResourceAsStream(name)) {
            return new String(Objects.requireNonNull(in, name + " is missing from the test classpath").readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + name, e);
        }
    }

    static List<String> tbClusterStatements(String sql) {
        List<String> statements = new ArrayList<>();
        Matcher matcher = TB_CLUSTER_DDL.matcher(sql);
        while (matcher.find()) {
            statements.add(matcher.group(1));
        }
        return statements;
    }

}
