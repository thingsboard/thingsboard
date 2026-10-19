// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleListener;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbEntityViewService extends SimpleTbEntityService<EntityView>, ComponentLifecycleListener {

    EntityView save(EntityView entityView, List<EntityGroup> entityGroups, NameConflictStrategy nameConflictStrategy, SecurityUser user) throws Exception;

    void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException;

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndIdsAsync(TenantId tenantId, List<EntityViewId> entityViewIds);

}
