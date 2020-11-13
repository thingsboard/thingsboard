package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.id.TenantId;

public interface TenantEntityDao {

    Long countByTenantId(TenantId tenantId);
}
