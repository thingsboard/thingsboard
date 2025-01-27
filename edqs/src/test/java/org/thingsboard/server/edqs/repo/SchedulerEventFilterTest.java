/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SchedulerEventFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantSchedulerEvents() {
        UUID dashboardId = createDashboard("test dashboard");
        UUID deviceId = createDevice("test device");

        UUID eventId1 = createSchedulerEvent("Update attributes", new DeviceId(deviceId), "Turn off device");
        UUID eventId2 = createSchedulerEvent("Generate report", new DashboardId(dashboardId), "Generate morning report");
        UUID eventId3 = createSchedulerEvent("Generate report", new DashboardId(dashboardId), "Generate evening report");

        // find all scheduler events with type "Generate report"
        var result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery("Generate report", null, null), false);
        Assert.assertEquals(2, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId2));
        Assert.assertTrue(checkContains(result, eventId3));

        // find all scheduler events for device originator
        result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery(null, new DeviceId(deviceId), null), false);
        Assert.assertEquals(1, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId1));

        // find  all scheduler events with name "%morning%"
        KeyFilter containsNameFilter = getSchedulerEventNameKeyFilter(StringFilterPredicate.StringOperation.CONTAINS, "morning", true);
        result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery(null, null, List.of(containsNameFilter)), false);
        Assert.assertEquals(1, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId2));
    }

    @Test
    public void testFindCustomerEdges() {
        UUID dashboardId = createDashboard( "test dashboard");
        UUID deviceId = createDevice("test device");

        UUID eventId1 = createSchedulerEvent(customerId.getId(), "Update attributes", new DeviceId(deviceId), "Turn off device");
        UUID eventId2 = createSchedulerEvent(customerId.getId(), "Generate report", new DashboardId(dashboardId), "Generate morning report");
        UUID eventId3 = createSchedulerEvent(customerId.getId(), "Generate report", new DashboardId(dashboardId), "Generate evening report");

        // find all scheduler events with type "Generate report"
        var result = repository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery("Generate report", null, null), false);
        Assert.assertEquals(2, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId2));
        Assert.assertTrue(checkContains(result, eventId3));

        // find all scheduler events for device originator
        result = repository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery(null, new DeviceId(deviceId), null), false);
        Assert.assertEquals(1, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId1));

        // find  all scheduler events with name "%morning%"
        KeyFilter containsNameFilter = getSchedulerEventNameKeyFilter(StringFilterPredicate.StringOperation.CONTAINS, "morning", true);
        result = repository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, getSchedulerEventQuery(null, null, List.of(containsNameFilter)), false);
        Assert.assertEquals(1, result.getTotalElements());
        Assert.assertTrue(checkContains(result, eventId2));
    }

    private static EntityDataQuery getSchedulerEventQuery(String eventType, EntityId entityId, List<KeyFilter> keyFilters) {
        SchedulerEventFilter filter = new SchedulerEventFilter();
        filter.setEventType(eventType);
        filter.setOriginator(entityId);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        var latestValues = Arrays.asList(new EntityKey(EntityKeyType.TIME_SERIES, "state"));

        return new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
    }

    private static KeyFilter getSchedulerEventNameKeyFilter(StringFilterPredicate.StringOperation operation, String predicateValue, boolean ignoreCase) {
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(ignoreCase);
        predicate.setOperation(operation);
        predicate.setValue(new FilterPredicateValue<>(predicateValue));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);
        return nameFilter;
    }

}
