// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

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
public class RateLimitsNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

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
