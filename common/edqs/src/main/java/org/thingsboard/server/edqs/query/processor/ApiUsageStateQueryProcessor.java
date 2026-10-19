// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ApiUsageStateFields;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.UUID;
import java.util.function.Consumer;

public class ApiUsageStateQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<ApiUsageStateFilter> {

    public ApiUsageStateQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (ApiUsageStateFilter) query.getEntityFilter());
    }

    @Override
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        CustomerData customerData = (CustomerData) repository.getEntityMap(EntityType.CUSTOMER).get(customerId);
        if (customerData != null) {
            process(customerData.getEntities(EntityType.API_USAGE_STATE), processor);
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(EntityType.API_USAGE_STATE), processor);
    }

    @Override
    protected boolean matches(EntityData<?> ed) {
        ApiUsageStateFields entityFields = (ApiUsageStateFields) ed.getFields();
        return super.matches(ed) && (filter.getCustomerId() == null || filter.getCustomerId().equals(entityFields.getEntityId()));
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
