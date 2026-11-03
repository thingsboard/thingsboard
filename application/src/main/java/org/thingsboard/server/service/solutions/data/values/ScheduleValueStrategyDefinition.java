// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleValueStrategyDefinition implements ValueStrategyDefinition {

    private List<ValueStrategySchedule> schedule;
    private String timeZone;
    private ValueStrategyDefinition defaultDefinition;

    @Override
    public ValueStrategyDefinitionType getStrategyType() {
        return ValueStrategyDefinitionType.SCHEDULE;
    }
}
