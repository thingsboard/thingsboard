// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.slack;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class SlackNotificationTargetConfig extends NotificationTargetConfig {

    private SlackConversationType conversationType;
    @NotNull
    @Valid
    private SlackConversation conversation;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the notification target")
    @Override
    public NotificationTargetType getType() {
        return NotificationTargetType.SLACK;
    }

}
