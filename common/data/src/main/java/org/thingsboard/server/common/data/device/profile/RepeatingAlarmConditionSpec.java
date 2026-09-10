// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.query.FilterPredicateValue;

@Schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class RepeatingAlarmConditionSpec implements AlarmConditionSpec {

    @Schema(description = "Repeating predicate")
    private FilterPredicateValue<Integer> predicate;

    @Override
    @Schema(description = "Type of the Alarm Condition Specification", requiredMode = Schema.RequiredMode.REQUIRED)
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.REPEATING;
    }
}
