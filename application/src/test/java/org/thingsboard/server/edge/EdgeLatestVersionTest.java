/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

import java.util.Arrays;
import java.util.Comparator;

public class EdgeLatestVersionTest {

    @Test
    public void edgeLatestVersionIsSynchronizedTest() {
        EdgeVersion currentHighestEdgeVersion = Arrays.stream(EdgeVersion.values())
                .filter(v -> v != EdgeVersion.UNRECOGNIZED)
                .filter(v -> v != EdgeVersion.V_LATEST)
                .max(Comparator.comparingInt(EdgeVersion::getNumber))
                .orElseThrow();

        String projectVersion = EdgeLatestVersionTest.class.getPackage().getImplementationVersion();
        if (projectVersion == null || projectVersion.isBlank()) {
            projectVersion = System.getProperty("project.version", "UNKNOWN");
        }

        String projectVersionDigits = projectVersion.replaceAll("\\D", "");
        String currentHighestEdgeVersionDigits = currentHighestEdgeVersion.name().replaceAll("\\D", "");

        String msg = "EdgeVersion enum in edge.proto is out of sync. Please add respective " + projectVersionDigits + " to EdgeVersion";
        Assert.assertEquals(msg, projectVersionDigits, currentHighestEdgeVersionDigits);
    }

}
