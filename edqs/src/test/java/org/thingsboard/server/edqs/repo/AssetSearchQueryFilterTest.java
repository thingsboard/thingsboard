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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        PageData<QueryResult> relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("Office"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets with max level = 1
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 1, false, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));

        // find all assets with asset type = default
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 1, false, Arrays.asList("default"));
        Assert.assertEquals(0, relationsResult.getData().size());

        // find all assets last level only, level = 2
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, true, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets last level only, level = 1
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 1, true, Arrays.asList("Office"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
    }

    @Test
    public void testFindTenantAssetsWithGroupPermissionsOnly() {
        UUID eg1 = createGroup(EntityType.ASSET, "Group A");

        UUID root = createAsset("root");
        UUID asset1 = createAsset("A1");
        createRelation(EntityType.ENTITY_GROUP, eg1, EntityType.ASSET, asset1, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
        UUID asset2 = createAsset("A2");

        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset1, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");

        // find all assets group permission only
        MergedUserPermissions readGroupPermissions = new MergedUserPermissions(Collections.emptyMap(), Collections.singletonMap(new EntityGroupId(eg1),
                new MergedGroupPermissionInfo(EntityType.ASSET, new HashSet<>(Arrays.asList(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY)))));
        PageData<QueryResult> relationsResult = findData(readGroupPermissions, null, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("default"));
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
        PageData<QueryResult> relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, customerId, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("Office"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));

        // find all assets of root asset with profile "Office" and "default"
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, customerId, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("Office", "default"));
        Assert.assertEquals(3, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));
        Assert.assertTrue(checkContains(relationsResult, asset3));

        // find all assets with other customer
        relationsResult = findData(RepositoryUtils.ALL_READ_PERMISSIONS, new CustomerId(UUID.randomUUID()), new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 1, false, Arrays.asList("Office"));
        Assert.assertEquals(0, relationsResult.getData().size());
    }

    @Test
    public void testFindCustomerAssetsWithGroupPermission() {
        UUID eg1 = createGroup(EntityType.ASSET, "Group A");

        UUID root = createAsset(customerId.getId(), defaultAssetProfileId, "root");
        UUID asset1 = createAsset(customerId.getId(), defaultAssetProfileId,"A1");
        createRelation(EntityType.ENTITY_GROUP, eg1, EntityType.ASSET, asset1, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");
        UUID asset2 = createAsset(customerId.getId(), defaultAssetProfileId,"A2");

        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset1, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");

        // find all assets group permission only
        MergedUserPermissions readGroupPermissions = new MergedUserPermissions(Collections.emptyMap(), Collections.singletonMap(new EntityGroupId(eg1),
                new MergedGroupPermissionInfo(EntityType.ASSET, new HashSet<>(Arrays.asList(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY)))));
        PageData<QueryResult> relationsResult = findData(readGroupPermissions, customerId, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("default"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
    }

    @Test
    public void testFindCustomerAssetsWithGenericAndGroupPermission() {
        CustomerId subCustomer = new CustomerId(UUID.randomUUID());
        createCustomer(subCustomer.getId(), customerId.getId(), "Sub Customer A");
        UUID asset1 = createAsset(subCustomer.getId(), defaultAssetProfileId,"A1");
        UUID eg1 = createGroup(subCustomer.getId(), EntityType.ASSET, "Group A");
        createRelation(EntityType.ENTITY_GROUP, eg1, EntityType.ASSET, asset1, RelationTypeGroup.FROM_ENTITY_GROUP, "Contains");

        UUID root = createAsset(customerId.getId(), defaultAssetProfileId, "root");
        UUID asset2 = createAsset(customerId.getId(), defaultAssetProfileId,"A2");

        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset1, "Contains");
        createRelation(EntityType.ASSET, root, EntityType.ASSET, asset2, "Contains");

        // find all assets group permission only
        MergedUserPermissions readGroupPermissions = new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.singletonMap(new EntityGroupId(eg1),
                new MergedGroupPermissionInfo(EntityType.ASSET, new HashSet<>(Arrays.asList(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY)))));
        PageData<QueryResult> relationsResult = findData(readGroupPermissions, customerId, new AssetId(root),
                EntitySearchDirection.FROM,  "Contains", 2, false, Arrays.asList("default"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, asset1));
        Assert.assertTrue(checkContains(relationsResult, asset2));
    }


    private PageData<QueryResult> findData(MergedUserPermissions permissions, CustomerId customerId, EntityId rootId,
                                           EntitySearchDirection direction, String relationType, int maxLevel, boolean lastLevelOnly, List<String> assetTypes) {
        AssetSearchQueryFilter filter = new AssetSearchQueryFilter();
        filter.setRootEntity(rootId);
        filter.setDirection(direction);
        filter.setRelationType(relationType);
        filter.setAssetTypes(assetTypes);
        filter.setFetchLastLevelOnly(lastLevelOnly);
        filter.setMaxLevel(maxLevel);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "A");
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
        return repository.findEntityDataByQuery(tenantId, customerId, permissions, query, false);
    }

}
