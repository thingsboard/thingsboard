// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Schema
@Data
public class UserListFilter implements UsersFilter {

    @ArraySchema(schema = @Schema(implementation = UUID.class))
    @NotEmpty
    private List<UUID> usersIds;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.USER_LIST;
    }

}
