// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Schema
@Data
public class TenantAdministratorsFilter implements SystemLevelUsersFilter {

    @ArraySchema(schema = @Schema(implementation = UUID.class))
    private Set<UUID> tenantsIds;
    @ArraySchema(schema = @Schema(implementation = UUID.class))
    private Set<UUID> tenantProfilesIds;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.TENANT_ADMINISTRATORS;
    }

}
