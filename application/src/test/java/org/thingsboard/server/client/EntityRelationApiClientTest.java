// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteRelationArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteRelationsArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationInfosByFromArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationInfosByQueryArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationInfosByToArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByFromAndRelationTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByFromArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByQueryArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByToAndRelationTypeArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByToArgs;
import org.thingsboard.client.api.ThingsboardApi.GetRelationArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAssetArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveRelationArgs;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.EntityRelation;
import org.thingsboard.client.model.EntityRelationInfo;
import org.thingsboard.client.model.EntityRelationsQuery;
import org.thingsboard.client.model.EntitySearchDirection;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.RelationEntityTypeFilter;
import org.thingsboard.client.model.RelationTypeGroup;
import org.thingsboard.client.model.RelationsSearchParameters;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class EntityRelationApiClientTest extends AbstractApiClientTest {

    @Test
    public void testEntityRelationLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // create assets and devices to relate
        Asset building = new Asset();
        building.setName(TEST_PREFIX + "Building_" + timestamp);
        building.setType("building");
        building = client.saveAsset(SaveAssetArgs.builder()
                .asset(building)
                .build());

        Asset floor = new Asset();
        floor.setName(TEST_PREFIX + "Floor_" + timestamp);
        floor.setType("floor");
        floor = client.saveAsset(SaveAssetArgs.builder()
                .asset(floor)
                .build());

        Device device1 = new Device();
        device1.setName(TEST_PREFIX + "Sensor_" + timestamp + "_1");
        device1.setType("sensor");
        device1 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device1)
                .build());

        Device device2 = new Device();
        device2.setName(TEST_PREFIX + "Sensor_" + timestamp + "_2");
        device2.setType("sensor");
        device2 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device2)
                .build());

        Device device3 = new Device();
        device3.setName(TEST_PREFIX + "Sensor_" + timestamp + "_3");
        device3.setType("sensor");
        device3 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device3)
                .build());

        // create relations: building -> Contains -> floor, floor -> Contains -> device1/device2/device3
        EntityRelation buildingToFloor = new EntityRelation();
        buildingToFloor.setFrom(building.getId());
        buildingToFloor.setTo(floor.getId());
        buildingToFloor.setType("Contains");
        buildingToFloor.setTypeGroup(RelationTypeGroup.COMMON);
        EntityRelation savedRelation = client.saveRelation(SaveRelationArgs.builder()
                .entityRelation(buildingToFloor)
                .build());
        assertNotNull(savedRelation);
        assertEquals("Contains", savedRelation.getType());

        client.saveRelation(SaveRelationArgs.builder()
                .entityRelation(new EntityRelation()
                        .from(floor.getId())
                        .to(device1.getId())
                        .type("Contains")
                        .typeGroup(RelationTypeGroup.COMMON))
                .build());
        client.saveRelation(SaveRelationArgs.builder()
                .entityRelation(new EntityRelation()
                        .from(floor.getId())
                        .to(device2.getId())
                        .type("Contains").typeGroup(RelationTypeGroup.COMMON))
                .build());
        client.saveRelation(SaveRelationArgs.builder()
                .entityRelation(new EntityRelation()
                        .from(floor.getId())
                        .to(device3.getId())
                        .type("Manages")
                        .typeGroup(RelationTypeGroup.COMMON))
                .build());

        // get specific relation
        EntityRelation fetched = client.getRelation(GetRelationArgs.builder()
                .fromId(building.getId().getId().toString())
                .fromType("ASSET")
                .relationType("Contains")
                .toId(floor.getId().getId().toString())
                .toType("ASSET")
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertNotNull(fetched);
        assertEquals("Contains", fetched.getType());

        // find all relations from floor
        List<EntityRelation> fromFloor = client.findEntityRelationsByFrom(FindEntityRelationsByFromArgs.builder()
                .fromType("ASSET")
                .fromId(floor.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(3, fromFloor.size());

        // find relations from floor with type filter "Contains"
        List<EntityRelation> containsFromFloor = client.findEntityRelationsByFromAndRelationType(FindEntityRelationsByFromAndRelationTypeArgs.builder()
                .fromType("ASSET")
                .fromId(floor.getId().getId().toString())
                .relationType("Contains")
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(2, containsFromFloor.size());

        // find relations to device1
        List<EntityRelation> toDevice1 = client.findEntityRelationsByTo(FindEntityRelationsByToArgs.builder()
                .toType("DEVICE")
                .toId(device1.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(1, toDevice1.size());
        assertEquals("Contains", toDevice1.get(0).getType());

        // find relations to device3 with type filter "Manages"
        List<EntityRelation> managesToDevice3 = client.findEntityRelationsByToAndRelationType(FindEntityRelationsByToAndRelationTypeArgs.builder()
                .toType("DEVICE")
                .toId(device3.getId().getId().toString())
                .relationType("Manages")
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(1, managesToDevice3.size());

        // find info by from (includes entity names)
        List<EntityRelationInfo> infoFromFloor = client.findEntityRelationInfosByFrom(FindEntityRelationInfosByFromArgs.builder()
                .fromType("ASSET")
                .fromId(floor.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(3, infoFromFloor.size());
        Device finalDevice = device1;
        assertTrue(infoFromFloor.stream().anyMatch(info ->
                finalDevice.getName().equals(info.getToName())));

        // find info by to
        List<EntityRelationInfo> infoToDevice2 = client.findEntityRelationInfosByTo(FindEntityRelationInfosByToArgs.builder()
                .toType("DEVICE")
                .toId(device2.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(1, infoToDevice2.size());
        assertEquals(floor.getName(), infoToDevice2.get(0).getFromName());

        // find by query - search from building, direction FROM, max 2 levels
        RelationsSearchParameters params = new RelationsSearchParameters();
        params.setRootId(building.getId().getId());
        params.setRootType(EntityType.ASSET);
        params.setDirection(EntitySearchDirection.FROM);
        params.setRelationTypeGroup(RelationTypeGroup.COMMON);
        params.setMaxLevel(2);

        RelationEntityTypeFilter filter = new RelationEntityTypeFilter();
        filter.setRelationType("Contains");
        filter.setEntityTypes(List.of(EntityType.ASSET, EntityType.DEVICE));

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(params);
        query.setFilters(List.of(filter));

        List<EntityRelation> queryResult = client.findEntityRelationsByQuery(FindEntityRelationsByQueryArgs.builder()
                .entityRelationsQuery(query)
                .build());
        assertTrue(queryResult.size() >= 3);

        // find info by query
        List<EntityRelationInfo> infoQueryResult = client.findEntityRelationInfosByQuery(FindEntityRelationInfosByQueryArgs.builder()
                .entityRelationsQuery(query)
                .build());
        assertTrue(infoQueryResult.size() >= 3);

        // delete single relation
        client.deleteRelation(DeleteRelationArgs.builder()
                .fromId(floor.getId().getId().toString())
                .fromType("ASSET")
                .relationType("Manages")
                .toId(device3.getId().getId().toString())
                .toType("DEVICE")
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());

        // verify deletion
        List<EntityRelation> afterDelete = client.findEntityRelationsByFrom(FindEntityRelationsByFromArgs.builder()
                .fromType("ASSET")
                .fromId(floor.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(2, afterDelete.size());

        // delete all relations for building
        client.deleteRelations(DeleteRelationsArgs.builder()
                .entityId(building.getId().getId().toString())
                .entityType("ASSET")
                .build());

        List<EntityRelation> afterDeleteAll = client.findEntityRelationsByFrom(FindEntityRelationsByFromArgs.builder()
                .fromType("ASSET")
                .fromId(building.getId().getId().toString())
                .relationTypeGroup(RelationTypeGroup.COMMON.getValue())
                .build());
        assertEquals(0, afterDeleteAll.size());
    }

}
