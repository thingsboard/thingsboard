/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseTbResourceControllerTest extends AbstractControllerTest {

    private IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private static final String DEFAULT_FILE_NAME = "test.jks";

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = save(resource);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedResource.getTenantId());
        Assert.assertEquals(resource.getTitle(), savedResource.getTitle());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getFileName());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getResourceKey());
        Assert.assertEquals(resource.getData(), savedResource.getData());

        savedResource.setTitle("My new resource");

        save(savedResource);

        TbResource foundResource = doGet("/api/resource/" + savedResource.getId().getId().toString(), TbResource.class);
        Assert.assertEquals(foundResource.getTitle(), savedResource.getTitle());
    }

    @Test
    public void testUpdateTbResourceFromDifferentTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = save(resource);

        loginDifferentTenant();
        doPostWithTypedResponse("/api/resource", savedResource, new TypeReference<>(){}, status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testFindTbResourceById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = save(resource);

        TbResource foundResource = doGet("/api/resource/" + savedResource.getId().getId().toString(), TbResource.class);
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource, foundResource);
    }

    @Test
    public void testDeleteTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = save(resource);

        doDelete("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTenantTbResources() throws Exception {
        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 173; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(save(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<PageData<TbResourceInfo>>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);
    }

    @Test
    public void testFindSystemTbResources() throws Exception {
        loginSysAdmin();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 173; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(save(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<PageData<TbResourceInfo>>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);

        for (TbResourceInfo resource : resources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(27);
        loadedResources.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<PageData<TbResourceInfo>>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(loadedResources.isEmpty());
    }

    @Test
    public void testFindSystemAndTenantTbResources() throws Exception {
        List<TbResourceInfo> systemResources = new ArrayList<>();
        List<TbResourceInfo> expectedResources = new ArrayList<>();
        for (int i = 0; i < 73; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            expectedResources.add(new TbResourceInfo(save(resource)));
        }

        loginSysAdmin();

        for (int i = 0; i < 173; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            TbResourceInfo savedResource = new TbResourceInfo(save(resource));
            systemResources.add(savedResource);
            if (i >= 73) {
                expectedResources.add(savedResource);
            }
        }

        login(tenantAdmin.getEmail(), "testPassword1");

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<PageData<TbResourceInfo>>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(expectedResources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(expectedResources, loadedResources);

        loginSysAdmin();

        for (TbResourceInfo resource : systemResources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    private TbResource save(TbResource tbResource) throws Exception {
        return doPostWithTypedResponse("/api/resource", tbResource, new TypeReference<>(){});
    }
}
