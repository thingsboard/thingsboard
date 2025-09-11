/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
