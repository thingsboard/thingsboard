/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseWidgetsBundleControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

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

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");
        doPost("/api/widgetsBundle", savedWidgetsBundle, WidgetsBundle.class);

        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());
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
    public void testDeleteWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        doDelete("/api/widgetsBundle/"+savedWidgetsBundle.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/widgetsBundle/"+savedWidgetsBundle.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveWidgetsBundleWithEmptyTitle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        doPost("/api/widgetsBundle", widgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets bundle title should be specified")));
    }

    @Test
    public void testUpdateWidgetsBundleAlias() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        savedWidgetsBundle.setAlias("new_alias");
        doPost("/api/widgetsBundle", savedWidgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widgets bundle alias is prohibited")));

    }

    @Test
    public void testFindTenantWidgetsBundlesByPageLink() throws Exception {

        login(tenantAdmin.getEmail(), "testPassword1");

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});


        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i=0;i<73;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle"+i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<PageData<WidgetsBundle>>(){}, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindSystemWidgetsBundlesByPageLink() throws Exception {

        loginSysAdmin();

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<120;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle"+i);
            createdWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<PageData<WidgetsBundle>>(){}, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            doDelete("/api/widgetsBundle/"+widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(17);
        loadedWidgetsBundles.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<PageData<WidgetsBundle>>(){}, pageLink);
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
                new TypeReference<List<WidgetsBundle>>(){});

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i=0;i<73;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle"+i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindSystemAndTenantWidgetsBundles() throws Exception {

        loginSysAdmin();


        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<82;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Sys widgets bundle"+i);
            createdSystemWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> systemWidgetsBundles = new ArrayList<>(createdSystemWidgetsBundles);
        systemWidgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        widgetsBundles.addAll(systemWidgetsBundles);

        login(tenantAdmin.getEmail(), "testPassword1");

        for (int i=0;i<127;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Tenant widgets bundle"+i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        loginSysAdmin();

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            doDelete("/api/widgetsBundle/"+widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>(){});

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }

}
