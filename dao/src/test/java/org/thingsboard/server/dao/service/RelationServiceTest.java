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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class RelationServiceTest extends AbstractServiceTest {

    @Autowired
    RelationService relationService;

    @Autowired
    private TbTenantProfileCache tbTenantProfileCache;

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    @Test
    public void testSaveRelation() {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertNotNull(saveRelation(relation));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));
    }

    @Test
    public void testDeleteRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA).get());

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    @Test
    public void testDeleteRelationConcurrently() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);

        List<ListenableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA));
        }
        List<Boolean> results = Futures.allAsList(futures).get();
        Assert.assertTrue(results.contains(true));
    }

    @Test
    public void testDeleteEntityRelations() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        relationService.deleteEntityRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
    }

    @Test
    public void testDeleteEntityCommonRelations() {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);
        EntityRelation relationD = new EntityRelation(childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        relationService.deleteEntityCommonRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
    }

    @Test
    public void testFindFrom() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.CONTAINS_TYPE, relation.getType());
            Assert.assertEquals(parentA, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.MANAGES_TYPE, relation.getType());
            Assert.assertEquals(parentB, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());
    }

    private EntityRelation saveRelation(EntityRelation relation) {
        return relationService.saveRelation(SYSTEM_TENANT_ID, relation);
    }

    @Test
    public void testFindTo() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByTo(SYSTEM_TENANT_ID, childA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childA, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByTo(SYSTEM_TENANT_ID, childB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childB, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }
    }

    @Test
    public void testCyclicRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> C -> A
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetA, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
    }

    @Test
    public void testRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> [C,D]
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        DeviceId deviceD = new DeviceId(Uuids.timeBased());

        EntityRelation relationAB = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBD = new EntityRelation(assetB, deviceD, EntityRelation.CONTAINS_TYPE);


        relationAB = saveRelation(relationAB);
        relationBC = saveRelation(relationBC);
        saveRelation(relationBD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));
    }

    @Test
    public void testRecursiveRelationDepth() throws ExecutionException, InterruptedException {
        int maxLevel = 1000;
        AssetId root = new AssetId(Uuids.timeBased());
        AssetId left = new AssetId(Uuids.timeBased());
        AssetId right = new AssetId(Uuids.timeBased());

        List<EntityRelation> expected = new ArrayList<>();

        EntityRelation relationAB = new EntityRelation(root, left, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(root, right, EntityRelation.CONTAINS_TYPE);
        expected.add(saveRelation(relationAB));
        expected.add(saveRelation(relationBC));

        for (int i = 0; i < maxLevel; i++) {
            var newLeft = new AssetId(Uuids.timeBased());
            var newRight = new AssetId(Uuids.timeBased());
            EntityRelation relationLeft = new EntityRelation(left, newLeft, EntityRelation.CONTAINS_TYPE);
            EntityRelation relationRight = new EntityRelation(right, newRight, EntityRelation.CONTAINS_TYPE);
            expected.add(saveRelation(relationLeft));
            expected.add(saveRelation(relationRight));
            left = newLeft;
            right = newRight;
        }

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(root, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for (EntityRelation r : expected) {
            Assert.assertTrue(relations.contains(r));
        }

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for (EntityRelation r : expected) {
            Assert.assertTrue(relations.contains(r));
        }
    }

    @Test
    public void testSaveRelationWithEmptyFrom() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setTo(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertNotNull(saveRelation(relation));
        });
    }

    @Test
    public void testSaveRelationWithEmptyTo() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertNotNull(saveRelation(relation));
        });
    }

    @Test
    public void testSaveRelationWithEmptyType() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setTo(new AssetId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertNotNull(saveRelation(relation));
        });
    }

    @Test
    public void testFindByQueryFetchLastOnlyTreeLike() throws Exception {
        // A -> B
        // A -> C
        // C -> D
        // C -> E

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);
        relationD = saveRelation(relationD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    public void testFindByQueryFetchLastOnlySingleLinked() throws Exception {
        // A -> B -> C -> D

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    public void testFindByQueryFetchLastOnlyTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);
        relationD = saveRelation(relationD);
        relationE = saveRelation(relationE);
        relationF = saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    public void testFindByQueryTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);
        relationD = saveRelation(relationD);
        relationE = saveRelation(relationE);
        relationF = saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(4, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    public void testFindByQueryTreeLikeWithUnlimLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        relationA = saveRelation(relationA);
        relationB = saveRelation(relationB);
        relationC = saveRelation(relationC);
        relationD = saveRelation(relationD);
        relationE = saveRelation(relationE);
        relationF = saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(6, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));
    }

    @Test
    public void testFindByPathQueryWithoutExceedingLimit() throws Exception {
        /*
        A
        └──[firstLevel, TO]→ B
            └──[secondLevel, TO]→ C
                ├──[thirdLevel, FROM]→ D1
                ├──[thirdLevel, FROM]→ D2
                ├──[thirdLevel, FROM]→ ...
                └──[thirdLevel, FROM]→ D{N - 1}, where N is the limit
        */
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());

        // create first and second level
        saveRelation(new EntityRelation(assetB, assetA, "firstLevel"));
        saveRelation(new EntityRelation(assetC, assetB, "secondLevel"));

        int limit = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMaxRelatedEntitiesToReturnPerCfArgument();

        int totalCreated = limit - 1;

        List<EntityRelation> allThirdLevelRelations = new ArrayList<>();
        for (int i = 0; i < totalCreated; i++) {
            AssetId leaf = new AssetId(Uuids.timeBased());
            allThirdLevelRelations.add(saveRelation(new EntityRelation(assetC, leaf, "thirdLevel")));
        }

        EntityRelationPathQuery query = new EntityRelationPathQuery(assetA, List.of(
                new RelationPathLevel(EntitySearchDirection.TO, "firstLevel"),
                new RelationPathLevel(EntitySearchDirection.TO, "secondLevel"),
                new RelationPathLevel(EntitySearchDirection.FROM, "thirdLevel")
        ));

        // call a method that applies the default limit internally
        List<EntityRelation> result = relationService.findByRelationPathQueryAsync(tenantId, query).get();

        // verify that limit has been applied
        assertThat(result).hasSize(totalCreated);

        // verify all returned are valid third-level relations under C
        assertThat(result)
                .allSatisfy(rel -> {
                    assertThat(rel.getType()).isEqualTo("thirdLevel");
                    assertThat(rel.getFrom()).isEqualTo(assetC);
                });

        // verify the returned subset is part of all created relations
        assertThat(result).isEqualTo(allThirdLevelRelations);
    }

    @Test
    public void testFindByPathQueryWithExceedingLimit() throws Exception {
        /*
        A
        └──[firstLevel, TO]→ B
            └──[secondLevel, TO]→ C
                ├──[thirdLevel, FROM]→ D1
                ├──[thirdLevel, FROM]→ D2
                ├──[thirdLevel, FROM]→ ...
                └──[thirdLevel, FROM]→ D{N + 20}, where N is the limit
        */
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());

        // create first and second level
        saveRelation(new EntityRelation(assetB, assetA, "firstLevel"));
        saveRelation(new EntityRelation(assetC, assetB, "secondLevel"));

        int limit = tbTenantProfileCache.get(tenantId)
                .getDefaultProfileConfiguration()
                .getMaxRelatedEntitiesToReturnPerCfArgument();

        int totalCreated = limit + 20;

        List<EntityRelation> allThirdLevelRelations = new ArrayList<>();
        for (int i = 0; i < totalCreated; i++) {
            AssetId leaf = new AssetId(Uuids.timeBased());
            allThirdLevelRelations.add(saveRelation(new EntityRelation(assetC, leaf, "thirdLevel")));
        }

        EntityRelationPathQuery query = new EntityRelationPathQuery(assetA, List.of(
                new RelationPathLevel(EntitySearchDirection.TO, "firstLevel"),
                new RelationPathLevel(EntitySearchDirection.TO, "secondLevel"),
                new RelationPathLevel(EntitySearchDirection.FROM, "thirdLevel")
        ));

        // call a method that applies the default limit internally
        List<EntityRelation> result = relationService.findByRelationPathQueryAsync(tenantId, query).get();

        // verify that limit has been applied
        assertThat(result).hasSize(limit);

        // verify all returned are valid third-level relations under C
        assertThat(result)
                .allSatisfy(rel -> {
                    assertThat(rel.getType()).isEqualTo("thirdLevel");
                    assertThat(rel.getFrom()).isEqualTo(assetC);
                });

        // verify the returned subset is part of all created relations
        assertThat(result).isSubsetOf(allThirdLevelRelations);
    }

    @Test
    public void testFindByQueryLargeHierarchyFetchAllWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, false);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    @Test
    public void testFindByQueryLargeHierarchyFetchLastOnlyWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, true);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    private void createAssetRelationsRecursively(AssetId rootAsset, int lvl, List<EntityRelation> entityRelations, boolean lastLvlOnly) throws Exception {
        if (lvl == 0) return;

        AssetId firstAsset = new AssetId(Uuids.timeBased());
        AssetId secondAsset = new AssetId(Uuids.timeBased());

        EntityRelation firstRelation = new EntityRelation(rootAsset, firstAsset, EntityRelation.CONTAINS_TYPE);
        EntityRelation secondRelation = new EntityRelation(rootAsset, secondAsset, EntityRelation.CONTAINS_TYPE);

        firstRelation = saveRelation(firstRelation);
        secondRelation = saveRelation(secondRelation);

        if (!lastLvlOnly || lvl == 1) {
            entityRelations.add(firstRelation);
            entityRelations.add(secondRelation);
        }

        createAssetRelationsRecursively(firstAsset, lvl - 1, entityRelations, lastLvlOnly);
        createAssetRelationsRecursively(secondAsset, lvl - 1, entityRelations, lastLvlOnly);
    }
}
