/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
