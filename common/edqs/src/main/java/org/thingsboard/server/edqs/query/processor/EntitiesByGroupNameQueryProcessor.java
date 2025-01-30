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
import org.thingsboard.server.common.data.edqs.fields.EntityGroupFields;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.thingsboard.server.common.data.EntityType.ENTITY_GROUP;
import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public class EntitiesByGroupNameQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntitiesByGroupNameFilter> {

    private final String groupType;
    private final UUID ownerId;
    private final Pattern pattern;

    public EntitiesByGroupNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntitiesByGroupNameFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType().name();
        this.ownerId = filter.getOwnerId() != null ? filter.getOwnerId().getId() : null;
        this.pattern = RepositoryUtils.toSqlLikePattern(filter.getEntityGroupNameFilter());
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        var customers = repository.getEntityMap(EntityType.CUSTOMER);
        for (UUID cId : repository.getAllCustomers(customerId)) {
            var customerData = (CustomerData) customers.get(cId);
            if (customerData != null) {
                process(customerData.getEntities(ENTITY_GROUP), processor);
            }
        }
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
        Collection<EntityData<?>> entities = new HashSet<>(getProbableResultSize());
        for (GroupPermissions groupPermission : groupPermissions) {
            entities.add(repository.getEntityGroup(groupPermission.groupId));
        }
        process(entities, processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(ENTITY_GROUP), processor);
    }

    @Override
    protected void process(Collection<EntityData<?>> entities, Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : entities) {
            if (matches(ed)) {
                Collection<EntityData<?>> groupEntities = repository.getEntityGroup(ed.getId()).getEntities();
                for (EntityData<?> groupEntity : groupEntities) {
                    processor.accept(groupEntity);
                }
                return;
            }
        }
    }

    @Override
    protected boolean matches(EntityData ed) {
        EntityGroupFields fields = (EntityGroupFields)ed.getFields();
        return super.matches(ed) && groupType.equals(fields.getType())
                && (pattern == null || pattern.matcher(fields.getName()).matches())
                && (ownerId == null || ownerId.equals(fields.getOwnerId()));
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
