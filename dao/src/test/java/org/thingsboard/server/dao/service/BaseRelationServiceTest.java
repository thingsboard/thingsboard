/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.EntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class BaseRelationServiceTest extends AbstractServiceTest {

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    @Test
    public void testSaveRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(UUIDs.timeBased());
        AssetId childId = new AssetId(UUIDs.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(saveRelation(relation));

        Assert.assertTrue(relationService.checkRelation(parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        Assert.assertFalse(relationService.checkRelation(parentId, childId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON).get());

        Assert.assertFalse(relationService.checkRelation(childId, parentId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        Assert.assertFalse(relationService.checkRelation(childId, parentId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON).get());
    }

    @Test
    public void testDeleteRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(UUIDs.timeBased());
        AssetId childId = new AssetId(UUIDs.timeBased());
        AssetId subChildId = new AssetId(UUIDs.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteRelation(relationA).get());

        Assert.assertFalse(relationService.checkRelation(parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        Assert.assertTrue(relationService.checkRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        Assert.assertTrue(relationService.deleteRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    @Test
    public void testDeleteEntityRelations() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(UUIDs.timeBased());
        AssetId childId = new AssetId(UUIDs.timeBased());
        AssetId subChildId = new AssetId(UUIDs.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteEntityRelations(childId).get());

        Assert.assertFalse(relationService.checkRelation(parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());

        Assert.assertFalse(relationService.checkRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    @Test
    public void testFindFrom() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(UUIDs.timeBased());
        AssetId parentB = new AssetId(UUIDs.timeBased());
        AssetId childA = new AssetId(UUIDs.timeBased());
        AssetId childB = new AssetId(UUIDs.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByFrom(parentA, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.CONTAINS_TYPE, relation.getType());
            Assert.assertEquals(parentA, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(parentA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(2, relations.size());

        relations = relationService.findByFromAndType(parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFrom(parentB, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.MANAGES_TYPE, relation.getType());
            Assert.assertEquals(parentB, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFromAndType(parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(0, relations.size());
    }

    private Boolean saveRelation(EntityRelation relationA1) throws ExecutionException, InterruptedException {
        return relationService.saveRelation(relationA1).get();
    }

    @Test
    public void testFindTo() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(UUIDs.timeBased());
        AssetId parentB = new AssetId(UUIDs.timeBased());
        AssetId childA = new AssetId(UUIDs.timeBased());
        AssetId childB = new AssetId(UUIDs.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        // Data propagation to views is async
        Thread.sleep(3000);

        List<EntityRelation> relations = relationService.findByTo(childA, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childA, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }

        relations = relationService.findByToAndType(childA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(childB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByToAndType(parentB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByTo(childB, RelationTypeGroup.COMMON).get();
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childB, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }
    }

    @Test
    public void testCyclicRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> C -> A
        AssetId assetA = new AssetId(UUIDs.timeBased());
        AssetId assetB = new AssetId(UUIDs.timeBased());
        AssetId assetC = new AssetId(UUIDs.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetA, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1));
        query.setFilters(Collections.singletonList(new EntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
    }

    @Test
    public void testRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> [C,D]
        AssetId assetA = new AssetId(UUIDs.timeBased());
        AssetId assetB = new AssetId(UUIDs.timeBased());
        AssetId assetC = new AssetId(UUIDs.timeBased());
        DeviceId deviceD = new DeviceId(UUIDs.timeBased());

        EntityRelation relationAB = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBD = new EntityRelation(assetB, deviceD, EntityRelation.CONTAINS_TYPE);


        saveRelation(relationAB);
        saveRelation(relationBC);
        saveRelation(relationBD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1));
        query.setFilters(Collections.singletonList(new EntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));
    }


    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyFrom() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setTo(new AssetId(UUIDs.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(saveRelation(relation));
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyTo() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(UUIDs.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(saveRelation(relation));
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyType() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(UUIDs.timeBased()));
        relation.setTo(new AssetId(UUIDs.timeBased()));
        Assert.assertTrue(saveRelation(relation));
    }
}
