// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.usagerecord;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface ApiUsageStateService extends EntityDaoService {

    ApiUsageState createDefaultApiUsageState(TenantId id, EntityId entityId);

    ApiUsageState update(ApiUsageState apiUsageState);

    ApiUsageState findTenantApiUsageState(TenantId tenantId);

    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    void deleteApiUsageStateByEntityId(EntityId entityId);

    ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id);

}
