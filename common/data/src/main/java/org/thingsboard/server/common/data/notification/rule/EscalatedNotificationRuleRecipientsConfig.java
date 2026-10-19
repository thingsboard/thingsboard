// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Escalated notification rule recipients configuration")
@Data
@EqualsAndHashCode
public class EscalatedNotificationRuleRecipientsConfig implements NotificationRuleRecipientsConfig {

    @NotEmpty
    private Map<Integer, List<UUID>> escalationTable;

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

    @Override
    public Map<Integer, List<UUID>> getTargetsTable() {
        return escalationTable;
    }

}
