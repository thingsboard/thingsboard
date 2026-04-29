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
package org.thingsboard.server.client;

import org.junit.Test;
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
        building = client.saveAsset(building, null, null, null);

        Asset floor = new Asset();
        floor.setName(TEST_PREFIX + "Floor_" + timestamp);
        floor.setType("floor");
        floor = client.saveAsset(floor, null, null, null);

        Device device1 = new Device();
        device1.setName(TEST_PREFIX + "Sensor_" + timestamp + "_1");
        device1.setType("sensor");
        device1 = client.saveDevice(device1, null, null, null, null);

        Device device2 = new Device();
        device2.setName(TEST_PREFIX + "Sensor_" + timestamp + "_2");
        device2.setType("sensor");
        device2 = client.saveDevice(device2, null, null, null, null);

        Device device3 = new Device();
        device3.setName(TEST_PREFIX + "Sensor_" + timestamp + "_3");
        device3.setType("sensor");
        device3 = client.saveDevice(device3, null, null, null, null);

        // create relations: building -> Contains -> floor, floor -> Contains -> device1/device2/device3
        EntityRelation buildingToFloor = new EntityRelation();
        buildingToFloor.setFrom(building.getId());
        buildingToFloor.setTo(floor.getId());
        buildingToFloor.setType("Contains");
        buildingToFloor.setTypeGroup(RelationTypeGroup.COMMON);
        EntityRelation savedRelation = client.saveRelation(buildingToFloor);
        assertNotNull(savedRelation);
        assertEquals("Contains", savedRelation.getType());

        client.saveRelation(new EntityRelation()
                .from(floor.getId())
                .to(device1.getId())
                .type("Contains")
                .typeGroup(RelationTypeGroup.COMMON));
        client.saveRelation(new EntityRelation()
                .from(floor.getId())
                .to(device2.getId())
                .type("Contains").typeGroup(RelationTypeGroup.COMMON));
        client.saveRelation(new EntityRelation()
                .from(floor.getId())
                .to(device3.getId())
                .type("Manages")
                .typeGroup(RelationTypeGroup.COMMON));

        // get specific relation
        EntityRelation fetched = client.getRelation(
                building.getId().getId().toString(), "ASSET",
                "Contains",
                floor.getId().getId().toString(), "ASSET",
                RelationTypeGroup.COMMON.getValue());
        assertNotNull(fetched);
        assertEquals("Contains", fetched.getType());

        // find all relations from floor
        List<EntityRelation> fromFloor = client.findEntityRelationsByFrom("ASSET",
                floor.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
        assertEquals(3, fromFloor.size());

        // find relations from floor with type filter "Contains"
        List<EntityRelation> containsFromFloor = client.findEntityRelationsByFromAndRelationType("ASSET",
                floor.getId().getId().toString(), "Contains", RelationTypeGroup.COMMON.getValue());
        assertEquals(2, containsFromFloor.size());

        // find relations to device1
        List<EntityRelation> toDevice1 = client.findEntityRelationsByTo("DEVICE",
                device1.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
        assertEquals(1, toDevice1.size());
        assertEquals("Contains", toDevice1.get(0).getType());

        // find relations to device3 with type filter "Manages"
        List<EntityRelation> managesToDevice3 = client.findEntityRelationsByToAndRelationType("DEVICE",
                device3.getId().getId().toString(), "Manages", RelationTypeGroup.COMMON.getValue());
        assertEquals(1, managesToDevice3.size());

        // find info by from (includes entity names)
        List<EntityRelationInfo> infoFromFloor = client.findEntityRelationInfosByFrom("ASSET",
                floor.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
        assertEquals(3, infoFromFloor.size());
        Device finalDevice = device1;
        assertTrue(infoFromFloor.stream().anyMatch(info ->
                finalDevice.getName().equals(info.getToName())));

        // find info by to
        List<EntityRelationInfo> infoToDevice2 = client.findEntityRelationInfosByTo("DEVICE",
                device2.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
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

        List<EntityRelation> queryResult = client.findEntityRelationsByQuery(query);
        assertTrue(queryResult.size() >= 3);

        // find info by query
        List<EntityRelationInfo> infoQueryResult = client.findEntityRelationInfosByQuery(query);
        assertTrue(infoQueryResult.size() >= 3);

        // delete single relation
        client.deleteRelation(
                floor.getId().getId().toString(), "ASSET",
                "Manages",
                device3.getId().getId().toString(), "DEVICE",
                RelationTypeGroup.COMMON.getValue());

        // verify deletion
        List<EntityRelation> afterDelete = client.findEntityRelationsByFrom("ASSET",
                floor.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
        assertEquals(2, afterDelete.size());

        // delete all relations for building
        client.deleteRelations(building.getId().getId().toString(), "ASSET");

        List<EntityRelation> afterDeleteAll = client.findEntityRelationsByFrom("ASSET",
                building.getId().getId().toString(), RelationTypeGroup.COMMON.getValue());
        assertEquals(0, afterDeleteAll.size());
    }

}
