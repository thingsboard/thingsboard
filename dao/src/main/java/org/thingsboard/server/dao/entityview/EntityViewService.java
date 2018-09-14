/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public interface EntityViewService {

    EntityView findEntityViewById(EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    EntityView saveEntityView(EntityView entityView);

    EntityView assignEntityViewToCustomer(EntityViewId entityViewId, CustomerId customerId);

    EntityView unassignEntityViewFromCustomer(EntityViewId entityViewId);

    void deleteEntityView(EntityViewId entityViewId);

    TextPageData<EntityView> findEntityViewByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewByTenantIdAndEntityId(TenantId tenantId, EntityId entityId,
                                                                 TextPageLink pageLink);

    void deleteEntityViewByTenantId(TenantId tenantId);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,
                                                                    TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndEntityId(TenantId tenantId,
                                                                               CustomerId customerId,
                                                                               EntityId entityId,
                                                                               TextPageLink pageLink);

    void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId);

    ListenableFuture<EntityView> findEntityViewByIdAsync(EntityViewId entityViewId);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(EntityViewSearchQuery query);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);
}
