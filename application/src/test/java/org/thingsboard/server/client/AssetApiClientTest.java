// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteAssetArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllAssetInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAssetsByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantAssetByNameArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantAssetsArgs;
import org.thingsboard.client.api.ThingsboardApi.ProcessAssetBulkImportArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAssetArgs;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetInfo;
import org.thingsboard.client.model.BulkImportColumnType;
import org.thingsboard.client.model.BulkImportRequest;
import org.thingsboard.client.model.BulkImportResultAsset;
import org.thingsboard.client.model.ColumnMapping;
import org.thingsboard.client.model.EntitySubtype;
import org.thingsboard.client.model.Mapping;
import org.thingsboard.client.model.PageDataAsset;
import org.thingsboard.client.model.PageDataAssetInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class AssetApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAssetLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Asset> createdAssets = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Asset asset = new Asset();
            String assetName = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            asset.setName(assetName);
            asset.setLabel("Test Asset " + i);
            asset.setType(((i % 2 == 0) ? "default" : "building"));

            Asset createdAsset = client.saveAsset(SaveAssetArgs.builder()
                    .asset(asset)
                    .build());
            assertNotNull(createdAsset);
            assertNotNull(createdAsset.getId());
            assertEquals(assetName, createdAsset.getName());

            createdAssets.add(createdAsset);
        }

        PageDataAsset allAssets = client.getTenantAssets(GetTenantAssetsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allAssets);
        assertNotNull(allAssets.getData());
        int initialSize = allAssets.getData().size();
        assertEquals("Expected at least 20 assets, but got " + allAssets.getData().size(), 20, initialSize);

        PageDataAsset allAssetsBySearchText = client.getTenantAssets(GetTenantAssetsArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 test assets", 10, allAssetsBySearchText.getData().size());

        Asset searchAsset = createdAssets.get(10);
        Asset asset = client.getAssetById(GetAssetByIdArgs.builder()
                .assetId(searchAsset.getId().getId().toString())
                .build());
        assertEquals(searchAsset.getName(), asset.getName());

        UUID assetToDeleteId = createdAssets.get(0).getId().getId();
        client.deleteAsset(DeleteAssetArgs.builder()
                .assetId(assetToDeleteId.toString())
                .build());

        PageDataAsset assetsAfterDelete = client.getTenantAssets(GetTenantAssetsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize - 1, assetsAfterDelete.getData().size());

        assertReturns404(() ->
                client.getAssetById(GetAssetByIdArgs.builder()
                        .assetId(assetToDeleteId.toString())
                        .build())
        );
    }

    @Test
    public void testGetAssetInfoAndSearch() throws Exception {
        long timestamp = System.currentTimeMillis();

        Asset a1 = new Asset();
        a1.setName(TEST_PREFIX + timestamp + "_info_0");
        a1.setType("warehouse");
        Asset saved1 = client.saveAsset(SaveAssetArgs.builder()
                .asset(a1)
                .build());

        Asset a2 = new Asset();
        a2.setName(TEST_PREFIX + timestamp + "_info_1");
        a2.setType("factory");
        Asset saved2 = client.saveAsset(SaveAssetArgs.builder()
                .asset(a2)
                .build());

        AssetInfo info = client.getAssetInfoById(GetAssetInfoByIdArgs.builder()
                .assetId(saved1.getId().getId().toString())
                .build());
        assertNotNull(info);
        assertEquals(saved1.getName(), info.getName());
        assertNotNull(info.getAssetProfileId());

        Asset byName = client.getTenantAssetByName(GetTenantAssetByNameArgs.builder()
                .assetName(saved1.getName())
                .build());
        assertNotNull(byName);
        assertEquals(saved1.getName(), byName.getName());
        assertEquals(saved1.getId().getId(), byName.getId().getId());

        List<String> ids = List.of(
                saved1.getId().getId().toString(),
                saved2.getId().getId().toString()
        );
        List<Asset> batch = client.getAssetsByIds(GetAssetsByIdsArgs.builder()
                .assetIds(ids)
                .build());
        assertNotNull(batch);
        assertEquals(2, batch.size());

        List<EntitySubtype> types = client.getAssetTypes();
        assertNotNull(types);
        List<String> typeNames = types.stream().map(EntitySubtype::getType).collect(Collectors.toList());
        assertTrue(typeNames.contains("warehouse"));
        assertTrue(typeNames.contains("factory"));

        PageDataAssetInfo allInfos = client.getAllAssetInfos(GetAllAssetInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(false)
                .build());
        assertNotNull(allInfos);
        assertNotNull(allInfos.getData());
        assertTrue(allInfos.getData().size() >= 2);
    }

    @Test
    public void testAssetBulkImport() throws Exception {
        long timestamp = System.currentTimeMillis();

        String csv = "name,label\n"
                     + TEST_PREFIX + timestamp + "_bulk_0,Bulk Asset 0\n"
                     + TEST_PREFIX + timestamp + "_bulk_1,Bulk Asset 1\n"
                     + TEST_PREFIX + timestamp + "_bulk_2,Bulk Asset 2\n";

        Mapping mapping = new Mapping();
        mapping.setDelimiter(",");
        mapping.setHeader(true);
        mapping.setUpdate(false);
        mapping.setColumns(List.of(
                new ColumnMapping().type(BulkImportColumnType.NAME),
                new ColumnMapping().type(BulkImportColumnType.LABEL)
        ));

        BulkImportRequest request = new BulkImportRequest();
        request.setFile(csv);
        request.setMapping(mapping);

        BulkImportResultAsset result = client.processAssetBulkImport(ProcessAssetBulkImportArgs.builder()
                .bulkImportRequest(request)
                .build());
        assertNotNull(result);
        List<String> errors = result.getErrorsList();
        assertTrue("Expected no import errors: " + errors, errors == null || errors.isEmpty());
    }

}
