// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultNotificationRuleRecipientsConfig extends NotificationRuleRecipientsConfig {

    @NotEmpty
    private List<UUID> targets;

    @Override
    public Map<Integer, List<UUID>> getTargetsTable() {
        return Map.of(0, targets);
    }

}
