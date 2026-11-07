// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.converter;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterDefaultsResourcesTest {

    private static final String DEFAULT_DIR = "default";
    private static final String TEST_DIR = "test";
    private static final String INTEGRATION_FILE = "integration.json";
    private static final Set<String> DIRECTIONS = Set.of("uplink", "downlink");
    private static final Set<String> DEFAULT_FILES = Set.of("tbel.raw", "js.raw", "js-v2.raw");
    private static final Set<String> INTEGRATION_FILES = Set.of("tbel.raw", "js.raw", "payload.json", "metadata.json");
    private static final Set<String> TEST_CASE_FILES = Set.of("expected.json", "configuration.json", "payload.json");

    @Test
    void converterResourcesFollowLayout() throws IOException {
        Map<String, Resource> files = listFiles();
        assertThat(files).isNotEmpty();

        Set<String> topDirs = Arrays.stream(IntegrationType.values())
                .map(IntegrationType::getDirectory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        topDirs.add(DEFAULT_DIR);

        SoftAssertions softly = new SoftAssertions();
        files.forEach((path, resource) -> {
            String[] parts = path.split("/");
            softly.assertThat(parts[0]).as(path + ": unknown integration directory").isIn(topDirs);
            if (parts.length == 2 && !DEFAULT_DIR.equals(parts[0])) {
                softly.assertThat(parts[1]).as(path + ": unexpected file").isEqualTo(INTEGRATION_FILE);
            } else {
                softly.assertThat(parts.length > 1 ? parts[1] : "").as(path + ": unknown direction").isIn(DIRECTIONS);
                if (parts.length == 3) {
                    Set<String> allowed = DEFAULT_DIR.equals(parts[0]) ? DEFAULT_FILES : INTEGRATION_FILES;
                    softly.assertThat(parts[2]).as(path + ": unexpected file").isIn(allowed);
                } else {
                    softly.assertThat(parts.length).as(path + ": expected <integration>/<direction>/test/<case>/<file>").isEqualTo(5);
                    softly.assertThat(parts.length > 2 ? parts[2] : "").as(path).isEqualTo(TEST_DIR);
                    softly.assertThat(parts[parts.length - 1]).as(path + ": unexpected test case file").isIn(TEST_CASE_FILES);
                }
            }
            if (path.endsWith(".json")) {
                softly.assertThatCode(() -> JacksonUtil.toJsonNode(resource.getContentAsString(StandardCharsets.UTF_8)))
                        .as(path + ": invalid JSON")
                        .doesNotThrowAnyException();
            }
        });

        files.keySet().stream()
                .filter(path -> path.endsWith("/uplink/payload.json") || path.endsWith("/downlink/metadata.json"))
                .map(path -> path.substring(0, path.lastIndexOf('/')))
                .filter(dir -> files.containsKey(dir + "/tbel.raw") || files.containsKey(dir + "/js.raw"))
                .forEach(dir -> softly.assertThat(files.keySet())
                        .as(dir + ": template and example without a test case")
                        .anyMatch(path -> path.startsWith(dir + "/" + TEST_DIR + "/")));
        softly.assertAll();
    }

    private Map<String, Resource> listFiles() throws IOException {
        Map<String, Resource> files = new HashMap<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:converters/**/*.*")) {
            if (!"file".equals(resource.getURL().getProtocol())) {
                continue;
            }
            String url = resource.getURL().getPath();
            files.put(url.substring(url.lastIndexOf("/converters/") + "/converters/".length()), resource);
        }
        return files;
    }

}
