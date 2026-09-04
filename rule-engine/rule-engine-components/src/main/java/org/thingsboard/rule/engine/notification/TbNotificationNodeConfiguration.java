// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.notification;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.NotificationTemplateId;

import java.util.List;
import java.util.UUID;

@Data
public class TbNotificationNodeConfiguration implements NodeConfiguration<TbNotificationNodeConfiguration> {

    @NotEmpty
    private List<UUID> targets;
    @NotNull
    private NotificationTemplateId templateId;

    @Override
    public TbNotificationNodeConfiguration defaultConfiguration() {
        return new TbNotificationNodeConfiguration();
    }

}
