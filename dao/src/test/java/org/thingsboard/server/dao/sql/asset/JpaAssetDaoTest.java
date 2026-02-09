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
package org.thingsboard.server.dao.sql.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
public class JpaAssetDaoTest extends AbstractJpaDaoTest {

    UUID tenantId1;
    UUID tenantId2;
    UUID customerId1;
    UUID customerId2;
    List<Asset> assets = new ArrayList<>();
    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetProfileDao assetProfileDao;

    private Map<String, AssetProfileId> savedAssetProfiles = new HashMap<>();

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID assetId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            assets.add(saveAsset(assetId, tenantId, customerId, "ASSET_" + i, "label_" + i));
        }
        assertEquals(assets.size(), assetDao.find(TenantId.fromUUID(tenantId1)).size());
    }

    @After
    public void tearDown() {
        for (Asset asset : assets) {
            assetDao.removeById(asset.getTenantId(), asset.getUuidId());
        }
        assets.clear();
        for (AssetProfileId assetProfileId : savedAssetProfiles.values()) {
            assetProfileDao.removeById(TenantId.SYS_TENANT_ID, assetProfileId.getId());
        }
        savedAssetProfiles.clear();
    }

    @Test
    public void testSaveDeviceName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() -> assets.add(
                saveAsset(UUID.randomUUID(), tenantId2, customerId2, "F0929906\000\000\000\000\000\000\000\000\000")));
    }

    @Test
    public void testFindAssetsByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "ASSET_");
        PageData<Asset> assets1 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(20, assets1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets2 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(10, assets2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets3 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(0, assets3.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "ASSET_");
        PageData<Asset> assets1 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, assets1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets2 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, assets2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets3 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, assets3.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerIdByLabel() {
        PageLink pageLink = new PageLink(20, 0, "label_");
        PageData<Asset> assets1 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, assets1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets2 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, assets2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets3 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, assets3.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        List<UUID> searchIds = getAssetsUuids(tenantId1);

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndIdsAsync(tenantId1, searchIds);
        List<Asset> assets = assetsFuture.get(30, TimeUnit.SECONDS);
        assertNotNull(assets);
        assertEquals(searchIds.size(), assets.size());
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        List<UUID> searchIds = getAssetsUuids(tenantId1);

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId1, customerId1, searchIds);
        List<Asset> assets = assetsFuture.get(30, TimeUnit.SECONDS);
        assertNotNull(assets);
        assertEquals(searchIds.size(), assets.size());
    }

    private List<UUID> getAssetsUuids(UUID tenantId) {
        List<UUID> result = new ArrayList<>();
        for (Asset asset : assets) {
            if (asset.getTenantId().getId().equals(tenantId)) {
                result.add(asset.getUuidId());
            }
        }
        return result;
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        UUID assetId = Uuids.timeBased();
        String name = "TEST_ASSET";
        assets.add(saveAsset(assetId, tenantId2, customerId2, name));

        Optional<Asset> assetOpt1 = assetDao.findAssetsByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", assetOpt1.isPresent());
        assertEquals(assetId, assetOpt1.get().getId().getId());

        Optional<Asset> assetOpt2 = assetDao.findAssetsByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", assetOpt2.isPresent());
    }

    @Test
    public void testFindAssetsByTenantIdAndType() {
        String type = "TYPE_2";
        String testLabel = "test_label";
        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET", type, testLabel));

        List<Asset> foundedAssetsByType = assetDao
                .findAssetsByTenantIdAndType(tenantId2, type, new PageLink(3)).getData();
        compareFoundedAssetByType(foundedAssetsByType, type);

        List<Asset> foundedAssetsByTypeAndLabel = assetDao
                .findAssetsByTenantIdAndType(tenantId2, type, new PageLink(3, 0, testLabel)).getData();
        assertEquals(1, foundedAssetsByTypeAndLabel.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerIdAndType() {
        String type = "TYPE_2";
        String testLabel = "test_label";
        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET", type, testLabel));

        List<Asset> foundedAssetsByType = assetDao
                .findAssetsByTenantIdAndCustomerIdAndType(tenantId2, customerId2, type, new PageLink(3)).getData();
        compareFoundedAssetByType(foundedAssetsByType, type);

        List<Asset> foundedAssetsByTypeAndLabel = assetDao
                .findAssetsByTenantIdAndCustomerIdAndType(tenantId2, customerId2, type, new PageLink(3, 0, testLabel)).getData();
        assertEquals(1, foundedAssetsByTypeAndLabel.size());
    }

    private void compareFoundedAssetByType(List<Asset> foundedAssetsByType, String type) {
        assertNotNull(foundedAssetsByType);
        assertEquals(1, foundedAssetsByType.size());
        assertEquals(type, foundedAssetsByType.get(0).getType());
    }

    @Test
    public void testFindTenantAssetTypesAsync() throws ExecutionException, InterruptedException, TimeoutException {
        // Assets with type "TYPE_1" added in setUp method
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_3", "TYPE_2"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_4", "TYPE_3"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_5", "TYPE_3"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_6", "TYPE_3"));

        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET_7", "TYPE_4"));

        List<EntitySubtype> tenant1Types = assetDao.findTenantAssetTypesAsync(tenantId1).get(30, TimeUnit.SECONDS);
        assertNotNull(tenant1Types);
        List<EntitySubtype> tenant2Types = assetDao.findTenantAssetTypesAsync(tenantId2).get(30, TimeUnit.SECONDS);
        assertNotNull(tenant2Types);

        List<String> types = List.of("default", "TYPE_1", "TYPE_2", "TYPE_3", "TYPE_4");
        assertEquals(getDifferentTypesCount(types, tenant1Types), tenant1Types.size());
        assertEquals(getDifferentTypesCount(types, tenant2Types), tenant2Types.size());
    }

    private long getDifferentTypesCount(List<String> types, List<EntitySubtype> foundedAssetsTypes) {
        return foundedAssetsTypes.stream().filter(type -> types.contains(type.getType())).count();
    }

    private Asset saveAsset(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveAsset(id, tenantId, customerId, name, null, null);
    }

    private Asset saveAsset(UUID id, UUID tenantId, UUID customerId, String name, String label) {
        return saveAsset(id, tenantId, customerId, name, null, label);
    }

    private Asset saveAsset(UUID id, UUID tenantId, UUID customerId, String name, String type, String label) {
        if (type == null) {
            type = "default";
        }
        Asset asset = new Asset();
        asset.setId(new AssetId(id));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName(name);
        asset.setType(type);
        asset.setLabel(label);
        asset.setAssetProfileId(assetProfileId(type));
        return assetDao.save(TenantId.fromUUID(tenantId), asset);
    }

    private AssetProfileId assetProfileId(String type) {
        AssetProfileId assetProfileId = savedAssetProfiles.get(type);
        if (assetProfileId == null) {
            AssetProfile assetProfile = new AssetProfile();
            assetProfile.setName(type);
            assetProfile.setTenantId(TenantId.SYS_TENANT_ID);
            assetProfile.setDescription("Test");
            AssetProfile savedAssetProfile = assetProfileDao.save(TenantId.SYS_TENANT_ID, assetProfile);
            assetProfileId = savedAssetProfile.getId();
            savedAssetProfiles.put(type, assetProfileId);
        }
        return assetProfileId;
    }

}
