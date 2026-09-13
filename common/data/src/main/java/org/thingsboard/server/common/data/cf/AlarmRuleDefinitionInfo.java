// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AlarmRuleDefinitionInfo extends AlarmRuleDefinition {

    private String entityName;

    public AlarmRuleDefinitionInfo(AlarmRuleDefinition alarmRuleDefinition, String entityName) {
        super(alarmRuleDefinition);
        this.entityName = entityName;
    }

    public static AlarmRuleDefinitionInfo fromCalculatedFieldInfo(CalculatedFieldInfo cfi) {
        AlarmRuleDefinition def = AlarmRuleDefinition.fromCalculatedField(cfi);
        return new AlarmRuleDefinitionInfo(def, cfi.getEntityName());
    }

}
