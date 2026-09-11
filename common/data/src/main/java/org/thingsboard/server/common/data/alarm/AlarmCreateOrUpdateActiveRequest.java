// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@Builder
public class AlarmCreateOrUpdateActiveRequest implements AlarmModificationRequest {

    @NotNull
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @NoXss
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with alarm originator id")
    private EntityId originator;
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @Schema(description = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @Schema(description = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;

    @ToString.Exclude
    @NoXss
    @Schema(description = "JSON object with alarm details")
    private JsonNode details;

    @Valid
    @Schema(description = "JSON object with propagation details")
    private AlarmPropagationInfo propagation;

    private UserId userId;

    private AlarmId edgeAlarmId;

    public static AlarmCreateOrUpdateActiveRequest fromAlarm(Alarm a) {
        return fromAlarm(a, null);
    }

    public static AlarmCreateOrUpdateActiveRequest fromAlarm(Alarm a, UserId userId) {
        return fromAlarm(a, userId, null);
    }

    public static AlarmCreateOrUpdateActiveRequest fromAlarm(Alarm a, UserId userId, AlarmId edgeAlarmId) {
        return AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(a.getTenantId())
                .customerId(a.getCustomerId())
                .type(a.getType())
                .originator(a.getOriginator())
                .severity((a.getSeverity()))
                .startTs(a.getStartTs())
                .endTs(a.getEndTs())
                .details(a.getDetails())
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(a.isPropagate())
                        .propagateToOwner(a.isPropagateToOwner())
                        .propagateToTenant(a.isPropagateToTenant())
                        .propagateRelationTypes(a.getPropagateRelationTypes()).build())
                .userId(userId)
                .edgeAlarmId(edgeAlarmId)
                .build();
    }

}
