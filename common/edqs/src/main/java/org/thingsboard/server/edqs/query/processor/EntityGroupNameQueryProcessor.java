// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.EntityGroupData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.thingsboard.server.common.data.EntityType.ENTITY_GROUP;

public class EntityGroupNameQueryProcessor extends AbstractEntityGroupQueryProcessor<EntityGroupNameFilter> {

    private final String groupType;
    private final Pattern groupNamePattern;

    public EntityGroupNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupNameFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType().name();
        this.groupNamePattern = RepositoryUtils.toEntityNameSqlLikePattern(filter.getEntityGroupNameFilter());
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
    protected void processGroupsOnly(List<GroupPermissions> groupPermissions, Consumer<EntityData<?>> processor) {
        for (GroupPermissions groupPermission : groupPermissions) {
            EntityGroupData entityGroup = repository.getEntityGroup(groupPermission.groupId);
            if (matches(entityGroup)) {
                processor.accept(entityGroup);
            }
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(ENTITY_GROUP), processor);
    }

    @Override
    protected boolean matches(EntityData ed) {
        return super.matches(ed) && (groupNamePattern == null || groupNamePattern.matcher(ed.getFields().getName()).matches())
            && groupType.equals(ed.getFields().getType());
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
