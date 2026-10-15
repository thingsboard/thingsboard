// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.usagerecord;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.UUID;

public interface ApiUsageStateDao extends Dao<ApiUsageState>, TenantEntityDao<ApiUsageState> {

    /**
     * Save or update usage record object
     *
     * @param apiUsageState the usage record
     * @return saved usage record entity
     */
    ApiUsageState save(TenantId tenantId, ApiUsageState apiUsageState);

    /**
     * Find usage record by tenantId.
     *
     * @param tenantId the tenantId
     * @return the corresponding usage record
     */
    ApiUsageState findTenantApiUsageState(UUID tenantId);

    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    /**
     * Delete usage record by tenantId.
     *
     * @param tenantId the tenantId
     */
    void deleteApiUsageStateByTenantId(TenantId tenantId);

    void deleteApiUsageStateByEntityId(EntityId entityId);

}
