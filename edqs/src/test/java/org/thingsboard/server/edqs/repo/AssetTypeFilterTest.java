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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetTypeFilterTest extends AbstractEDQTest {

    private final AssetProfileId assetProfileId = new AssetProfileId(UUID.randomUUID());
    private final AssetProfileId assetProfileId2 = new AssetProfileId(UUID.randomUUID());
    private Asset asset;
    private Asset asset2;
    private Asset asset3;

    @Before
    public void setUp() {
        AssetProfile assetProfile = new AssetProfile(assetProfileId);
        assetProfile.setName("Office");
        assetProfile.setDefault(false);
        addOrUpdate(EntityType.ASSET_PROFILE, assetProfile);

        AssetProfile assetProfile2 = new AssetProfile(assetProfileId2);
        assetProfile2.setName("Street");
        assetProfile2.setDefault(false);
        addOrUpdate(EntityType.ASSET_PROFILE, assetProfile2);

        asset = buildAsset(assetProfileId, "Office 1");
        asset2 = buildAsset(assetProfileId, "Office 2");
        asset3 = buildAsset(assetProfileId2, "Abbey Road");

        addOrUpdate(EntityType.ASSET, asset);
        addOrUpdate(EntityType.ASSET, asset2);
        addOrUpdate(EntityType.ASSET, asset3);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantAsset() {
        // find asset with type "Office"
        var result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(Collections.singletonList("Office"), null, null), false);

        Assert.assertEquals(2, result.getTotalElements());
        var first = result.getData().stream().filter(queryResult -> queryResult.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue().equals("Office 1")).findAny();
        assertThat(first).isPresent();
        assertThat(first.get().getEntityId()).isEqualTo(asset.getId());
        assertThat(first.get().getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue()).isEqualTo(String.valueOf(asset.getCreatedTime()));

        // find asset with type "Office" and "Street"
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office", "Street"), null, null), false);

        Assert.assertEquals(3, result.getTotalElements());
        var third = result.getData().stream().filter(queryResult -> queryResult.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue().equals("Abbey Road")).findAny();
        assertThat(third).isPresent();
        assertThat(third.get().getEntityId()).isEqualTo(asset3.getId());
        assertThat(third.get().getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue()).isEqualTo(String.valueOf(asset.getCreatedTime()));

        // find asset with type "Supermarket"
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Supermarket"), null, null), false);
        Assert.assertEquals(0, result.getTotalElements());

        // find asset with name "%Office%"
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office"), "%Office%", null), false);
        Assert.assertEquals(2, result.getTotalElements());

        // find asset with name "Office 1"
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office"), "Office 1", null), false);
        Assert.assertEquals(1, result.getTotalElements());

        // find asset with name "%Super%"
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office"), "%Super%", null), false);
        Assert.assertEquals(0, result.getTotalElements());

        // find asset with key filter: name contains "Office"
        KeyFilter containsNameFilter = getAssetNameKeyFilter(StringFilterPredicate.StringOperation.CONTAINS, "office", true);
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office"), null, Arrays.asList(containsNameFilter)), false);
        Assert.assertEquals(2, result.getTotalElements());

        // find asset with key filter: name starts with "office" and matches case
        KeyFilter startsWithNameFilter = getAssetNameKeyFilter(StringFilterPredicate.StringOperation.STARTS_WITH, "office", false);
        result = repository.findEntityDataByQuery(tenantId, null, getAssetTypeQuery(List.of("Office"), null, Arrays.asList(startsWithNameFilter)), false);
        Assert.assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindCustomerAsset() {
        addOrUpdate(EntityType.ASSET, asset);
        addOrUpdate(new LatestTsKv(asset.getId(), new BasicTsKvEntry(43, new StringDataEntry("state", "TEST")), 0L));

        var result = repository.findEntityDataByQuery(tenantId, customerId, getAssetTypeQuery(List.of("Office"), null, null), false);
        Assert.assertEquals(0, result.getTotalElements());

        asset.setCustomerId(customerId);
        addOrUpdate(EntityType.ASSET, asset);

        result = repository.findEntityDataByQuery(tenantId, customerId, getAssetTypeQuery(List.of("Office"), null, null), false);

        Assert.assertEquals(1, result.getTotalElements());
        var first = result.getData().get(0);
        Assert.assertEquals(asset.getId(), first.getEntityId());
        Assert.assertEquals("Office 1", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
        Assert.assertEquals("42", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue());

        result = repository.findEntityDataByQuery(tenantId, customerId, getAssetTypeQuery(List.of("Supermarket"), null, null), false);
        Assert.assertEquals(0, result.getTotalElements());
    }

    private Asset buildAsset(AssetProfileId assetProfileId, String assetName) {
        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        asset.setTenantId(tenantId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(assetName);
        asset.setCreatedTime(42L);
        return asset;
    }

    private static EntityDataQuery getAssetTypeQuery(List<String> assetTypes, String assetNameRegex, List<KeyFilter> keyFilters) {
        AssetTypeFilter filter = new AssetTypeFilter();
        filter.setAssetTypes(assetTypes);
        filter.setAssetNameFilter(assetNameRegex);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        var latestValues = Arrays.asList(new EntityKey(EntityKeyType.TIME_SERIES, "state"));

        return new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
    }

    private static KeyFilter getAssetNameKeyFilter(StringFilterPredicate.StringOperation operation, String predicateValue, boolean ignoreCase) {
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
