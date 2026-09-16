// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

public interface EntityCountService {

    long countByTenantIdAndEntityType(TenantId tenantId, EntityType entityType);

    void publishCountEntityEvictEvent(TenantId tenantId, EntityType entityType);
}
