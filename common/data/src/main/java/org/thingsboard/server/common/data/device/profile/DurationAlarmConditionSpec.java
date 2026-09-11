// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.query.FilterPredicateValue;

import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurationAlarmConditionSpec implements AlarmConditionSpec {

    private TimeUnit unit;
    private FilterPredicateValue<Long> predicate;

    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.DURATION;
    }
}
