/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AssetSearchQueryFilterTest extends AbstractEDQTest {
    private final AssetProfileId assetProfileId = new AssetProfileId(UUID.randomUUID());

    @Before
    public void setUp() {
    }

    @Test
    public void testFindTenantAssets() {
        AssetProfile assetProfile = new AssetProfile(assetProfileId);
        assetProfile.setName("Office");
        assetProfile.setDefault(false);
        addOrUpdate(EntityType.ASSET_PROFILE, assetProfile);

        UUID root = createAsset(null, assetProfileId.getId(), "root");
        UUID asset1 = createAsset(null, assetProfileId.getId(), "A1");
        UUID asset2 = createAsset(null, assetProfileId.getId(), "A2");

        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset1, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");

        // find all assets of root asset
        PageData<QueryResult> relationsResult = findData(null, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("Office"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets with max level = 1
        relationsResult = findData(null, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 1, false, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));

        // find all assets with asset type = default
        relationsResult = findData(null, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 1, false, Arrays.asList("default"));
        Assert.assertEquals(0, relationsResult.getData().size());

        // find all assets last level only, level = 2
        relationsResult = findData(null, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 2, true, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets last level only, level = 1
        relationsResult = findData(null, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 1, true, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
    }

    @Test
    public void testFindCustomerAssets() {
        AssetProfile assetProfile = new AssetProfile(assetProfileId);
        assetProfile.setName("Office");
        assetProfile.setDefault(false);
        addOrUpdate(EntityType.ASSET_PROFILE, assetProfile);

        UUID root = createAsset(customerId.getId(), assetProfileId.getId(), "root");
        UUID asset1 = createAsset(customerId.getId(), assetProfileId.getId(), "A1");
        UUID asset2 = createAsset(customerId.getId(), assetProfileId.getId(), "A2");
        UUID asset3 = createAsset(customerId.getId(), defaultAssetProfileId, "A3");

        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset1, "Contains");
        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset3, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");

        // find all assets of root asset with profile "Office"
        PageData<QueryResult> relationsResult = findData(customerId, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("Office"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets of root asset with profile "Office" and "default"
        relationsResult = findData(customerId, new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("Office", "default"));
        Assert.assertEquals(3, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));
        Assert.assertTrue(checkContains(relationsResult, asset3));

        // find all assets with other customer
        relationsResult = findData(new CustomerId(UUID.randomUUID()), new AssetId(root),
                EntitySearchDirection.FROM, "Contains", 1, false, Arrays.asList("Office"));
        Assert.assertEquals(0, relationsResult.getData().size());
    }


    private PageData<QueryResult> findData(CustomerId customerId, EntityId rootId,
                                           EntitySearchDirection direction, String relationType, int maxLevel, boolean lastLevelOnly, List<String> assetTypes) {
        AssetSearchQueryFilter filter = new AssetSearchQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(rootId));
        filter.setDirection(direction);
        filter.setRelationType(relationType);
        filter.setAssetTypes(assetTypes);
        filter.setFetchLastLevelOnly(lastLevelOnly);
        filter.setMaxLevel(maxLevel);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "A");
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
        return repository.findEntityDataByQuery(tenantId, customerId, query, false);
    }

}
