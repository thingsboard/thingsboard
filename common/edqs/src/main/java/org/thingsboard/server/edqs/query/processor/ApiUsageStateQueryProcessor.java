/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ApiUsageStateFields;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ApiUsageStateQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<ApiUsageStateFilter> {

    public ApiUsageStateQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (ApiUsageStateFilter) query.getEntityFilter());
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        CustomerData customerData = (CustomerData) repository.getEntityMap(EntityType.CUSTOMER).get(customerId);
        process(customerData.getEntities(EntityType.API_USAGE_STATE), processor);
    }


    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        CustomerData customerData = (CustomerData) repository.getEntityMap(EntityType.CUSTOMER).get(customerId);
        Collection<EntityData<?>> entities = customerData.getEntities(EntityType.API_USAGE_STATE);
        EntityData<?> ed = entities.iterator().next();
        if (entities.isEmpty() || !matches(ed)) {
            return Collections.emptyList();
        } else {
            boolean genericRead = customerId.equals(ed.getCustomerId());
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), genericRead, readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                SortableEntityData sortData = toSortData(customerData, permissions);
                return Collections.singletonList(sortData);
            } else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        processAll(processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(EntityType.API_USAGE_STATE), processor);
    }

    @Override
    protected boolean matches(EntityData<?> ed) {
        ApiUsageStateFields entityFields = (ApiUsageStateFields) ed.getFields();
        return super.matches(ed) && (filter.getCustomerId() != null ? entityFields.getEntityId().equals(filter.getCustomerId()) :
                entityFields.getEntityId().equals(repository.getTenantId()));
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
