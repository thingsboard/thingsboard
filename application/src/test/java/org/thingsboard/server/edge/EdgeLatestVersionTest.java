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
