// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.query.FilterPredicateValue;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatingAlarmConditionSpec implements AlarmConditionSpec {

    private FilterPredicateValue<Integer> predicate;

    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.REPEATING;
    }
}
