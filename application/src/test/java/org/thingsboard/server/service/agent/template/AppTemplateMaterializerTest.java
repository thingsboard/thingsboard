// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AppTemplateMaterializerTest {

    private static final Function<String, List<String>> EDGE_FOLDERS = parent -> parent.equals("compose/edge")
            ? List.of("3.9", "4.3", "4.4", "4.4.0.1", "4.10", "latest")
            : List.of();

    @Test
    void lineVersionUsesItsLineFolderThenOlderOnes() {
        assertThat(candidates("compose/edge/4.4/kafka.yml", "4.4", "4.4.0EDGEPE")).containsExactly(
                "compose/edge/4.4/kafka.yml",
                "compose/edge/4.3/kafka.yml",
                "compose/edge/3.9/kafka.yml");
    }

    @Test
    void versionWithItsOwnFolderUsesItFirst() {
        assertThat(candidates("compose/edge/4.4/kafka.yml", "4.4", "4.4.0.1EDGE")).startsWith(
                "compose/edge/4.4.0.1/kafka.yml",
                "compose/edge/4.4/kafka.yml");
    }

    @Test
    void versionWithoutItsOwnFolderFallsBackToTheNewestLowerOne() {
        assertThat(candidates("compose/edge/4.4/kafka.yml", "4.4", "4.4.1EDGE")).startsWith(
                "compose/edge/4.4.0.1/kafka.yml",
                "compose/edge/4.4/kafka.yml");
        assertThat(candidates("compose/edge/4.5/kafka.yml", "4.5", "4.5.0EDGE")).startsWith(
                "compose/edge/4.4.0.1/kafka.yml");
    }

    @Test
    void foldersAreComparedAsVersions() {
        assertThat(candidates("compose/edge/4.10/kafka.yml", "4.10", "4.10.0EDGE")).startsWith(
                "compose/edge/4.10/kafka.yml",
                "compose/edge/4.4.0.1/kafka.yml");
    }

    @Test
    void pathWithoutVersionFoldersIsUsedAsIs() {
        assertThat(candidates("compose/generic/default/default.yml", null, "default"))
                .containsExactly("compose/generic/default/default.yml");
        assertThat(candidates("compose/edge/3.8/kafka.yml", "3.8", "3.8.0EDGEPE"))
                .containsExactly("compose/edge/3.8/kafka.yml");
    }

    private static List<String> candidates(String path, String composeLine, String version) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("composeLine", composeLine);
        vars.put("version", version);
        return AppTemplateMaterializer.composePathCandidates(path, vars, EDGE_FOLDERS);
    }
}
