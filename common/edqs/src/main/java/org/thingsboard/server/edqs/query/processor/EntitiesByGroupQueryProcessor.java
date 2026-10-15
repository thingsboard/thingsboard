// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
                sortData.setSortValue(getSortValue(ed, sortKey, ctx));
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
                if (super.matches(ed)) {
                    processor.accept(ed);
                }
            }
        }
    }

    @Override
    protected boolean matches(EntityData ed) {
        return ed.getFields() != null && groupType.equals(ed.getFields().getType());
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
