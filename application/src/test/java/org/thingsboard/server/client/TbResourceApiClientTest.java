// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteResourceArgs;
import org.thingsboard.client.api.ThingsboardApi.DownloadResourceArgs;
import org.thingsboard.client.api.ThingsboardApi.GetResourceByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetResourceInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetResourcesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSystemOrTenantResourcesByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantResourcesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveResourceArgs;
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

            TbResourceInfo created = client.saveResource(SaveResourceArgs.builder()
                    .tbResource(resource)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(resource.getTitle(), created.getTitle());
            assertEquals(ResourceType.JS_MODULE, created.getResourceType());

            createdResources.add(created);
        }

        // get tenant resources, check count
        PageDataTbResourceInfo tenantResources = client.getTenantResources(GetTenantResourcesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(tenantResources);
        assertNotNull(tenantResources.getData());
        int initialSize = tenantResources.getData().size();
        assertTrue("Expected at least 5 resources, but got " + initialSize, initialSize >= 5);

        // find with text search
        PageDataTbResourceInfo filteredResources = client.getTenantResources(GetTenantResourcesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Resource_" + timestamp)
                .build());
        assertEquals(5, filteredResources.getData().size());

        // get resources with type filter
        PageDataTbResourceInfo jsResources = client.getResources(GetResourcesArgs.builder()
                .pageSize(100)
                .page(0)
                .resourceType(ResourceType.JS_MODULE.getValue())
                .textSearch(TEST_PREFIX + "Resource_" + timestamp)
                .build());
        assertEquals(5, jsResources.getData().size());

        // get resource info by id
        TbResourceInfo searchResource = createdResources.get(2);
        TbResourceInfo fetchedInfo = client.getResourceInfoById(GetResourceInfoByIdArgs.builder()
                .resourceId(searchResource.getId().getId().toString())
                .build());
        assertEquals(searchResource.getTitle(), fetchedInfo.getTitle());
        assertEquals(searchResource.getResourceKey(), fetchedInfo.getResourceKey());

        // get full resource by id (includes data)
        TbResource fullResource = client.getResourceById(GetResourceByIdArgs.builder()
                .resourceId(searchResource.getId().getId().toString())
                .build());
        assertNotNull(fullResource);
        assertEquals(searchResource.getTitle(), fullResource.getTitle());
        assertNotNull(fullResource.getData());

        // download resource
        File downloadedFile = client.downloadResource(DownloadResourceArgs.builder()
                .resourceId(searchResource.getId().getId().toString())
                .build());
        assertNotNull(downloadedFile);
        assertTrue(downloadedFile.exists());
        assertTrue(downloadedFile.length() > 0);

        // get resources by list of ids
        List<String> idsToFetch = List.of(
                createdResources.get(0).getId().getId().toString(),
                createdResources.get(1).getId().getId().toString()
        );
        List<TbResourceInfo> resourceList = client.getSystemOrTenantResourcesByIds(GetSystemOrTenantResourcesByIdsArgs.builder()
                .resourceIds(idsToFetch)
                .build());
        assertEquals(2, resourceList.size());

        // update resource
        TbResource resourceToUpdate = client.getResourceById(GetResourceByIdArgs.builder()
                .resourceId(createdResources.get(3).getId().getId().toString())
                .build());
        resourceToUpdate.setTitle(resourceToUpdate.getTitle() + "_updated");
        String updatedContent = "export default function updated() { return 42; }";
        resourceToUpdate.setData(Base64.getEncoder().encodeToString(updatedContent.getBytes()));
        TbResourceInfo updatedResource = client.saveResource(SaveResourceArgs.builder()
                .tbResource(resourceToUpdate)
                .build());
        assertEquals(resourceToUpdate.getTitle(), updatedResource.getTitle());

        // delete resource
        UUID resourceToDeleteId = createdResources.get(0).getId().getId();
        client.deleteResource(DeleteResourceArgs.builder()
                .resourceId(resourceToDeleteId.toString())
                .force(false)
                .build());

        // verify deletion
        assertReturns404(() ->
                client.getResourceInfoById(GetResourceInfoByIdArgs.builder()
                        .resourceId(resourceToDeleteId.toString())
                        .build())
        );

        PageDataTbResourceInfo resourcesAfterDelete = client.getTenantResources(GetTenantResourcesArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + "Resource_" + timestamp)
                .build());
        assertEquals(4, resourcesAfterDelete.getData().size());
    }

}
