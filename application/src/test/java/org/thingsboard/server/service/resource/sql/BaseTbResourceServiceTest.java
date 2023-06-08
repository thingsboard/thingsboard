/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.resource.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.resource.TbResourceService;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class BaseTbResourceServiceTest extends AbstractControllerTest {

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
            "<Operations>RW</Operations>\n" +
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

    private static final String LWM2M_TEST_MODEL_WITH_XXE = "<!DOCTYPE replace [<!ENTITY ObjectVersion SYSTEM \"file:///etc/hostname\"> ]>" +
            "<LWM2M xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.openmobilealliance.org/tech/profiles/LWM2M-v1_1.xsd\">\n" +
            "<Object ObjectType=\"MODefinition\">\n" +
            "<Name>My first resource</Name>\n" +
            "<Description1></Description1>\n" +
            "<ObjectID>0</ObjectID>\n" +
            "<ObjectURN></ObjectURN>\n" +
            "<ObjectVersion>&ObjectVersion;</ObjectVersion>\n" +
            "<MultipleInstances>Multiple</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Resources>\n" +
            "<Item ID=\"0\">\n" +
            "<Name>LWM2M</Name>\n" +
            "<Operations>RW</Operations>\n" +
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

    private static final String DEFAULT_FILE_NAME = "test.jks";

    private IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Autowired
    private TbResourceService resourceService;

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
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
    public void testSaveResourceWithMaxSumDataSizeOutOfLimit() throws Exception {
        loginSysAdmin();
        long limit = 1;
        EntityInfo defaultTenantProfileInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        TenantProfile defaultTenantProfile = doGet("/api/tenantProfile/" + defaultTenantProfileInfo.getId().getId().toString(), TenantProfile.class);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxResourcesInBytes(limit).build());
        doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);

        loginTenantAdmin();

        assertEquals(0, resourceService.sumDataSizeByTenantId(tenantId));

        createResource("test", DEFAULT_FILE_NAME);

        assertEquals(1, resourceService.sumDataSizeByTenantId(tenantId));

        try {
            assertThatThrownBy(() -> createResource("test1", 1 + DEFAULT_FILE_NAME))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageContaining("Failed to create the tb resource, files size limit is exhausted %d bytes!", limit);
        } finally {
            defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxResourcesInBytes(0).build());
            loginSysAdmin();
            doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);
        }
    }

    @Test
    public void sumDataSizeByTenantId() throws Exception {
        assertEquals(0, resourceService.sumDataSizeByTenantId(tenantId));

        createResource("test", DEFAULT_FILE_NAME);
        assertEquals(1, resourceService.sumDataSizeByTenantId(tenantId));

        int maxSumDataSize = 8;

        for (int i = 2; i <= maxSumDataSize; i++) {
            createResource("test" + i, i + DEFAULT_FILE_NAME);
            assertEquals(i, resourceService.sumDataSizeByTenantId(tenantId));
        }

        assertEquals(maxSumDataSize, resourceService.sumDataSizeByTenantId(tenantId));
    }

    private TbResource createResource(String title, String filename) throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setTitle(title);
        resource.setResourceType(ResourceType.JKS);
        resource.setFileName(filename);
        resource.setData("1");
        return resourceService.save(resource);
    }

    @Test
    public void testSaveTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = resourceService.save(resource);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        assertEquals(resource.getTenantId(), savedResource.getTenantId());
        assertEquals(resource.getTitle(), savedResource.getTitle());
        assertEquals(resource.getResourceKey(), savedResource.getResourceKey());
        assertEquals(resource.getData(), savedResource.getData());

        savedResource.setTitle("My new resource");

        resourceService.save(savedResource);
        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        assertEquals(foundResource.getTitle(), savedResource.getTitle());

        resourceService.delete(savedResource, null);
    }

    @Test
    public void testSaveLwm2mTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("test_model.xml");
        resource.setData(Base64.getEncoder().encodeToString(LWM2M_TEST_MODEL.getBytes()));

        TbResource savedResource = resourceService.save(resource);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        assertEquals(resource.getTenantId(), savedResource.getTenantId());
        assertEquals("My first resource id=0 v1.0", savedResource.getTitle());
        assertEquals("0_1.0", savedResource.getResourceKey());
        assertEquals(resource.getData(), savedResource.getData());

        resourceService.delete(savedResource, null);
    }

    @Test
    public void testSaveTbResourceWithEmptyTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        TbResource savedResource = resourceService.save(resource);

        assertEquals(TenantId.SYS_TENANT_ID, savedResource.getTenantId());

        resourceService.delete(savedResource, null);
    }

    @Test
    public void testSaveTbResourceWithExistsFileName() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        TbResource savedResource = resourceService.save(resource);

        TbResource resource2 = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");

        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                resourceService.save(resource2);
            });
        } finally {
            resourceService.delete(savedResource, null);
        }
    }

    @Test
    public void testSaveTbResourceWithEmptyTitle() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        Assertions.assertThrows(DataValidationException.class, () -> {
            resourceService.save(resource);
        });
    }

    @Test
    public void testSaveTbResourceWithInvalidTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        Assertions.assertThrows(DataValidationException.class, () -> {
            resourceService.save(resource);
        });
    }

    @Test
    public void testSaveLwm2mTbResourceWithXXE() {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("xxe_test_model.xml");
        resource.setData(Base64.getEncoder().encodeToString(LWM2M_TEST_MODEL_WITH_XXE.getBytes()));

        DataValidationException thrown = assertThrows(DataValidationException.class, () -> {
            resourceService.save(resource);
        });
        assertEquals("Failed to parse file xxe_test_model.xml", thrown.getMessage());
    }


    @Test
    public void testFindResourceById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        TbResource savedResource = resourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNotNull(foundResource);
        assertEquals(savedResource, foundResource);
        resourceService.delete(savedResource, null);
    }

    @Test
    public void testFindResourceByTenantIdAndResourceTypeAndResourceKey() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTenantId(tenantId);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        TbResource savedResource = resourceService.save(resource);

        TbResource foundResource = resourceService.getResource(tenantId, savedResource.getResourceType(), savedResource.getResourceKey());
        Assert.assertNotNull(foundResource);
        assertEquals(savedResource, foundResource);
        resourceService.delete(savedResource, null);
    }

    @Test
    public void testDeleteResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData("Test Data");
        TbResource savedResource = resourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNotNull(foundResource);
        resourceService.delete(savedResource, null);
        foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNull(foundResource);
    }

    @Test
    public void testFindTenantResourcesByTenantId() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = doPost("/api/tenant", tenant, Tenant.class);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(resourceService.save(resource)));
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(4);
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

        assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(3);
        pageData = resourceService.findTenantResourcesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindAllTenantResourcesByTenantId() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = doPost("/api/tenant", tenant, Tenant.class);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(TenantId.SYS_TENANT_ID);
            resource.setTitle("System Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            TbResourceInfo tbResourceInfo = new TbResourceInfo(resourceService.save(resource));
            if (i >= 50) {
                resources.add(tbResourceInfo);
            }
        }

        for (int i = 0; i < 50; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Tenant Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData("Test Data");
            resources.add(new TbResourceInfo(resourceService.save(resource)));
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

        assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(100);
        pageData = resourceService.findAllTenantResourcesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        assertEquals(pageData.getData().size(), 100);

        resourceService.deleteResourcesByTenantId(TenantId.SYS_TENANT_ID);

        pageLink = new PageLink(100);
        pageData = resourceService.findAllTenantResourcesByTenantId(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());
    }

}
