// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.targets.platform;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserGroupListFilter implements UsersFilter {

    @NotEmpty
    private List<UUID> groupsIds;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.USER_GROUP_LIST;
    }

}
