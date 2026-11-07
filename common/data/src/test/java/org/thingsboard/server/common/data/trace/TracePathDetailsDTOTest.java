// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TracePathDetailsDTOTest {

    @Test
    void givenPathDetails_whenSerialized_thenPathFieldsStayAtTheTopLevel() throws Exception {
        TracePathDetailsDTO details = TracePathDetailsDTO.builder()
                .pathId("path-1")
                .rootMessageType("POST_TELEMETRY_REQUEST")
                .processedTraces(10)
                .errorCount(2)
                .ruleNodeTree(List.of())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(details);

        assertThat(json.get("pathId").asText()).isEqualTo("path-1");
        assertThat(json.get("rootMessageType").asText()).isEqualTo("POST_TELEMETRY_REQUEST");
        assertThat(json.get("processedTraces").asLong()).isEqualTo(10);
        assertThat(json.get("errorCount").asLong()).isEqualTo(2);
        assertThat(json.get("ruleNodeTree").isArray()).isTrue();
        assertThat(mapper.treeToValue(json, TracePathDetailsDTO.class)).isEqualTo(details);
    }

}
