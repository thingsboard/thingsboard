/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.security.Authority;

import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseEntityViewControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;
    private Device testDevice;
    private TelemetryEntityView obj;

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

        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        testDevice = doPost("/api/device", device, Device.class);

        obj = new TelemetryEntityView(
                Arrays.asList("109L", "209L"),
                new AttributesEntityView(
                        Arrays.asList("caKey1", "caKey2", "caKey3"),
                        Arrays.asList("saKey1", "saKey2", "saKey3", "saKey4"),
                        Arrays.asList("shKey1", "shKey2", "shKey3", "shKey4", "shKey5")));
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindEntityViewById() throws Exception {
        EntityView view = new EntityView();
        view.setName("Test entity view");
        view.setEntityId(testDevice.getId());
        view.setKeys(new TelemetryEntityView(obj));
        EntityView savedView = doPost("/api/entity-view", view, EntityView.class);
        EntityView foundView = doGet("/api/entity-view/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertNotNull(foundView);
        Assert.assertEquals(savedView, foundView);
    }

    @Test
    public void testSaveEntityViewWithIdOfDevice() throws Exception {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setName("Test entity view");
        view.setTenantId(savedTenant.getId());
        view.setKeys(new TelemetryEntityView(obj));
        EntityView savedView = doPost("/api/entity-view", view, EntityView.class);

        Assert.assertNotNull(savedView);
        Assert.assertNotNull(savedView.getId());
        Assert.assertTrue(savedView.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedView.getTenantId());
        Assert.assertNotNull(savedView.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedView.getCustomerId().getId());
        Assert.assertEquals(savedView.getName(), savedView.getName());

        savedView.setName("New test entity view");
        doPost("/api/entity-view", savedView, EntityView.class);

        EntityView foundEntityView = doGet("/api/entity-view/"
                + savedView.getId().getId().toString(), EntityView.class);

        Assert.assertEquals(foundEntityView.getName(), savedView.getName());
    }

    @Test
    public void testDeleteEntityView() throws Exception {
        EntityView view = new EntityView();
        view.setName("Test entity view");
        view.setEntityId(testDevice.getId());
        view.setKeys(new TelemetryEntityView((TelemetryEntityView) obj));
        EntityView savedView = doPost("/api/entity-view", view, EntityView.class);

        doDelete("/api/entity-view/" + savedView.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/entity-view/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound());
    }
}
