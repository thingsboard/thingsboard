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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class WidgetsBundleControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
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

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");

        Mockito.reset(tbClusterService, auditLogService);

        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Mockito.reset(tbClusterService, auditLogService);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");
        savedWidgetsBundle = doPost("/api/widgetsBundle", savedWidgetsBundle, WidgetsBundle.class);

        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);
    }

    @Test
    public void testSaveWidgetBundleWithViolationOfLengthValidation() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle(StringUtils.randomAlphabetic(300));

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("title");
        doPost("/api/widgetsBundle", widgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(widgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException("Validation error: title length must be equal or less than 255"));
    }

    @Test
    public void testUpdateWidgetsBundleFromDifferentTenant() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/widgetsBundle", savedWidgetsBundle)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedWidgetsBundle.getId(), savedWidgetsBundle);

        deleteDifferentTenant();
    }

    @Test
    public void testFindWidgetsBundleById() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
    }

    @Test
    public void testFindWidgetsBundlesByIds() throws Exception {
        List<WidgetsBundle> savedWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("My widgets bundle " + i);
            savedWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        String idsParam = savedWidgetsBundles.stream()
                .map(wb -> wb.getId().getId().toString())
                .collect(Collectors.joining(","));

        WidgetsBundle[] foundWidgetsBundles =
                doGet("/api/widgetsBundles?widgetsBundleIds=" + idsParam, WidgetsBundle[].class);

        Assert.assertNotNull(foundWidgetsBundles);
        Assert.assertEquals(savedWidgetsBundles.size(), foundWidgetsBundles.length);

        Map<UUID, WidgetsBundle> foundById = Arrays.stream(foundWidgetsBundles)
                .collect(Collectors.toMap(wb -> wb.getId().getId(), Function.identity()));

        for (WidgetsBundle savedWidgetsBundle : savedWidgetsBundles) {
            UUID id = savedWidgetsBundle.getId().getId();
            WidgetsBundle foundWidgetsBundle = foundById.get(id);
            Assert.assertNotNull("WidgetsBundle not found for id " + id, foundWidgetsBundle);
            Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        }
    }


    @Test
    public void testDeleteWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");

        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString())
                .andExpect(status().isOk());

        String savedWidgetsBundleIdStr = savedWidgetsBundle.getId().getId().toString();

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.DELETED);

        doGet("/api/widgetsBundle/" + savedWidgetsBundleIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Widgets bundle", savedWidgetsBundleIdStr))));
    }

    @Test
    public void testSaveWidgetsBundleWithEmptyTitle() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        doPost("/api/widgetsBundle", widgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets bundle title " + msgErrorShouldBeSpecified)));

        testNotifyEntityEqualsOneTimeServiceNeverError(widgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException("Widgets bundle title should be specified!"));
    }

    @Test
    public void testUpdateWidgetsBundleAlias() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        savedWidgetsBundle.setAlias("new_alias");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/widgetsBundle", savedWidgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widgets bundle alias is prohibited")));
        testNotifyEntityEqualsOneTimeServiceNeverError(savedWidgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED,
                new DataValidationException("Update of widgets bundle alias is prohibited!"));
    }

    @Test
    public void testFindTenantWidgetsBundlesByPageLink() throws Exception {
        loginSysAdmin();

        //upload some system bundles
        int sysCntEntity = 10;
        for (int i = 0; i < sysCntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        }

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        login(tenantAdmin.getEmail(), "testPassword1");

        int cntEntity = 73;
        List<WidgetsBundle> tenantWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            tenantWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> allWidgetsBundles = new ArrayList<>(tenantWidgetsBundles);
        allWidgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(allWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allWidgetsBundles, loadedWidgetsBundles);

        //retrieve tenant only bundles
        List<WidgetsBundle> loadedWidgetsBundles2 = new ArrayList<>();
        PageLink pageLink2 = new PageLink(14);
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?tenantOnly=true&",
                    new TypeReference<>() {
                    }, pageLink2);
            loadedWidgetsBundles2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink2 = pageLink2.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles2, idComparator);

        Assert.assertEquals(tenantWidgetsBundles, loadedWidgetsBundles2);

        // cleanup
        loginSysAdmin();
        for (WidgetsBundle sysWidgetsBundle : sysWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + sysWidgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testFindSystemWidgetsBundlesByPageLink() throws Exception {

        loginSysAdmin();

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        int cntEntity = 120;
        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            createdWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(17);
        loadedWidgetsBundles.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<PageData<WidgetsBundle>>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }


    @Test
    public void testFindTenantWidgetsBundles() throws Exception {

        login(tenantAdmin.getEmail(), "testPassword1");

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>() {
                });

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i = 0; i < 73; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindSystemAndTenantWidgetsBundles() throws Exception {

        loginSysAdmin();


        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < 82; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Sys widgets bundle" + i);
            createdSystemWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> systemWidgetsBundles = new ArrayList<>(createdSystemWidgetsBundles);
        systemWidgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        widgetsBundles.addAll(systemWidgetsBundles);

        login(tenantAdmin.getEmail(), "testPassword1");

        for (int i = 0; i < 127; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Tenant widgets bundle" + i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>() {
                });

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        loginSysAdmin();

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }

}
