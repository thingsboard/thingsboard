// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.UUID;

/**
 * Whose queues a tenant's messages run on: the system queues, or its own when its profile isolates the rule engine.
 * A tenant without a profile is treated as on the system queues, as the default profile is not isolated.
 */
public final class QueueOwners {

    private QueueOwners() {
    }

    public static UUID resolveQueueTenantId(TbTenantProfileCache tenantProfileCache, UUID tenantId) {
        TenantProfile profile = tenantProfileCache.get(TenantId.fromUUID(tenantId));
        boolean usesSystemQueues = profile == null || !profile.isIsolatedTbRuleEngine();
        return usesSystemQueues ? TenantId.SYS_TENANT_ID.getId() : tenantId;
    }

}
