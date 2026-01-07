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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RelationsQueryFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testFindTenantDevices() {
        UUID ta1 = createAsset("T A1");
        UUID ta2 = createAsset("T A2");
        UUID da1 = createDevice("T D1");
        UUID da2 = createDevice(customerId, "T D2");
        UUID da3 = createDevice("NOT MATCHING D3");

        // A1 --Contains--> A2, A1 --Contains--> D1. A1 --Manages--> D2.
        createRelation(EntityType.ASSET, ta1, EntityType.ASSET, ta2, "Contains");
        createRelation(EntityType.ASSET, ta1, EntityType.DEVICE, da1, "Contains");
        createRelation(EntityType.ASSET, ta1, EntityType.DEVICE, da2, "Manages");
        createRelation(EntityType.ASSET, ta1, EntityType.DEVICE, da3, "Contains");

        PageData<QueryResult> relationsResult = filter(new AssetId(ta1), new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, ta2));
        Assert.assertTrue(checkContains(relationsResult, da1));

        relationsResult = filter(new AssetId(ta1), new RelationEntityTypeFilter("Manages", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, da2));
    }

    @Test
    public void testFindTenantDevicesLastLevelOnly() {
        UUID root = createAsset("T ROOT");

        UUID ta1 = createAsset("T A1 NO MORE RELATIONS");
        UUID ta2 = createAsset("T A2");
        UUID da1 = createDevice("T D1");
        UUID da2 = createDevice(customerId, "T D2");
        UUID da3 = createDevice(customerId, "T D3");
        UUID da4 = createDevice(customerId, "T D4"); // Lvl 4

        // ROOT --Contains--> A1, A2; A2 --Contains--> D1, D2; D2 --Contains--> D3.
        createRelation(EntityType.ASSET, root, EntityType.ASSET, ta1, "Contains");
        createRelation(EntityType.ASSET, root, EntityType.ASSET, ta2, "Contains");
        createRelation(EntityType.ASSET, ta2, EntityType.DEVICE, da1, "Contains");
        createRelation(EntityType.ASSET, ta2, EntityType.DEVICE, da2, "Contains");
        createRelation(EntityType.ASSET, da2, EntityType.DEVICE, da3, "Contains");
        createRelation(EntityType.ASSET, da3, EntityType.DEVICE, da4, "Contains");

        PageData<QueryResult> relationsResult = filter(null, new AssetId(root), 1, true,
                new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, ta1));
        Assert.assertTrue(checkContains(relationsResult, ta2));

        relationsResult = filter(null, new AssetId(root), 2, true,
                new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(3, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, ta1));
        Assert.assertTrue(checkContains(relationsResult, da1));
        Assert.assertTrue(checkContains(relationsResult, da2));

        relationsResult = filter(null, new AssetId(root), 3, true,
                new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(3, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, ta1));
        Assert.assertTrue(checkContains(relationsResult, da1));
        Assert.assertTrue(checkContains(relationsResult, da3));

        relationsResult = filter(null, new AssetId(root), 4, true,
                new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(3, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, ta1));
        Assert.assertTrue(checkContains(relationsResult, da1));
        Assert.assertTrue(checkContains(relationsResult, da4));

    }

    @Test
    public void testFindCustomerDevices() {
        UUID ta1 = createAsset("T A1");
        UUID ta2 = createAsset("T A2");
        UUID da1 = createDevice(customerId, "T D1");
        UUID da2 = createDevice("T D2");

        // A1 --Contains--> A2, A1 --Contains--> D1. A1 --Manages--> D2.
        createRelation(EntityType.ASSET, ta1, EntityType.ASSET, ta2, "Contains");
        createRelation(EntityType.ASSET, ta1, EntityType.DEVICE, da1, "Contains");
        createRelation(EntityType.ASSET, ta1, EntityType.DEVICE, da2, "Manages");

        PageData<QueryResult> relationsResult = filter(customerId, new AssetId(ta1), new RelationEntityTypeFilter("Contains", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, da1));

        relationsResult = filter(customerId, new AssetId(ta1), new RelationEntityTypeFilter("Manages", Arrays.asList(EntityType.DEVICE, EntityType.ASSET)));
        Assert.assertEquals(0, relationsResult.getData().size());
    }

    private PageData<QueryResult> filter(EntityId rootId, RelationEntityTypeFilter... relationEntityTypeFilters) {
        return filter(null, rootId, relationEntityTypeFilters);
    }

    private PageData<QueryResult> filter(CustomerId customerId, EntityId rootId, RelationEntityTypeFilter... relationEntityTypeFilters) {
        return filter(customerId, rootId, 3, false, relationEntityTypeFilters);
    }

    private PageData<QueryResult> filter(CustomerId customerId, EntityId rootId, int maxLevel, boolean lastLevelOnly, RelationEntityTypeFilter... relationEntityTypeFilters) {
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(rootId));
        filter.setFilters(Arrays.asList(relationEntityTypeFilters));
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setFetchLastLevelOnly(lastLevelOnly);
        filter.setMaxLevel(maxLevel);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "T");
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
        return repository.findEntityDataByQuery(tenantId, customerId, query, false);
    }

}
