// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemVersionInfoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesAllFieldsInFixedOrderWhenBuildInfoIsPresent() throws Exception {
        SystemVersionInfo info = new SystemVersionInfo("4.4.1", "application", "ThingsBoard Server Application", "PE");

        assertThat(mapper.writeValueAsString(info))
                .isEqualTo("{\"version\":\"4.4.1\",\"artifact\":\"application\",\"name\":\"ThingsBoard Server Application\",\"type\":\"PE\"}");
    }

    @Test
    void omitsArtifactAndNameWhenBuildInfoIsMissing() throws Exception {
        SystemVersionInfo info = new SystemVersionInfo("unknown", null, null, "PE");

        assertThat(mapper.writeValueAsString(info))
                .isEqualTo("{\"version\":\"unknown\",\"type\":\"PE\"}");
    }

}
