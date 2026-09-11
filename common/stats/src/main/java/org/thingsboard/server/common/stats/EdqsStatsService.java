// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.stats;

import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

public interface EdqsStatsService {

    void reportAdded(ObjectType objectType);

    void reportRemoved(ObjectType objectType);

    void reportEntityDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos);

    void reportEntityCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos);

    void reportEdqsDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos);

    void reportEdqsCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos);

    void reportStringCompressed();

    void reportStringUncompressed();

}
