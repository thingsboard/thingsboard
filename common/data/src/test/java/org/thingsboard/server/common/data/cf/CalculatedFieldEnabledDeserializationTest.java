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
package org.thingsboard.server.common.data.cf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculatedFieldEnabledDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /*
     * The 'enabled' field was added together with the enable/disable feature. Calculated fields and alarm rules
     * exported before this change have no 'enabled' property in their JSON. Importing such JSON must keep them
     * enabled (backward compatible), which relies on the 'enabled = true' field initializer being preserved by
     * Jackson when the property is absent.
     */

    @Test
    public void givenCalculatedFieldJsonWithoutEnabled_whenDeserialized_thenEnabledIsTrue() throws Exception {
        String json = "{\"name\":\"Test CF\",\"type\":\"SIMPLE\",\"configurationVersion\":1}";

        CalculatedField cf = mapper.readValue(json, CalculatedField.class);

        assertThat(cf.isEnabled()).isTrue();
    }

    @Test
    public void givenCalculatedFieldJsonWithEnabledFalse_whenDeserialized_thenEnabledIsFalse() throws Exception {
        String json = "{\"name\":\"Test CF\",\"type\":\"SIMPLE\",\"enabled\":false}";

        CalculatedField cf = mapper.readValue(json, CalculatedField.class);

        assertThat(cf.isEnabled()).isFalse();
    }

    @Test
    public void givenAlarmRuleDefinitionJsonWithoutEnabled_whenDeserialized_thenEnabledIsTrue() throws Exception {
        String json = "{\"name\":\"Test Alarm Rule\",\"configurationVersion\":1}";

        AlarmRuleDefinition alarmRule = mapper.readValue(json, AlarmRuleDefinition.class);

        assertThat(alarmRule.isEnabled()).isTrue();
    }

    @Test
    public void givenAlarmRuleDefinitionJsonWithEnabledFalse_whenDeserialized_thenEnabledIsFalse() throws Exception {
        String json = "{\"name\":\"Test Alarm Rule\",\"enabled\":false}";

        AlarmRuleDefinition alarmRule = mapper.readValue(json, AlarmRuleDefinition.class);

        assertThat(alarmRule.isEnabled()).isFalse();
    }

}
