// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
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
