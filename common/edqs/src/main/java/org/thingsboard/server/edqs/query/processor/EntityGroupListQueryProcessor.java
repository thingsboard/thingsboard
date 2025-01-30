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
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EntityGroupListQueryProcessor extends AbstractEntityGroupQueryProcessor<EntityGroupListFilter> {

    private final String groupType;
    private final Set<UUID> groupIds;

    public EntityGroupListQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupListFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType().name();
        this.groupIds = filter.getEntityGroupList().stream().map(UUID::fromString).collect(Collectors.toSet());
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
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        Set<UUID> allowedGroupIds = groupPermissions.stream().map(GroupPermissions::getGroupId)
                .filter(this.groupIds::contains).collect(Collectors.toSet());

        checkGroupIds(allowedGroupIds, processor);
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        checkGroupIds(groupIds, processor);
    }

    @Override
    protected int getProbableResultSize() {
        return groupIds.size();
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed) && groupType.equals(ed.getFields().getType());
    }

    private void checkGroupIds(Set<UUID> allowedGroupIds, Consumer<EntityData<?>> processor) {
        for (UUID groupId : allowedGroupIds) {
            EntityGroupData entityGroup = repository.getEntityGroup(groupId);
            if (matches(entityGroup)) {
                processor.accept(entityGroup);
            }
        }
    }

}
