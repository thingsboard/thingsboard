package org.thingsboard.server.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;

import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseEntityViewControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;
    private Device testDevice;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        testDevice = doPost("/api/device", device, Device.class);

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
    public void testSaveEntityViewWithIdOfDevice() throws Exception {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setName("Test entity view");
        view.setKeys(Arrays.asList("key1", "key2", "key3"));
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

        EntityView foundEntityView = doGet("/api/device/"
                + savedView.getId().getId().toString(), EntityView.class);

        Assert.assertEquals(foundEntityView.getName(), savedView.getName());
    }
}
