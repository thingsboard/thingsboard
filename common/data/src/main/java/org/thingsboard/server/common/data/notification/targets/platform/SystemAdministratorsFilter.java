// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class SystemAdministratorsFilter implements SystemLevelUsersFilter {

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.SYSTEM_ADMINISTRATORS;
    }

}
