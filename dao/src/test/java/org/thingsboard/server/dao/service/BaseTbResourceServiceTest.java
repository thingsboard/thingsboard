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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public abstract class BaseTbResourceServiceTest extends AbstractServiceTest {

    private static final String LWM2M_TEST_MODEL = "<LWM2M xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.openmobilealliance.org/tech/profiles/LWM2M-v1_1.xsd\">\n" +
            "<Object ObjectType=\"MODefinition\">\n" +
            "<Name>My first resource</Name>\n" +
            "<Description1></Description1>\n" +
            "<ObjectID>0</ObjectID>\n" +
            "<ObjectURN></ObjectURN>\n" +
            "<ObjectVersion>1.0</ObjectVersion>\n" +
            "<MultipleInstances>Multiple</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Resources>\n" +
            "<Item ID=\"0\">\n" +
            "<Name>LWM2M</Name>\n" +
            "<Operations></Operations>\n" +
            "<MultipleInstances>Single</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Type>String</Type>\n" +
            "<RangeEnumeration>0..255</RangeEnumeration>\n" +
            "<Units></Units>\n" +
            "<Description></Description>\n" +
            "</Item>\n" +
            "</Resources>\n" +
            "<Description2></Description2>\n" +
            "</Object>\n" +
            "</LWM2M>";

    private IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");

        TbResource savedResource = resourceService.saveResource(resource);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        Assert.assertEquals(resource.getTenantId(), savedResource.getTenantId());
        Assert.assertEquals(resource.getTitle(), savedResource.getTitle());
        Assert.assertEquals(resource.getResourceKey(), savedResource.getResourceKey());
        Assert.assertEquals(resource.getData(), savedResource.getData());

        savedResource.setTitle("My new resource");

        resourceService.saveResource(savedResource);
        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertEquals(foundResource.getTitle(), savedResource.getTitle());

        resourceService.deleteResource(tenantId, savedResource.getId());
    }

    @Test
    public void testSaveLwm2mTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setData(Base64.getEncoder().encodeToString(LWM2M_TEST_MODEL.getBytes()));

        TbResource savedResource = resourceService.saveResource(resource);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        Assert.assertEquals(resource.getTenantId(), savedResource.getTenantId());
        Assert.assertEquals("My first resource", savedResource.getTitle());
        Assert.assertEquals("0_1.0", savedResource.getResourceKey());
        Assert.assertEquals(resource.getData(), savedResource.getData());

        resourceService.deleteResource(tenantId, savedResource.getId());
    }

    @Test
    public void testSaveTbResourceWithEmptyTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        TbResource savedResource = resourceService.saveResource(resource);

        Assert.assertEquals(TenantId.SYS_TENANT_ID, savedResource.getTenantId());

        resourceService.deleteResource(tenantId, savedResource.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTbResourceWithExistsKey() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");

        TbResource savedResource = resourceService.saveResource(resource);

        TbResource resource2 = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");

        try {
            resourceService.saveResource(resource2);
        } finally {
            resourceService.deleteResource(tenantId, savedResource.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTbResourceWithEmptyTitle() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        resourceService.saveResource(resource);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTbResourceWithInvalidTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(new TenantId(Uuids.timeBased()));
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        resourceService.saveResource(resource);
    }

    @Test
    public void testFindResourceById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        TbResource savedResource = resourceService.saveResource(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource, foundResource);
        resourceService.deleteResource(tenantId, savedResource.getId());
    }

    @Test
    public void testFindResourceByTenantIdAndResourceTypeAndResourceKey() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTenantId(tenantId);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        TbResource savedResource = resourceService.saveResource(resource);

        TbResource foundResource = resourceService.getResource(tenantId, savedResource.getResourceType(), savedResource.getResourceKey());
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource, foundResource);
        resourceService.deleteResource(tenantId, savedResource.getId());
    }

    @Test
    public void testDeleteResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setResourceKey("Test resource");
        resource.setData("Test Data");
        TbResource savedResource = resourceService.saveResource(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNotNull(foundResource);
        resourceService.deleteResource(tenantId, savedResource.getId());
        foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNull(foundResource);
    }

    @Test
    public void testFindTenantResourcesByTenantId() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setResourceKey("Key" + i);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(resourceService.saveResource(resource)));
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = resourceService.findTenantResourcesByTenantId(tenantId, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = resourceService.findTenantResourcesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAllTenantResourcesByTenantId() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(TenantId.SYS_TENANT_ID);
            resource.setTitle("System Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setResourceKey("Key" + i);
            resource.setData("Test Data");
            TbResourceInfo tbResourceInfo = new TbResourceInfo(resourceService.saveResource(resource));
            if (i >= 50) {
                resources.add(tbResourceInfo);
            }
        }

        for (int i = 0; i < 50; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Tenant Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setResourceKey("Key" + i);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(resourceService.saveResource(resource)));
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(10);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = resourceService.findAllTenantResourcesByTenantId(tenantId, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(100);
        pageData = resourceService.findAllTenantResourcesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(pageData.getData().size(), 100);

        resourceService.deleteResourcesByTenantId(TenantId.SYS_TENANT_ID);

        pageLink = new PageLink(100);
        pageData = resourceService.findAllTenantResourcesByTenantId(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

}
