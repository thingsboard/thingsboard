/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.TbResourceDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {BaseTbResourceControllerTest.Config.class})
public abstract class BaseTbResourceControllerTest extends AbstractControllerTest {

    private IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private static final String DEFAULT_FILE_NAME = "test.jks";

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private TbResourceDao tbResourceDao;

    static class Config {
        @Bean
        @Primary
        public TbResourceDao tbResourceDao(TbResourceDao tbResourceDao) {
            return Mockito.mock(TbResourceDao.class, AdditionalAnswers.delegatesTo(tbResourceDao));
        }
    }

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

        Mockito.reset(tbClusterService, auditLogService);

        TbResource resource = createTbResource();

        TbResource savedResource = savedTbResource(resource);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedResource.getTenantId());
        Assert.assertEquals(resource.getTitle(), savedResource.getTitle());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getFileName());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getResourceKey());
        Assert.assertEquals(resource.getData(), savedResource.getData());

        savedResource.setTitle("My new resource");

        savedTbResource(savedResource);

        TbResource foundResource = doGet("/api/resource/" + savedResource.getId().getId().toString(), TbResource.class);
        Assert.assertEquals(foundResource.getTitle(), savedResource.getTitle());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundResource, foundResource.getId(), foundResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveResourceInfoWithViolationOfLengthValidation() throws Exception {
        TbResource resource = createTbResource(StringUtils.randomAlphabetic(300), "Test Data");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("title");
        doPost("/api/resource", resource)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(resource, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateTbResourceFromDifferentTenant() throws Exception {
       TbResource savedResource = createAndSavedTbResource();

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/resource", savedResource)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedResource.getId(), savedResource);

        doDelete("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedResource.getId(), savedResource);

        deleteDifferentTenant();
    }

    @Test
    public void testFindTbResourceById() throws Exception {
        TbResource savedResource = createAndSavedTbResource();

        TbResource foundResource = doGet("/api/resource/" + savedResource.getId().getId().toString(), TbResource.class);
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource, foundResource);
    }

    @Test
    public void testDeleteTbResource() throws Exception {
        TbResource savedResource = createAndSavedTbResource();

        Mockito.reset(tbClusterService, auditLogService);
        String resourceIdStr = savedResource.getId().getId().toString();
        doDelete("/api/resource/" + resourceIdStr)
                .andExpect(status().isOk());


        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                                ActionType.DELETED, resourceIdStr);

        doGet("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Resource", resourceIdStr))));
    }

    @Test
    public void testFindTenantTbResources() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        List<TbResourceInfo> resources = new ArrayList<>();
        int cntEntity = 173;
        for (int i = 0; i < cntEntity; i++) {
            TbResource resource = createTbResource();
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resources.add(new TbResourceInfo(savedTbResource(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new TbResource(), new TbResource(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);
    }

    @Test
    public void testFindSystemTbResources() throws Exception {
        loginSysAdmin();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 173; i++) {
            TbResource resource = createTbResource("Resource" + i, "Test Data");
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resources.add(new TbResourceInfo(savedTbResource(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        Assert.assertEquals(resources, loadedResources);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = resources.size();
        for (TbResourceInfo resource : resources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new TbResource(), new TbResource(),
                resources.get(0).getTenantId(), null, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, cntEntity, 1);

        pageLink = new PageLink(27);
        loadedResources.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
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
            TbResource resource = createTbResource("Resource" + i, "Test Data");
            resource.setFileName(i + DEFAULT_FILE_NAME);
            expectedResources.add(new TbResourceInfo(savedTbResource(resource)));
        }

        loginSysAdmin();

        for (int i = 0; i < 173; i++) {
            TbResource resource = createTbResource("Resource" + i, "Test Data");
            resource.setFileName(i + DEFAULT_FILE_NAME);
            TbResourceInfo savedResource = new TbResourceInfo(savedTbResource(resource));
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


    @Test
    public void testDeleteTbResourceWithTransactionalOk() throws Exception {
        TbResource savedResource = createAndSavedTbResource();

        Mockito.reset(tbClusterService, auditLogService);
        String resourceIdStr = savedResource.getId().getId().toString();
        doDelete("/api/resource/" + resourceIdStr)
                .andExpect(status().isOk());

        doGet("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Resource", resourceIdStr))));;
    }

    @Test
    public void testDeleteTbResourceWithTransactionalException() throws Exception {
        TbResource tbResource = createAndSavedTbResource("MOCK_TransactionalException", "Test Data");
        TbResource foundTbResource = doGet("/api/resource/" + tbResource.getId().getId().toString(), TbResource.class);
        Assert.assertNotNull(foundTbResource);

        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(tbResourceDao).removeById(any(), any());
        try {
            doDelete("/api/resource/" + foundTbResource.getId().getId().toString())
                    .andExpect(status().isInternalServerError());

            doGet("/api/resource/" + foundTbResource.getId().getId().toString())
                    .andExpect(status().isOk());
        } finally {
            Mockito.reset(tbResourceDao);
        }
    }

    private TbResource createTbResource(String... args) throws Exception {
        String title = args.length == 2 ? args[0] : "My first resource";
        String data = args.length == 2 ? args[1] : "Test Data";
        TbResource tbResource = new TbResource();
        tbResource.setResourceType(ResourceType.JKS);
        tbResource.setTitle(title);
        tbResource.setFileName(DEFAULT_FILE_NAME);
        tbResource.setData(data);
        return tbResource;
    }

    private TbResource createAndSavedTbResource(String... args) throws Exception {
        TbResource tbResource = createTbResource(args);
        return savedTbResource(tbResource);
    }

    private TbResource savedTbResource(TbResource tbResource) throws Exception {
        return doPostWithTypedResponse("/api/resource", tbResource, new TypeReference<>(){});

    }
}
