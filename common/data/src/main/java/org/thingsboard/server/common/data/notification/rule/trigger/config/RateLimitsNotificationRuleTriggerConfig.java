// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.limit.LimitedApi;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class RateLimitsNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @ArraySchema(schema = @Schema(implementation = LimitedApi.class))
    private Set<LimitedApi> apis;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

    @Override
    public String getDeduplicationKey() {
        return apis == null ? "#" : apis.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

}
