/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleListener;

import java.util.List;

public interface TbEntityViewService extends ComponentLifecycleListener {

    EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws Exception;

    EntityView save(EntityView entityView, EntityView existingEntityView, NameConflictStrategy nameConflictStrategy, User user) throws Exception;

    void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException;

    void delete(EntityView entity, User user) throws ThingsboardException;

    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    EntityView assignEntityViewToPublicCustomer(TenantId tenantId, EntityViewId entityViewId, User user) throws ThingsboardException;

    EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException;

    EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView, Edge edge, User user) throws ThingsboardException;

    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException;

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);
}
