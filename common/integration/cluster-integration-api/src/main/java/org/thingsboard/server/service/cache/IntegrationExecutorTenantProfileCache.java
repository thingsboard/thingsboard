// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import org.thingsboard.server.cache.limits.TenantProfileProvider;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;

public interface IntegrationExecutorTenantProfileCache extends TenantProfileProvider {

    TenantProfile get(TenantId tenantId);

    void evict(TenantProfileId profileId);

    void evict(TenantId tenantId);

}
