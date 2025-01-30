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

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public class EntitiesByGroupQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntityGroupFilter> {

    private final String groupType;
    private final UUID groupId;

    public EntitiesByGroupQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupFilter) query.getEntityFilter());
        groupId = UUID.fromString(filter.getEntityGroup());
        groupType = filter.getGroupType().name();
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        var customers = repository.getAllCustomers(customerId);
        processAll(ed -> {
            if (checkCustomerHierarchy(customers, ed)) {
                processor.accept(ed);
            }
        });
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        List<SortableEntityData> result = new ArrayList<>(getProbableResultSize());
        var customers = repository.getAllCustomers(customerId);
        processAll(ed -> {
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), checkCustomerHierarchy(customers, ed), readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                SortableEntityData sortData = new SortableEntityData(ed);
                sortData.setSortValue(getSortValue(ed, sortKey));
                sortData.setReadAttrs(permissions.isReadAttrs());
                sortData.setReadTs(permissions.isReadTs());
                result.add(sortData);
            }
        });
        return result;
    }

    @Override
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        processAll(processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        EntityGroupData entityGroup = repository.getEntityGroup(groupId);
        if (matches(entityGroup)) {
            for (EntityData<?> ed : entityGroup.getEntities()) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed) && groupType.equals(ed.getFields().getType());
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
