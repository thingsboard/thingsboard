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
package org.thingsboard.server.edqs.repo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import java.util.Arrays;
import java.util.UUID;

public class ApiUsageStateFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
        Tenant entity = new Tenant();
        entity.setId(tenantId);
        entity.setTitle("test tenant");
        addOrUpdate(EntityType.TENANT, entity);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindCustomerApiUsageState() {
        UUID customerId = UUID.randomUUID();
        createCustomer(customerId, null, "Customer A");

        ApiUsageState apiUsageState = buildApiUsageState(customerId);
        addOrUpdate(EntityType.API_USAGE_STATE, apiUsageState);

        var result = repository.findEntityDataByQuery(tenantId, null, getEntityDataQuery(new CustomerId(customerId)), false);

        Assert.assertEquals(1, result.getTotalElements());
        var customer = result.getData().get(0);
        Assert.assertEquals("Customer A", customer.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
    }

    private ApiUsageState buildApiUsageState(UUID customerId) {
        ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setId(new ApiUsageStateId(UUID.randomUUID()));
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(new CustomerId(customerId));
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        return apiUsageState;
    }

    private static EntityDataQuery getEntityDataQuery(CustomerId customerId) {
        ApiUsageStateFilter filter = new ApiUsageStateFilter();
        filter.setCustomerId(customerId);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.TIME_SERIES, "name"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(false);
        predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
        predicate.setValue(new FilterPredicateValue<>("Customer A"));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);

        return new EntityDataQuery(filter, pageLink, entityFields, null, Arrays.asList(nameFilter));
    }

}
