// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TracePathDetailsDTOTest {

    @Test
    void givenPathDetails_whenSerialized_thenPathFieldsStayAtTheTopLevel() {
        TracePathDetailsDTO details = TracePathDetailsDTO.builder()
                .pathId("path-1")
                .rootMessageType("POST_TELEMETRY_REQUEST")
                .processedTraces(10)
                .errorCount(2)
                .ruleNodeTree(List.of())
                .build();

        JsonNode json = JacksonUtil.valueToTree(details);

        assertThat(json.get("pathId").asText()).isEqualTo("path-1");
        assertThat(json.get("rootMessageType").asText()).isEqualTo("POST_TELEMETRY_REQUEST");
        assertThat(json.get("processedTraces").asLong()).isEqualTo(10);
        assertThat(json.get("errorCount").asLong()).isEqualTo(2);
        assertThat(json.get("ruleNodeTree").isArray()).isTrue();
        assertThat(JacksonUtil.treeToValue(json, TracePathDetailsDTO.class)).isEqualTo(details);
    }

}
