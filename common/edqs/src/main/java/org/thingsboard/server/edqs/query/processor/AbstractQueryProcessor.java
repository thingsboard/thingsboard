// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.query.EdqsDataQuery;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.edqs.util.RepositoryUtils.checkFilters;
import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public abstract class AbstractQueryProcessor<T extends EntityFilter> implements EntityQueryProcessor {

    protected final TenantRepo repository;
    protected final QueryContext ctx;
    protected final EdqsQuery query;
    protected final DataKey sortKey;
    protected final T filter;

    public AbstractQueryProcessor(TenantRepo repository, QueryContext ctx, EdqsQuery query, T filter) {
        this.repository = repository;
        this.ctx = ctx;
        this.query = query;
        this.sortKey = query instanceof EdqsDataQuery dataQuery ? dataQuery.getSortKey() : null;
        this.filter = filter;
    }

    protected SortableEntityData toSortData(EntityData<?> ed) {
        SortableEntityData sortData = new SortableEntityData(ed);
        sortData.setSortValue(getSortValue(ed, sortKey, ctx));
        return sortData;
    }

    protected void process(Collection<EntityData<?>> entities, Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : entities) {
            if (matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    protected static boolean checkCustomerId(UUID customerId, EntityData<?> ed) {
        return customerId.equals(ed.getCustomerId())
                || (ed.getEntityType() == EntityType.DASHBOARD && ed.getFields().getAssignedCustomerIds().contains(customerId))
                || (ed.getEntityType() == EntityType.CUSTOMER && customerId.equals(ed.getId()));
    }

    protected boolean matches(EntityData<?> ed) {
        return checkFilters(query, ed);
    }

}
