// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface EntityViewService extends EntityDaoService {

    EntityView saveEntityView(EntityView entityView);

    EntityView saveEntityView(EntityView entityView, NameConflictStrategy nameConflictStrategy);

    EntityView saveEntityView(EntityView entityView, boolean doValidate);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    EntityViewInfo findEntityViewInfoById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId, boolean putInCache);

    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<EntityView> findEntityViewByTenantIdAndNameAsync(TenantId tenantId, String name);

    PageData<EntityView> findEntityViewByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, PageLink pageLink, String type);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(TenantId tenantId, List<EntityViewId> entityViewIds);

    List<EntityView> findEntityViewsByTenantIdAndIds(TenantId tenantId, List<EntityViewId> entityViewIds);

    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, PageLink pageLink, String type);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    List<EntityView> findEntityViewsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId);

    boolean existsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId);

    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    void deleteEntityViewsByTenantId(TenantId tenantId);

    void deleteEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);

    PageData<EntityView> findEntityViewsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<EntityView> findEntityViewsByEntityGroupIdsAndType(List<EntityGroupId> groupIds, String type, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findTenantEntityViewInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityViewInfo> findTenantEntityViewInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

}
