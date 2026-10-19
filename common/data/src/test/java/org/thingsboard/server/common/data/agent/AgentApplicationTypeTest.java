// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AgentApplicationTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "thingsboard/tb-edge-pe:4.2.0",
            "registry.example.com/thingsboard/tb-edge-pe:4.2.0",
            "registry.example.com:5000/thingsboard/tb-edge-pe:4.2.0",
            "my-mirror/proxy/thingsboard/tb-edge-pe:4.2.0",
            "thingsboard/tb-edge:4.5.0EDGE",
            "registry.example.com/thingsboard/tb-edge:4.5.0EDGE"
    })
    void edgePatternAcceptsDockerHubAndMirroredImages(String image) {
        assertThat(AgentApplicationType.EDGE.getMainImagePattern().matcher(image).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "thingsboard/tb-gateway:3.8.0",
            "registry.example.com/thingsboard/tb-gateway:3.8.0"
    })
    void gatewayPatternAcceptsDockerHubAndMirroredImages(String image) {
        assertThat(AgentApplicationType.GATEWAY.getMainImagePattern().matcher(image).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "thingsboard/tb-edge-pe",
            "thingsboard/tb-edge",
            "thingsboard/tb-edge-foo:4.2.0",
            "someone/tb-edge-pe:4.2.0",
            "someone/tb-edge:4.5.0EDGE",
            "registry.example.com/thingsboard/tb-gateway:3.8.0"
    })
    void edgePatternStillRejectsAnythingButTheEdgeImage(String image) {
        assertThat(AgentApplicationType.EDGE.getMainImagePattern().matcher(image).matches()).isFalse();
    }

    @Test
    void genericHasNoMainImagePattern() {
        assertThat(AgentApplicationType.GENERIC.getMainImagePattern()).isNull();
    }
}
