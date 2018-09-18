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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseEntityViewControllerTest extends AbstractControllerTest {

    private IdComparator<EntityView> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;
    private Device testDevice;
    private TelemetryEntityView obj;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        idComparator = new IdComparator<>();

        savedTenant = doPost("/api/tenant", getNewTenant("My tenant"), Tenant.class);
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
                        Arrays.asList("caKey1", "caKey2"),
                        Arrays.asList("saKey1", "saKey2", "saKey3"),
                        Arrays.asList("shKey1", "shKey2", "shKey3", "shKey4")
                )
        );
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindEntityViewById() throws Exception {
        EntityView savedView = doPost("/api/entityView", getNewEntityView("Test entity view"), EntityView.class);
        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertNotNull(foundView);
        Assert.assertEquals(savedView, foundView);
    }

    @Test
    public void testSaveEntityView() throws Exception {
        EntityView savedView = doPost("/api/entityView", getNewEntityView("Test entity view"), EntityView.class);

        Assert.assertNotNull(savedView);
        Assert.assertNotNull(savedView.getId());
        Assert.assertTrue(savedView.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedView.getTenantId());
        Assert.assertNotNull(savedView.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedView.getCustomerId().getId());
        Assert.assertEquals(savedView.getName(), savedView.getName());

        savedView.setName("New test entity view");
        doPost("/api/entityView", savedView, EntityView.class);
        EntityView foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        Assert.assertEquals(foundEntityView.getName(), savedView.getName());
    }

    @Test
    public void testDeleteEntityView() throws Exception {
        EntityView view = getNewEntityView("Test entity view");
        Customer customer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(customer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        doDelete("/api/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveEntityViewWithEmptyName() throws Exception {
        doPost("/api/entityView", new EntityView())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity view name should be specified!")));
    }

    @Test
    public void testAssignAndUnAssignedEntityViewToCustomer() throws Exception {
        EntityView view = getNewEntityView("Test entity view");
        Customer savedCustomer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(savedCustomer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        EntityView assignedView = doPost(
                "/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString(),
                EntityView.class);
        Assert.assertEquals(savedCustomer.getId(), assignedView.getCustomerId());

        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertEquals(savedCustomer.getId(), foundView.getCustomerId());

        EntityView unAssignedView = doDelete("/api/customer/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unAssignedView.getCustomerId().getId());

        foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundView.getCustomerId().getId());
    }

    @Test
    public void testAssignEntityViewToNonExistentCustomer() throws Exception {
        EntityView savedView = doPost("/api/entityView", getNewEntityView("Test entity view"), EntityView.class);
        doPost("/api/customer/" + UUIDs.timeBased().toString() + "/device/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignEntityViewToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = getNewTenant("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");
        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = getNewCustomer("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        EntityView view = getNewEntityView("Test entity view");
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isForbidden());

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetCustomerEntityViews() throws Exception {
        CustomerId customerId = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class).getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViews?";

        List<EntityView> views = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            views.add(doPost("/api/customer/" + customerId.getId().toString() + "/entityView/"
                    + getNewEntityView("Test entity view " + i).getId().getId().toString(), EntityView.class));
        }

        List<EntityView> loadedViews = loadListOf(new TextPageLink(23), urlTemplate);

        Collections.sort(views, idComparator);
        Collections.sort(loadedViews, idComparator);

        Assert.assertEquals(views, loadedViews);
    }

    @Test
    public void testGetCustomerEntityViewsByName() throws Exception {
        CustomerId customerId = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class).getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViews?";

        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = fillListOf(125, name1, "/api/customer/" + customerId.getId().toString()
                + "/entityView/");
        List<EntityView> loadedNamesOfView1 = loadListOf(new TextPageLink(15, name1), urlTemplate);
        Collections.sort(namesOfView1, idComparator);
        Collections.sort(loadedNamesOfView1, idComparator);
        Assert.assertEquals(namesOfView1, loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> NamesOfView2 = fillListOf(143, name2, "/api/customer/" + customerId.getId().toString()
                + "/entityView/");
        List<EntityView> loadedNamesOfView2 = loadListOf(new TextPageLink(4, name2), urlTemplate);
        Collections.sort(NamesOfView2, idComparator);
        Collections.sort(loadedNamesOfView2, idComparator);
        Assert.assertEquals(NamesOfView2, loadedNamesOfView2);

        for (EntityView view : loadedNamesOfView1) {
            doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        TextPageData<EntityView> pageData = doGetTypedWithPageLink(urlTemplate,
                    new TypeReference<TextPageData<EntityView>>(){}, new TextPageLink(4, name1));
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (EntityView view : loadedNamesOfView2) {
            doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<TextPageData<EntityView>>(){},
                new TextPageLink(4, name2));
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testGetTenantEntityViews() throws Exception {

        List<EntityView> views = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            views.add(doPost("/api/entityView/", getNewEntityView("Test entity view" + i), EntityView.class));
        }
        List<EntityView> loadedViews = loadListOf(new TextPageLink(23), "/api/tenant/entityViews?");

        Collections.sort(views, idComparator);
        Collections.sort(loadedViews, idComparator);

        Assert.assertEquals(views, loadedViews);
    }

    @Test
    public void testGetTenantEntityViewsByName() throws Exception {
        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = fillListOf(143, name1);
        List<EntityView> loadedNamesOfView1 = loadListOf(new TextPageLink(15, name1), "/api/tenant/entityViews?");
        Collections.sort(namesOfView1, idComparator);
        Collections.sort(loadedNamesOfView1, idComparator);
        Assert.assertEquals(namesOfView1, loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> NamesOfView2 = fillListOf(75, name2);
        List<EntityView> loadedNamesOfView2 = loadListOf(new TextPageLink(4, name2), "/api/tenant/entityViews?");
        Collections.sort(NamesOfView2, idComparator);
        Collections.sort(loadedNamesOfView2, idComparator);
        Assert.assertEquals(NamesOfView2, loadedNamesOfView2);

        for (EntityView view : loadedNamesOfView1) {
            doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        TextPageData<EntityView> pageData = doGetTypedWithPageLink("/api/tenant/entityViews?",
                new TypeReference<TextPageData<EntityView>>(){}, new TextPageLink(4, name1));
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (EntityView view : loadedNamesOfView2) {
            doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", new TypeReference<TextPageData<EntityView>>(){},
                new TextPageLink(4, name2));
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    private EntityView getNewEntityView(String name) throws Exception {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(savedTenant.getId());
        view.setName(name);
        view.setKeys(new TelemetryEntityView(obj));
        return doPost("/api/entityView", view, EntityView.class);
    }

    private Customer getNewCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return customer;
    }

    private Tenant getNewTenant(String title) {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        return tenant;
    }

    private List<EntityView> fillListOf(int limit, String partOfName, String urlTemplate) throws Exception {
        List<EntityView> views = new ArrayList<>();
        for (EntityView view : fillListOf(limit, partOfName)) {
            views.add(doPost(urlTemplate + view.getId().getId().toString(), EntityView.class));
        }
        return views;
    }

    private List<EntityView> fillListOf(int limit, String partOfName) throws Exception {
        List<EntityView> viewNames = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String fullName = partOfName + ' ' + RandomStringUtils.randomAlphanumeric(15);
            fullName = i % 2 == 0 ? fullName.toLowerCase() : fullName.toUpperCase();
            EntityView view = getNewEntityView(fullName);
            Customer customer = getNewCustomer("Test customer " + String.valueOf(Math.random()));
            view.setCustomerId(doPost("/api/customer", customer, Customer.class).getId());
            viewNames.add(doPost("/api/entityView", view, EntityView.class));
        }
        return viewNames;
    }

    private List<EntityView> loadListOf(TextPageLink pageLink, String urlTemplate) throws Exception {
        List<EntityView> loadedItems = new ArrayList<>();
        TextPageData<EntityView> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<TextPageData<EntityView>>(){}, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }
}
