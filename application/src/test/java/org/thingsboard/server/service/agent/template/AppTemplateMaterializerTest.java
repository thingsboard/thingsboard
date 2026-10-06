// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppTemplateMaterializerTest {

    private static final String PATH = "compose/edge/4.4/kafka.yml";

    @Test
    void patchVersionFallsBackThroughLowerPatchFoldersToTheLine() {
        assertThat(AppTemplateMaterializer.composePathCandidates(PATH, vars("4.4", "4.4.3EDGE"))).containsExactly(
                "compose/edge/4.4/4.4.3/kafka.yml",
                "compose/edge/4.4/4.4.2/kafka.yml",
                "compose/edge/4.4/4.4.1/kafka.yml",
                "compose/edge/4.4/4.4.0/kafka.yml",
                "compose/edge/4.4/kafka.yml");
    }

    @Test
    void hotfixVersionFallsBackThroughLowerHotfixesThenLowerPatches() {
        assertThat(AppTemplateMaterializer.composePathCandidates(PATH, vars("4.4", "4.4.1.2EDGE"))).containsExactly(
                "compose/edge/4.4/4.4.1/4.4.1.2/kafka.yml",
                "compose/edge/4.4/4.4.1/4.4.1.1/kafka.yml",
                "compose/edge/4.4/4.4.1/4.4.1.0/kafka.yml",
                "compose/edge/4.4/4.4.1/kafka.yml",
                "compose/edge/4.4/4.4.0/kafka.yml",
                "compose/edge/4.4/kafka.yml");
    }

    @Test
    void lineVersionUsesTheLineFolderOnly() {
        assertThat(AppTemplateMaterializer.composePathCandidates("compose/gateway/3.8/compose.yml", vars("3.8", "3.8-stable")))
                .containsExactly("compose/gateway/3.8/compose.yml");
    }

    @Test
    void versionOutsideTheLineUsesThePathAsIs() {
        assertThat(AppTemplateMaterializer.composePathCandidates(PATH, vars("4.4", "4.5.0EDGE")))
                .containsExactly(PATH);
    }

    private static Map<String, Object> vars(String composeLine, String version) {
        return Map.of("composeLine", composeLine, "version", version);
    }
}
