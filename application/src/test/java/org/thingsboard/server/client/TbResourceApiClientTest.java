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
import org.thingsboard.client.model.PageDataTbResourceInfo;
import org.thingsboard.client.model.ResourceType;
import org.thingsboard.client.model.TbResource;
import org.thingsboard.client.model.TbResourceInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TbResourceApiClientTest extends AbstractApiClientTest {

    @Test
    public void testResourceLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<TbResourceInfo> createdResources = new ArrayList<>();

        // create 5 JS_MODULE resources
        for (int i = 0; i < 5; i++) {
            TbResource resource = new TbResource();
            resource.setTitle(TEST_PREFIX + "Resource_" + timestamp + "_" + i);
            resource.setResourceType(ResourceType.JS_MODULE);
            resource.setResourceKey("test_module_" + timestamp + "_" + i + ".js");
            resource.setFileName("test_module_" + timestamp + "_" + i + ".js");

            String jsContent = "export default function test" + i + "() { return " + i + "; }";
            resource.setData(Base64.getEncoder().encodeToString(jsContent.getBytes()));

            TbResourceInfo created = client.saveResource(resource);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(resource.getTitle(), created.getTitle());
            assertEquals(ResourceType.JS_MODULE, created.getResourceType());

            createdResources.add(created);
        }

        // get tenant resources, check count
        PageDataTbResourceInfo tenantResources = client.getTenantResources(100, 0, null, null, null);
        assertNotNull(tenantResources);
        assertNotNull(tenantResources.getData());
        int initialSize = tenantResources.getData().size();
        assertTrue("Expected at least 5 resources, but got " + initialSize, initialSize >= 5);

        // find with text search
        PageDataTbResourceInfo filteredResources = client.getTenantResources(100, 0,
                TEST_PREFIX + "Resource_" + timestamp, null, null);
        assertEquals(5, filteredResources.getData().size());

        // get resources with type filter
        PageDataTbResourceInfo jsResources = client.getResources(100, 0,
                ResourceType.JS_MODULE.getValue(), null, TEST_PREFIX + "Resource_" + timestamp, null, null);
        assertEquals(5, jsResources.getData().size());

        // get resource info by id
        TbResourceInfo searchResource = createdResources.get(2);
        TbResourceInfo fetchedInfo = client.getResourceInfoById(searchResource.getId().getId().toString());
        assertEquals(searchResource.getTitle(), fetchedInfo.getTitle());
        assertEquals(searchResource.getResourceKey(), fetchedInfo.getResourceKey());

        // get full resource by id (includes data)
        TbResource fullResource = client.getResourceById(searchResource.getId().getId().toString());
        assertNotNull(fullResource);
        assertEquals(searchResource.getTitle(), fullResource.getTitle());
        assertNotNull(fullResource.getData());

        // download resource
        File downloadedFile = client.downloadResource(searchResource.getId().getId().toString());
        assertNotNull(downloadedFile);
        assertTrue(downloadedFile.exists());
        assertTrue(downloadedFile.length() > 0);

        // get resources by list of ids
        List<String> idsToFetch = List.of(
                createdResources.get(0).getId().getId().toString(),
                createdResources.get(1).getId().getId().toString()
        );
        List<TbResourceInfo> resourceList = client.getSystemOrTenantResourcesByIds(idsToFetch);
        assertEquals(2, resourceList.size());

        // update resource
        TbResource resourceToUpdate = client.getResourceById(createdResources.get(3).getId().getId().toString());
        resourceToUpdate.setTitle(resourceToUpdate.getTitle() + "_updated");
        String updatedContent = "export default function updated() { return 42; }";
        resourceToUpdate.setData(Base64.getEncoder().encodeToString(updatedContent.getBytes()));
        TbResourceInfo updatedResource = client.saveResource(resourceToUpdate);
        assertEquals(resourceToUpdate.getTitle(), updatedResource.getTitle());

        // delete resource
        UUID resourceToDeleteId = createdResources.get(0).getId().getId();
        client.deleteResource(resourceToDeleteId.toString(), false);

        // verify deletion
        assertReturns404(() ->
                client.getResourceInfoById(resourceToDeleteId.toString())
        );

        PageDataTbResourceInfo resourcesAfterDelete = client.getTenantResources(100, 0,
                TEST_PREFIX + "Resource_" + timestamp, null, null);
        assertEquals(4, resourcesAfterDelete.getData().size());
    }

}
