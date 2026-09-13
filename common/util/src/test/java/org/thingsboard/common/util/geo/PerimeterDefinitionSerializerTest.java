// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util.geo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class PerimeterDefinitionSerializerTest {

    @Test
    void shouldSerializeCircle() {
        PerimeterDefinition circle = new CirclePerimeterDefinition(50.45, 30.52, 120.0);

        String json = JacksonUtil.writeValueAsString(circle);

        JsonNode actual = JacksonUtil.toJsonNode(json);
        assertThat(actual.get("latitude").asDouble()).isEqualTo(50.45);
        assertThat(actual.get("longitude").asDouble()).isEqualTo(30.52);
        assertThat(actual.get("radius").asDouble()).isEqualTo(120.0);
    }

    @Test
    void shouldSerializePolygon() throws Exception {
        String rawArray = "[[50.45,30.52],[50.46,30.53],[50.44,30.54]]";
        PerimeterDefinition polygon = new PolygonPerimeterDefinition(rawArray);

        String json = JacksonUtil.writeValueAsString(polygon);

        JsonNode actual = JacksonUtil.toJsonNode(json);
        JsonNode expected = JacksonUtil.toJsonNode(rawArray);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.isArray()).isTrue();
        assertThat(actual.size()).isEqualTo(3);
    }

}
