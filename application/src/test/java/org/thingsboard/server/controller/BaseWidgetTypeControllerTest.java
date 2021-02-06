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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseWidgetTypeControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private WidgetsBundle savedWidgetsBundle;
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

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveWidgetType() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getAlias());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());
        Assert.assertEquals(savedWidgetsBundle.getAlias(), savedWidgetType.getBundleAlias());

        savedWidgetType.setName("New Widget Type");

        doPost("/api/widgetType", savedWidgetType, WidgetType.class);

        WidgetType foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetType.class);
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());
    }

    @Test
    public void testUpdateWidgetTypeFromDifferentTenant() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);

        loginDifferentTenant();
        doPost("/api/widgetType", savedWidgetType, WidgetType.class, status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testFindWidgetTypeById() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        WidgetType foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetType.class);
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
    }

    @Test
    public void testDeleteWidgetType() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);

        doDelete("/api/widgetType/"+savedWidgetType.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/widgetType/"+savedWidgetType.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveWidgetTypeWithEmptyName() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type name should be specified")));
    }

    @Test
    public void testSaveWidgetTypeWithEmptyBundleAlias() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type bundle alias should be specified")));
    }

    @Test
    public void testSaveWidgetTypeWithEmptyDescriptor() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{}", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type descriptor can't be empty")));
    }

    @Test
    public void testSaveWidgetTypeWithInvalidBundleAlias() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias("some_alias");
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widget type is referencing to non-existent widgets bundle")));
    }

    @Test
    public void testUpdateWidgetTypeBundleAlias() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        savedWidgetType.setBundleAlias("some_alias");
        doPost("/api/widgetType", savedWidgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widget type bundle alias is prohibited")));

    }

    @Test
    public void testUpdateWidgetTypeAlias() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        savedWidgetType.setAlias("some_alias");
        doPost("/api/widgetType", savedWidgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widget type alias is prohibited")));

    }

    @Test
    public void testGetBundleWidgetTypes() throws Exception {
        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i=0;i<89;i++) {
            WidgetType widgetType = new WidgetType();
            widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(doPost("/api/widgetType", widgetType, WidgetType.class));
        }

        List<WidgetType> loadedWidgetTypes = doGetTyped("/api/widgetTypes?isSystem={isSystem}&bundleAlias={bundleAlias}",
                new TypeReference<List<WidgetType>>(){}, false, savedWidgetsBundle.getAlias());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);
    }

    @Test
    public void testGetWidgetType() throws Exception {
        WidgetType widgetType = new WidgetType();
        widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(new ObjectMapper().readValue("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        WidgetType foundWidgetType = doGet("/api/widgetType?isSystem={isSystem}&bundleAlias={bundleAlias}&alias={alias}",
                WidgetType.class, false, savedWidgetsBundle.getAlias(), savedWidgetType.getAlias());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
    }

}
