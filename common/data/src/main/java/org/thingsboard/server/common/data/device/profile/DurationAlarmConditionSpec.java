// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.query.FilterPredicateValue;

import java.util.concurrent.TimeUnit;

@Schema(description = "Duration Alarm Condition Specification")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class DurationAlarmConditionSpec implements AlarmConditionSpec {

    @Schema(description = "Duration time unit")
    private TimeUnit unit;
    @Schema(description = "Duration predicate")
    private FilterPredicateValue<Long> predicate;

    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.DURATION;
    }
}
