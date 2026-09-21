// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

public class EdgeLatestVersionTest {

    @Test
    public void edgeLatestVersionIsSynchronizedTest() {
        EdgeVersion currentHighestEdgeVersion = EdgeVersionComparator.getNewestEdgeVersion();

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
