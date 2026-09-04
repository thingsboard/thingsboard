// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserListFilter implements UsersFilter {

    @NotEmpty
    private List<UUID> usersIds;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.USER_LIST;
    }

}
