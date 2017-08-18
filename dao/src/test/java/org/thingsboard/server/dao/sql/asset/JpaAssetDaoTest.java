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
package org.thingsboard.server.dao.sql.asset;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
public class JpaAssetDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AssetDao assetDao;

    @Test
    public void testFindAssetsByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID assetId = UUIDs.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            saveAsset(assetId, tenantId, customerId, "ASSET_" + i, "TYPE_1");
        }
        assertEquals(60, assetDao.find().size());

        TextPageLink pageLink1 = new TextPageLink(20, "ASSET_");
        List<Asset> assets1 = assetDao.findAssetsByTenantId(tenantId1, pageLink1);
        assertEquals(20, assets1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "ASSET_", assets1.get(19).getId().getId(), null);
        List<Asset> assets2 = assetDao.findAssetsByTenantId(tenantId1, pageLink2);
        assertEquals(10, assets2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "ASSET_", assets2.get(9).getId().getId(), null);
        List<Asset> assets3 = assetDao.findAssetsByTenantId(tenantId1, pageLink3);
        assertEquals(0, assets3.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID assetId = UUIDs.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            saveAsset(assetId, tenantId, customerId, "ASSET_" + i, "TYPE_1");
        }

        TextPageLink pageLink1 = new TextPageLink(20, "ASSET_");
        List<Asset> assets1 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink1);
        assertEquals(20, assets1.size());

        TextPageLink pageLink2 = new TextPageLink(20, "ASSET_", assets1.get(19).getId().getId(), null);
        List<Asset> assets2 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink2);
        assertEquals(10, assets2.size());

        TextPageLink pageLink3 = new TextPageLink(20, "ASSET_", assets2.get(9).getId().getId(), null);
        List<Asset> assets3 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink3);
        assertEquals(0, assets3.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException {
        UUID tenantId = UUIDs.timeBased();
        UUID customerId = UUIDs.timeBased();
        List<UUID> searchIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID assetId = UUIDs.timeBased();
            saveAsset(assetId, tenantId, customerId, "ASSET_" + i, "TYPE_1");
            if (i % 3 == 0) {
                searchIds.add(assetId);
            }
        }

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndIdsAsync(tenantId, searchIds);
        List<Asset> assets = assetsFuture.get();
        assertNotNull(assets);
        assertEquals(10, assets.size());
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndIdsAsync() throws ExecutionException, InterruptedException {
        UUID tenantId = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();
        List<UUID> searchIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID assetId = UUIDs.timeBased();
            UUID customerId = i%2 == 0 ? customerId1 : customerId2;
            saveAsset(assetId, tenantId, customerId, "ASSET_" + i, "TYPE_1");
            if (i % 3 == 0) {
                searchIds.add(assetId);
            }
        }

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId, customerId1, searchIds);
        List<Asset> assets = assetsFuture.get();
        assertNotNull(assets);
        assertEquals(5, assets.size());
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        UUID assetId1 = UUIDs.timeBased();
        UUID assetId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();
        String name = "TEST_ASSET";
        saveAsset(assetId1, tenantId1, customerId1, name, "TYPE_1");
        saveAsset(assetId2, tenantId2, customerId2, name, "TYPE_1");

        Optional<Asset> assetOpt1 = assetDao.findAssetsByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", assetOpt1.isPresent());
        assertEquals(assetId2, assetOpt1.get().getId().getId());

        Optional<Asset> assetOpt2 = assetDao.findAssetsByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", assetOpt2.isPresent());
    }

    @Test
    public void testFindAssetsByTenantIdAndType() {
        // TODO: implement
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerIdAndType() {
        // TODO: implement
    }

    @Test
    public void testFindTenantAssetTypesAsync() throws ExecutionException, InterruptedException {
        UUID assetId1 = UUIDs.timeBased();
        UUID assetId2 = UUIDs.timeBased();
        UUID tenantId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_1", "TYPE_1");
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_2", "TYPE_1");
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_3", "TYPE_2");
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_4", "TYPE_3");
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_5", "TYPE_3");
        saveAsset(UUIDs.timeBased(), tenantId1, customerId1, "TEST_ASSET_6", "TYPE_3");

        saveAsset(UUIDs.timeBased(), tenantId2, customerId2, "TEST_ASSET_7", "TYPE_4");
        saveAsset(UUIDs.timeBased(), tenantId2, customerId2, "TEST_ASSET_8", "TYPE_1");
        saveAsset(UUIDs.timeBased(), tenantId2, customerId2, "TEST_ASSET_9", "TYPE_1");

        List<EntitySubtype> tenant1Types = assetDao.findTenantAssetTypesAsync(tenantId1).get();
        assertNotNull(tenant1Types);
        List<EntitySubtype> tenant2Types = assetDao.findTenantAssetTypesAsync(tenantId2).get();
        assertNotNull(tenant2Types);

        assertEquals(3, tenant1Types.size());
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_1")));
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_2")));
        assertTrue(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_3")));
        assertFalse(tenant1Types.stream().anyMatch(t -> t.getType().equals("TYPE_4")));

        assertEquals(2, tenant2Types.size());
        assertTrue(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_1")));
        assertTrue(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_4")));
        assertFalse(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_2")));
        assertFalse(tenant2Types.stream().anyMatch(t -> t.getType().equals("TYPE_3")));
    }

    private void saveAsset(UUID id, UUID tenantId, UUID customerId, String name, String type) {
        Asset asset = new Asset();
        asset.setId(new AssetId(id));
        asset.setTenantId(new TenantId(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName(name);
        asset.setType(type);
        assetDao.save(asset);
    }
}
