// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.id.DashboardId;

@Schema
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmRule {

    @Valid
    @NotNull
    private AlarmCondition condition;
    private String alarmDetails;
    private DashboardId dashboardId;

    @JsonIgnore
    public boolean requiresScheduledReevaluation() {
        return condition.requiresScheduledReevaluation();
    }

}
