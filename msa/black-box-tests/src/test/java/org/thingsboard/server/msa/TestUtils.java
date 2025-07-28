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
