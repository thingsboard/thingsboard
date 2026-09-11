// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MicrosoftTeamsNotificationTargetConfig extends NotificationTargetConfig implements NotificationRecipient {

    @NotBlank
    private String webhookUrl;
    @NotEmpty
    private String channelName;
    private Boolean useOldApi = Boolean.TRUE;

    @Override
    public NotificationTargetType getType() {
        return NotificationTargetType.MICROSOFT_TEAMS;
    }

    @Override
    public Object getId() {
        return webhookUrl;
    }

    @Override
    public String getTitle() {
        return channelName;
    }

}
