// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityGroupFields;
import org.thingsboard.server.common.data.id.EntityId;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.thingsboard.server.common.data.EntityType.ENTITY_GROUP;
import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public class EntitiesByGroupNameQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntitiesByGroupNameFilter> {

    private final String groupType;
    private final UUID ownerId;
    private final EntityType ownerType;
    private final Pattern pattern;
    private final Set<UUID> allCustomers;

    public EntitiesByGroupNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntitiesByGroupNameFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType().name();
        this.ownerId = filter.getOwnerId() != null ? filter.getOwnerId().getId() : null;
        this.ownerType = filter.getOwnerId() != null ? filter.getOwnerId().getEntityType() : null;
        this.pattern = RepositoryUtils.toEntityNameSqlLikePattern(filter.getEntityGroupNameFilter());
        if (ctx.getCustomerId() != null) {
            allCustomers = repo.getAllCustomers(ctx.getCustomerId().getId());
        } else {
            allCustomers = null;
        }
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
                    if (super.matches(groupEntity)) {
                        processor.accept(groupEntity);
                    }
                }
                return;
            }
        }
    }

    @Override
    protected boolean matches(EntityData ed) {
        EntityGroupFields fields = (EntityGroupFields)ed.getFields();
        return groupType.equals(fields.getType())
                && (pattern == null || pattern.matcher(fields.getName()).matches())
                && checkOwnerId(fields);
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

    private boolean checkOwnerId(EntityGroupFields fields) {
        if (ownerId != null) {
            return ownerId.equals(fields.getOwnerId()) && ownerType.equals(fields.getOwnerType());
        } else if (isEntityFromGenericPart(fields)) {
            return fields.getOwnerId().equals(getCtxOwnerId(ctx).getId())
                    && fields.getOwnerType().equals(getCtxOwnerId(ctx).getEntityType());
        }
        return true;
    }

    private boolean isEntityFromGenericPart(EntityGroupFields fields) {
        return allCustomers == null || allCustomers.contains(fields.getCustomerId());
    }

    public EntityId getCtxOwnerId(QueryContext ctx) {
        if (ctx.isTenantUser()) {
            return ctx.getTenantId();
        } else {
            return ctx.getCustomerId();
        }
    }

}
