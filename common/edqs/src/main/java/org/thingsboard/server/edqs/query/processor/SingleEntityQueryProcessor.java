// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

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
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        processAll(ed -> {
            if (checkCustomerId(customerId, ed)) {
                processor.accept(ed);
            }
        });
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
