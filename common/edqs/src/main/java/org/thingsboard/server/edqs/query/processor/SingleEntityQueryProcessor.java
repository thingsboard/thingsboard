// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SingleEntityQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<SingleEntityFilter> {

    private final EntityType entityType;
    private final UUID entityId;

    public SingleEntityQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (SingleEntityFilter) query.getEntityFilter());
        this.entityType = filter.getSingleEntity().getEntityType();
        this.entityId = filter.getSingleEntity().getId();
    }

    @Override
    protected void processCustomerGenericRead(UUID customerId, Consumer<EntityData<?>> processor) {
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (ed != null && ed.getPermissionCustomerId() != null && matches(ed)) {
            if (customerId.equals(ed.getPermissionCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getPermissionCustomerId())) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected List<SortableEntityData> processCustomerGenericReadWithGroups(UUID customerId, boolean readAttrPermissions, boolean readTsPermissions, List<GroupPermissions> groupPermissions) {
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (!matches(ed)) {
            return Collections.emptyList();
        } else {
            boolean genericRead = customerId.equals(ed.getPermissionCustomerId()) || repository.getAllCustomers(customerId).contains(ed.getPermissionCustomerId());
            CombinedPermissions permissions = getCombinedPermissions(ed.getId(), genericRead, readAttrPermissions, readTsPermissions, groupPermissions);
            if (permissions.isRead()) {
                SortableEntityData sortData = toSortData(ed, permissions);
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
        EntityData ed = repository.getEntityMap(entityType).get(entityId);
        if (matches(ed)) {
            processor.accept(ed);
        }
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
