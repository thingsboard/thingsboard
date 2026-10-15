// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edqs.repo;

import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.function.Predicate;

public interface EdqsRepository {

    void processEvent(EdqsEvent event);

    long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query, boolean ignorePermissionCheck);

    PageData<QueryResult> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query, boolean ignorePermissionCheck);

    void clearIf(Predicate<TenantId> predicate);

    void clear();

}
