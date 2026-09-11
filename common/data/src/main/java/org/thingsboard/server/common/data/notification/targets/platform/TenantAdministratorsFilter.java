// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.platform;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class TenantAdministratorsFilter implements UsersFilter {

    private Set<UUID> tenantsIds;
    private Set<UUID> tenantProfilesIds;

    @Override
    public UsersFilterType getType() {
        return UsersFilterType.TENANT_ADMINISTRATORS;
    }

}
