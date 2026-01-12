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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;

@ActiveProfiles("install")
@SpringBootTest(classes = DefaultDataUpdateService.class)
class DefaultDataUpdateServiceTest {

    @MockBean
    DefaultDataUpdateService service;

    @BeforeEach
    void setUp() {
        willCallRealMethod().given(service).convertDeviceProfileAlarmRulesForVersion330(any());
        willCallRealMethod().given(service).convertDeviceProfileForVersion330(any());
    }

    JsonNode readFromResource(String resourceName) throws IOException {
        return JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330FirstRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_in.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isTrue();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330SecondRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_out.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330EmptyJson() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ }");
        JsonNode expected = JacksonUtil.toJsonNode("{ }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330AlarmNodeNull() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ \"alarms\" : null }");
        JsonNode expected = JacksonUtil.toJsonNode("{ \"alarms\" : null }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330NoAlarmNode() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ \"configuration\": { \"type\": \"DEFAULT\" } }");
        JsonNode expected = JacksonUtil.toJsonNode("{ \"configuration\": { \"type\": \"DEFAULT\" } }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

}
