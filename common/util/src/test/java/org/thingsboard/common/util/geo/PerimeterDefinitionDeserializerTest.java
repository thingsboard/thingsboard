/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PerimeterDefinitionDeserializerTest {

    @Test
    void shouldDeserializeCircle() {
        String json = """
                {"latitude":50.45,"longitude":30.52,"radius":100.0}""";

        PerimeterDefinition def = JacksonUtil.fromString(json, PerimeterDefinition.class);

        assertThat(def).isNotNull().isInstanceOf(CirclePerimeterDefinition.class);

        CirclePerimeterDefinition circle = (CirclePerimeterDefinition) def;
        assertThat(circle.getLatitude()).isEqualTo(50.45);
        assertThat(circle.getLongitude()).isEqualTo(30.52);
        assertThat(circle.getRadius()).isEqualTo(100.0);
    }

    @Test
    void shouldDeserializePolygon() {
        String json = "[[50.45,30.52],[50.46,30.53],[50.44,30.54]]";

        PerimeterDefinition def = JacksonUtil.fromString(json, PerimeterDefinition.class);

        assertThat(def).isInstanceOf(PolygonPerimeterDefinition.class);
        PolygonPerimeterDefinition poly = (PolygonPerimeterDefinition) def;
        assertThat(poly.getPolygonDefinition()).isEqualTo(json);
    }

    @Test
    void shouldThrowWhenCircleFieldIsMissing() {
        // Missing "radius"
        String badJson = """
            {
                "latitude": 48.8566,
                "longitude": 2.3522
            }
            """;

        String customError = "Custom error context";

        assertThatThrownBy(() -> JacksonUtil.fromString(badJson, PerimeterDefinition.class, customError))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(customError)
                .hasCauseInstanceOf(JsonMappingException.class)
                .rootCause()
                .hasMessageContaining("CirclePerimeterDefinition missing required fields");
    }

    @Test
    void shouldThrowWhenJsonIsGarbage() {
        String garbageJson = "\"NotAnObjectOrArray\"";
        String customError = "Garbage check";

        assertThatThrownBy(() -> JacksonUtil.fromString(garbageJson, PerimeterDefinition.class, customError))
                .isInstanceOf(IllegalArgumentException.class)
                .hasCauseInstanceOf(JsonMappingException.class)
                .rootCause()
                .hasMessageContaining("Unknown JSON format");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        //noinspection ConstantConditions
        PerimeterDefinition result = JacksonUtil.fromString(null, PerimeterDefinition.class, "Error");
        assertThat(result).isNull();
    }

}
