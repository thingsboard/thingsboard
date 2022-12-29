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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
@ContextConfiguration(classes = {BaseEdgeControllerTest.Config.class})
public abstract class BaseEdgeControllerTest extends AbstractControllerTest {

    public static final String EDGE_HOST = "localhost";
    public static final int EDGE_PORT = 7070;

    private IdComparator<Edge> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    @Autowired
    private EdgeDao edgeDao;

    static class Config {
        @Bean
        @Primary
        public EdgeDao edgeDao(EdgeDao edgeDao) {
            return Mockito.mock(EdgeDao.class, AdditionalAnswers.delegatesTo(edgeDao));
        }
    }

   @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant for Edge");
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
    public void testSaveEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedEdge.setName("My new edge");
        doPost("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveEdgeWithViolationOfLengthValidation() throws Exception {
        Edge edge = constructEdge(StringUtils.randomAlphabetic(300), "default");
        String msgError = msgErrorFieldLength("name");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("type");
        edge.setName("normal name");
        edge.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("label");
        edge.setType("normal type");
        edge.setLabel(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindEdgeById() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertNotNull(foundEdge);
        Assert.assertEquals(savedEdge, foundEdge);
    }

    @Test
    public void testFindEdgeTypesByTenantId() throws Exception {
        List<Edge> edges = new ArrayList<>();

        int cntEntity = 3;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("My edge B" + i, "typeB");
            edges.add(doPost("/api/edge", edge, Edge.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Edge(), new Edge(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, 0);

        for (int i = 0; i < 7; i++) {
            Edge edge = constructEdge("My edge C" + i, "typeC");
            edges.add(doPost("/api/edge", edge, Edge.class));
        }
        for (int i = 0; i < 9; i++) {
            Edge edge = constructEdge("My edge A" + i, "typeA");
            edges.add(doPost("/api/edge", edge, Edge.class));
        }
        List<EntitySubtype> edgeTypes = doGetTyped("/api/edge/types",
                new TypeReference<>() {
                });

        Assert.assertNotNull(edgeTypes);
        Assert.assertEquals(3, edgeTypes.size());
        Assert.assertEquals("typeA", edgeTypes.get(0).getType());
        Assert.assertEquals("typeB", edgeTypes.get(1).getType());
        Assert.assertEquals("typeC", edgeTypes.get(2).getType());
    }

    @Test
    public void testDeleteEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedEdge.getId().getId().toString());

        doGet("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Edge", savedEdge.getId().getId().toString()))));
    }

    @Test
    public void testSaveEdgeWithEmptyType() throws Exception {
        Edge edge = constructEdge("My edge", null);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Edge type " + msgErrorShouldBeSpecified;
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveEdgeWithEmptyName() throws Exception {
        Edge edge = constructEdge(null, "default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Edge name " + msgErrorShouldBeSpecified;
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testAssignUnassignEdgeToCustomer() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        Edge assignedEdge = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(savedCustomer.getId(), assignedEdge.getCustomerId());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(assignedEdge, assignedEdge.getId(), assignedEdge.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_CUSTOMER,
                assignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(savedCustomer.getId(), foundEdge.getCustomerId());

        Edge unassignedEdge =
                doDelete("/api/customer/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedEdge.getCustomerId().getId());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(unassignedEdge, unassignedEdge.getId(), unassignedEdge.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UNASSIGNED_FROM_CUSTOMER,
                unassignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundEdge.getCustomerId().getId());
    }

    @Test
    public void testAssignEdgeToNonExistentCustomer() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        CustomerId customerId = new CustomerId(Uuids.timeBased());
        String customerIdStr = customerId.getId().toString();

        String msgError = msgErrorNoFound("Customer", customerIdStr);
        doPost("/api/customer/" + customerIdStr+ "/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityNever(savedEdge.getId(), savedEdge);
        testNotifyEntityNever(customerId, new Customer());
    }

    @Test
    public void testAssignEdgeToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedEdge.getId(), savedEdge);
        testNotifyEntityNever(savedCustomer.getId(), savedCustomer);

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantEdges() throws Exception {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            edges.add(doPost("/api/edge", edge, Edge.class));
        }
        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        Assert.assertEquals(edges, loadedEdges);
    }

    @Test
    public void testFindTenantEdgesByName() throws Exception {
        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle1.add(doPost("/api/edge", edge, Edge.class));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle2.add(doPost("/api/edge", edge, Edge.class));
        }

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink);
            loadedEdgesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle1, idComparator);
        Collections.sort(loadedEdgesTitle1, idComparator);

        Assert.assertEquals(edgesTitle1, loadedEdgesTitle1);

        List<Edge> loadedEdgesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        for (Edge edge : loadedEdgesTitle1) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantEdgesByType() throws Exception {
        String title1 = "Edge title 1";
        String type1 = "typeA";
        List<Edge> edgesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            edgesType1.add(doPost("/api/edge", edge, Edge.class));
        }
        String title2 = "Edge title 2";
        String type2 = "typeB";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            edgesType2.add(doPost("/api/edge", edge, Edge.class));
        }

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink, type1);
            loadedEdgesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType1, idComparator);
        Collections.sort(loadedEdgesType1, idComparator);

        Assert.assertEquals(edgesType1, loadedEdgesType1);

        List<Edge> loadedEdgesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink, type2);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType2, idComparator);
        Collections.sort(loadedEdgesType2, idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                new TypeReference<PageData<Edge>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                new TypeReference<PageData<Edge>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerEdges() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        Mockito.reset(tbClusterService, auditLogService);

        List<Edge> edges = new ArrayList<>();
        int cntEntity = 128;
        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            edge = doPost("/api/edge", edge, Edge.class);
            edges.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/edge/" + edge.getId().getId().toString(), Edge.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Edge(), new Edge(),
                savedTenant.getId(), customerId, tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.ASSIGNED_TO_CUSTOMER, cntEntity, cntEntity, cntEntity * 2,
                new String(), new String(), new String());

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        Assert.assertEquals(edges, loadedEdges);
    }

    @Test
    public void testFindCustomerEdgesByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edge = doPost("/api/edge", edge, Edge.class);
            edgesTitle1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/edge/" + edge.getId().getId().toString(), Edge.class));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edge = doPost("/api/edge", edge, Edge.class);
            edgesTitle2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/edge/" + edge.getId().getId().toString(), Edge.class));
        }

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink);
            loadedEdgesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle1, idComparator);
        Collections.sort(loadedEdgesTitle1, idComparator);

        Assert.assertEquals(edgesTitle1, loadedEdgesTitle1);

        List<Edge> loadedEdgesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        Mockito.reset(tbClusterService, auditLogService);

        for (Edge edge : loadedEdgesTitle1) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        int cntEntity = loadedEdgesTitle1.size();
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Edge(), new Edge(),
                savedTenant.getId(), customerId, tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UNASSIGNED_FROM_CUSTOMER, cntEntity, cntEntity, 3);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                new TypeReference<PageData<Edge>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                new TypeReference<PageData<Edge>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerEdgesByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Edge title 1";
        String type1 = "typeC";
        List<Edge> edgesType1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            edge = doPost("/api/edge", edge, Edge.class);
            edgesType1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/edge/" + edge.getId().getId().toString(), Edge.class));
        }
        String title2 = "Edge title 2";
        String type2 = "typeD";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            edge = doPost("/api/edge", edge, Edge.class);
            edgesType2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/edge/" + edge.getId().getId().toString(), Edge.class));
        }

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink, type1);
            loadedEdgesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType1, idComparator);
        Collections.sort(loadedEdgesType1, idComparator);

        Assert.assertEquals(edgesType1, loadedEdgesType1);

        List<Edge> loadedEdgesType2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                    new TypeReference<PageData<Edge>>() {
                    }, pageLink, type2);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType2, idComparator);
        Collections.sort(loadedEdgesType2, idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                new TypeReference<PageData<Edge>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                new TypeReference<PageData<Edge>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testSyncEdge() throws Exception {
        Edge edge = doPost("/api/edge", constructEdge("Test Sync Edge", "test"), Edge.class);

        Device device = new Device();
        device.setName("Test Sync Edge Device 1");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);

        Asset asset = new Asset();
        asset.setName("Test Sync Edge Asset 1");
        asset.setType("test");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        EdgeImitator edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.ignoreType(UserCredentialsUpdateMsg.class);

        edgeImitator.expectMessageAmount(19);
        edgeImitator.connect();
        assertThat(edgeImitator.waitForMessages()).as("await for messages on first connect").isTrue();

        assertThat(edgeImitator.findAllMessagesByType(QueueUpdateMsg.class)).as("one msg during sync process").hasSize(1);
        List<RuleChainUpdateMsg> ruleChainUpdateMsgs = edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class);
        assertThat(ruleChainUpdateMsgs).as("one msg during sync process, another from edge creation").hasSize(2);
        assertThat(edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class)).as("one msg during sync process for 'default' device profile").hasSize(3);
        assertThat(edgeImitator.findAllMessagesByType(DeviceUpdateMsg.class)).as("one msg once device assigned to edge").hasSize(2);
        assertThat(edgeImitator.findAllMessagesByType(AssetProfileUpdateMsg.class)).as("two msgs during sync process for 'default' and 'test' asset profiles").hasSize(4);
        assertThat(edgeImitator.findAllMessagesByType(AssetUpdateMsg.class)).as("two msgs - one during sync process, and one more once asset assigned to edge").hasSize(2);
        assertThat(edgeImitator.findAllMessagesByType(UserUpdateMsg.class)).as("one msg during sync process for tenant admin user").hasSize(1);
        assertThat(edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class)).as("admin setting update").hasSize(4);
        verifyRuleChainMsgsAreRoot(ruleChainUpdateMsgs);

        edgeImitator.expectMessageAmount(14);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        assertThat(edgeImitator.findAllMessagesByType(QueueUpdateMsg.class)).as("queue msg").hasSize(1);
        ruleChainUpdateMsgs = edgeImitator.findAllMessagesByType(RuleChainUpdateMsg.class);
        assertThat(ruleChainUpdateMsgs).as("rule chain msg").hasSize(1);
        assertThat(edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class)).as("device profile msg").hasSize(2);
        assertThat(edgeImitator.findAllMessagesByType(AssetProfileUpdateMsg.class)).as("asset profile msg").hasSize(3);
        assertThat(edgeImitator.findAllMessagesByType(AssetUpdateMsg.class)).as("asset update msg").hasSize(1);
        assertThat(edgeImitator.findAllMessagesByType(UserUpdateMsg.class)).as("user update msg").hasSize(1);
        assertThat(edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class)).as("admin setting update msg").hasSize(4);
        assertThat(edgeImitator.findAllMessagesByType(DeviceUpdateMsg.class)).as("asset update msg").hasSize(1);
        verifyRuleChainMsgsAreRoot(ruleChainUpdateMsgs);

        edgeImitator.allowIgnoredTypes();
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {
        }

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/edge/" + edge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void verifyRuleChainMsgsAreRoot(List<RuleChainUpdateMsg> ruleChainUpdateMsgs) {
        for (RuleChainUpdateMsg ruleChainUpdateMsg : ruleChainUpdateMsgs) {
            Assert.assertTrue(ruleChainUpdateMsg.getRoot());
        }
    }

    @Test
    public void testDeleteEdgeWithDeleteRelationsOk() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), edgeId, "/api/edge/" + edgeId);
    }

    @Test
    public void testDeleteEdgeExceptionWithRelationsTransactional() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(edgeDao, savedTenant.getId(), edgeId, "/api/edge/" + edgeId);
    }

    private Edge savedEdge(String name) {
        Edge edge = constructEdge(name, "default");
        return doPost("/api/edge", edge, Edge.class);
    }
}
