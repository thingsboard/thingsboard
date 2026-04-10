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
import org.thingsboard.client.model.PageDataAsset;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class AssetApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAssetLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Asset> createdAssets = new ArrayList<>();

        // create 20 assets
        for (int i = 0; i < 20; i++) {
            Asset asset = new Asset();
            String assetName = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            asset.setName(assetName);
            asset.setLabel("Test Asset " + i);
            asset.setType(((i % 2 == 0) ? "default" : "building"));

            Asset createdAsset = client.saveAsset(asset, null, null, null);
            assertNotNull(createdAsset);
            assertNotNull(createdAsset.getId());
            assertEquals(assetName, createdAsset.getName());

            createdAssets.add(createdAsset);
        }

        // find all, check count
        PageDataAsset allAssets = client.getTenantAssets(100, 0, null, null, null, null);

        assertNotNull(allAssets);
        assertNotNull(allAssets.getData());
        int initialSize = allAssets.getData().size();
        assertEquals("Expected at least 20 assets, but got " + allAssets.getData().size(), 20, initialSize);

        //find all with search text, check count
        PageDataAsset allAssetsBySearchText = client.getTenantAssets(100, 0, null, TEST_PREFIX_2, null, null);
        assertEquals("Expected exactly 10 test assets", 10, allAssetsBySearchText.getData().size());

        // find by id
        Asset searchAsset = createdAssets.get(10);
        Asset asset = client.getAssetById(searchAsset.getId().getId().toString());
        assertEquals(searchAsset.getName(), asset.getName());

        // delete asset
        UUID assetToDeleteId = createdAssets.get(0).getId().getId();
        client.deleteAsset(assetToDeleteId.toString());

        // Verify the asset is deleted
        PageDataAsset assetsAfterDelete = client.getTenantAssets(100, 0, null, null, null, null);
        assertEquals(initialSize - 1, assetsAfterDelete.getData().size());

        assertReturns404(() ->
                client.getAssetById(assetToDeleteId.toString())
        );
    }

}
