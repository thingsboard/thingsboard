// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformUsersNotificationTargetConfig extends NotificationTargetConfig {

    @NotNull
    @Valid
    private UsersFilter usersFilter;

    @Override
    public NotificationTargetType getType() {
        return NotificationTargetType.PLATFORM_USERS;
    }

}
