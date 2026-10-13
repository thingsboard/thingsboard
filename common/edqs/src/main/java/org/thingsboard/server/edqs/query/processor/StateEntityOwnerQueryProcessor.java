// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class StateEntityOwnerQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<StateEntityOwnerFilter> {

    private final EntityId entityId;

    public StateEntityOwnerQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (StateEntityOwnerFilter) query.getEntityFilter());
        this.entityId = filter.getSingleEntity();
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        EntityData ed = repository.getEntityMap(entityId.getEntityType()).get(entityId.getId());
        if (ed != null && ed.getPermissionCustomerId() != null && matches(ed)) {
            if (customerId.equals(ed.getPermissionCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getPermissionCustomerId())) {
                processor.accept(repository.getEntityMap(EntityType.CUSTOMER).get(ed.getPermissionCustomerId()));
            }
        }
    }


    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        EntityData ed = repository.getEntityMap(entityId.getEntityType()).get(entityId.getId());
        if (!matches(ed)) {
            return Collections.emptyList();
        } else {
            boolean genericRead = customerId.equals(ed.getPermissionCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getPermissionCustomerId());
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), genericRead, readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                EntityData<?> customer = repository.getEntityMap(EntityType.CUSTOMER).get(ed.getPermissionCustomerId());
                SortableEntityData sortData = toSortData(customer, readAttrPermissions, readTsPermissions);
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
        EntityData ed = repository.getEntityMap(entityId.getEntityType()).get(entityId.getId());
        if (ed != null) {
            if (ed.getCustomerId() != null) {
                processor.accept(repository.getEntityMap(EntityType.CUSTOMER).get(ed.getCustomerId()));
            } else {
                processor.accept(repository.getEntityMap(EntityType.TENANT).get(repository.getTenantId().getId()));
            }
        }
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
