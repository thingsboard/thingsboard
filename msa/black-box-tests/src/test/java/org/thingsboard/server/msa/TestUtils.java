// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

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
        // a file that already declares any version (e.g. version: '3') must not get a second key: newer docker compose rejects duplicate keys
        if (!data.contains(versionString) && !Pattern.compile("^version:", Pattern.MULTILINE).matcher(data).find()) {
            data += "\n" + versionString + "\n";
        }
        Files.writeString(composeFilePath, data);
    }

}
