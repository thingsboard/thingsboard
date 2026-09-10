// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestUtils {

    public static void addComposeVersion(List<File> composeFiles, String version) throws IOException {
        for (File composeFile : composeFiles) {
            addComposeVersion(composeFile, version);
        }
    }

    public static void addComposeVersion(File composeFile, String version) throws IOException {
        Path composeFilePath = composeFile.toPath();
        String data = Files.readString(composeFilePath);
        String versionString = "version: '" + version + "'";
        if (!data.contains(versionString)) {
            data += "\n" + versionString + "\n";
        }
        Files.writeString(composeFilePath, data);
    }

}
