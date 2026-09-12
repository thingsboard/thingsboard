// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.repo;

import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

import java.util.function.Predicate;

public interface EdqsRepository {

    void processEvent(EdqsEvent event);

    long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query, boolean ignorePermissionCheck);

    PageData<QueryResult> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query, boolean ignorePermissionCheck);

    void clearIf(Predicate<TenantId> predicate);

    void clear();

}
