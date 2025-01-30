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
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractSimpleQueryProcessor<T extends EntityFilter> extends AbstractSingleEntityTypeQueryProcessor<T> {

    private final EntityType entityType;

    public AbstractSimpleQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter, EntityType entityType) {
        super(repo, ctx, query, filter);
        this.entityType = entityType;
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        var customers = repository.getEntityMap(EntityType.CUSTOMER);
        for (UUID cId : repository.getAllCustomers(customerId)) {
            var customerData = (CustomerData) customers.get(cId);
            if (customerData != null) {
                process(customerData.getEntities(entityType), processor);
            }
        }
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        var genericReadResults = processCustomerGenericRead(customerId, readAttrPermissions, readTsPermissions);
        Map<UUID, SortableEntityData> mergedResult = new HashMap<>(genericReadResults.size());
        for (SortableEntityData sd : genericReadResults) {
            mergedResult.put(sd.getId(), sd);
        }

        for (GroupPermissions permissions : groupPermissions) {
            EntityGroupData egData = repository.getEntityGroup(permissions.groupId);
            for (EntityData<?> ed : egData.getEntities()) {
                SortableEntityData alreadyAdded = mergedResult.get(ed.getId());
                if (alreadyAdded != null) {
                    alreadyAdded.setReadAttrs(alreadyAdded.isReadAttrs() || permissions.readAttrs);
                    alreadyAdded.setReadTs(alreadyAdded.isReadTs() || permissions.readTs);
                } else {
                    if (matches(ed)) {
                        SortableEntityData sortData = toSortData(ed, permissions);
                        mergedResult.put(ed.getId(), sortData);
                    }
                }
            }
        }
        return new ArrayList<>(mergedResult.values());
    }

    @Override
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        for (GroupPermissions groupPermission : groupPermissions) {
            EntityGroupData egData = repository.getEntityGroup(groupPermission.groupId);
            process(egData.getEntities(), processor);
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(entityType), processor);
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
