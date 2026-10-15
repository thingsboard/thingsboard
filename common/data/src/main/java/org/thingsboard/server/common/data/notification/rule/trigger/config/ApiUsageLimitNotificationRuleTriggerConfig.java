// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger.config;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageStateValue;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class ApiUsageLimitNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @ArraySchema(schema = @Schema(implementation = ApiFeature.class))
    private Set<ApiFeature> apiFeatures;
    @ArraySchema(schema = @Schema(implementation = ApiUsageStateValue.class))
    private Set<ApiUsageStateValue> notifyOn;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.API_USAGE_LIMIT;
    }

}
