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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitiesByGroupIdFilterTest extends AbstractEDQTest {

    private UUID deviceId;
    private UUID deviceId2;
    private UUID deviceId3;

    private UUID groupAId;
    private UUID groupBId;

    @Before
    public void setUp() {
        deviceId = createDevice(customerId, "Lora-1");
        deviceId2 = createDevice(customerId, "Lora-2");
        deviceId3 = createDevice(customerId, "Lora-3");

        // add device and device 2 to Group A
        groupAId = createGroup(customerId.getId(), EntityType.DEVICE, "Group A");
        createRelation(EntityType.ENTITY_GROUP, groupAId, EntityType.DEVICE, deviceId, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
        createRelation(EntityType.ENTITY_GROUP, groupAId, EntityType.DEVICE, deviceId2, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");

        // add device and device 3 to Group B
        groupBId = createGroup(customerId.getId(), EntityType.DEVICE, "Group B");
        createRelation(EntityType.ENTITY_GROUP, groupBId, EntityType.DEVICE, deviceId3, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantEntitiesOfGroupA() {
        // get devices of group A
        var result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), null), false);

        Assert.assertEquals(2, result.getTotalElements());
        Assert.assertTrue(checkContains(result, deviceId));
        Assert.assertTrue(checkContains(result, deviceId2));

        //get devices of non-existing group
        result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(UUID.randomUUID()), null), false);
        Assert.assertEquals(0, result.getTotalElements());

        //add name filter
        KeyFilter nameFilter = getNameKeyFilter(StringFilterPredicate.StringOperation.CONTAINS,  "humidity");
        result = repository.findEntityDataByQuery(tenantId, null, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), List.of(nameFilter)),  false);
        Assert.assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindCustomerEntitiesOfGroupA() {
        var result = repository.findEntityDataByQuery(tenantId, new CustomerId(UUID.randomUUID()), RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), null), false);
        Assert.assertEquals(0, result.getTotalElements());

        result = repository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), null), false);
        Assert.assertEquals(2, result.getTotalElements());
        List<UUID> entityIds = result.getData().stream().map(queryResult -> queryResult.getEntityId().getId()).toList();
        assertThat(entityIds).containsOnly(deviceId, deviceId2);
    }

    @Test
    public void testFindCustomerEntitiesWithGroupPermission() {
        MergedUserPermissions groupAPermission = new MergedUserPermissions(
                Collections.emptyMap(), Map.of(new EntityGroupId(groupAId), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        var result = repository.findEntityDataByQuery(tenantId, customerId, groupAPermission, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), null),  false);
        Assert.assertEquals(2, result.getTotalElements());
        List<UUID> entityIds = result.getData().stream().map(queryResult -> queryResult.getEntityId().getId()).toList();
        assertThat(entityIds).containsOnly(deviceId, deviceId2);

        MergedUserPermissions groupBPermission = new MergedUserPermissions(
                Collections.emptyMap(), Map.of(new EntityGroupId(groupBId), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        result = repository.findEntityDataByQuery(tenantId, customerId, groupBPermission, getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupAId), null),  false);
        Assert.assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindCustomerEntitiesWithGenericAndGroupPermission() {
        CustomerId subCustomer = new CustomerId(UUID.randomUUID());
        createCustomer(subCustomer.getId(), customerId.getId(), "Subcustomer A");

        MergedUserPermissions groupPermission = new MergedUserPermissions(
                Map.of(Resource.ALL, Set.of(Operation.ALL)), Map.of(new EntityGroupId(groupBId), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        var result = repository.findEntityDataByQuery(tenantId, subCustomer, groupPermission,
                getEntitiesByGroupDataQuery(EntityType.DEVICE, new EntityGroupId(groupBId), null), false);

        Assert.assertEquals(1, result.getTotalElements());
        Assert.assertTrue(checkContains(result, deviceId3));
    }

    private static EntityDataQuery getEntitiesByGroupDataQuery(EntityType entityType, EntityGroupId groupId, List<KeyFilter> keyFilters) {
        EntityGroupFilter filter = new EntityGroupFilter();
        filter.setGroupType(entityType);
        filter.setEntityGroup(groupId.getId().toString());
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        return new EntityDataQuery(filter, pageLink, entityFields, null, keyFilters);
    }

    private static KeyFilter getNameKeyFilter(StringFilterPredicate.StringOperation operation, String value) {
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(false);
        predicate.setOperation(operation);
        predicate.setValue(new FilterPredicateValue<>(value));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);
        return nameFilter;
    }

}
