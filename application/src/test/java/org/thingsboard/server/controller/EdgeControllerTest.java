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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.ThingsBoardExecutors;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@TestPropertySource(properties = {
        "edges.enabled=true",
})
@ContextConfiguration(classes = {EdgeControllerTest.Config.class})
@DaoSqlTest
public class EdgeControllerTest extends AbstractControllerTest {

    public static final String EDGE_HOST = "localhost";
    public static final int EDGE_PORT = 7070;

    private IdComparator<Edge> idComparator = new IdComparator<>();

    ListeningExecutorService executor;

    List<ListenableFuture<Edge>> futures;

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
    public void setupEdgeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
        loginTenantAdmin();
    }

    @After
    public void teardownEdgeTest() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testSaveEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED);

        savedEdge.setName("My new edge");
        doPost("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
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

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("type");
        edge.setName("normal name");
        edge.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("label");
        edge.setType("normal type");
        edge.setLabel(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
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
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
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
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
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

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveEdgeWithEmptyName() throws Exception {
        Edge edge = constructEdge(null, "default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Edge name " + msgErrorShouldBeSpecified;
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
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
                tenantId, savedCustomer.getId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ASSIGNED_TO_CUSTOMER,
                ActionType.ASSIGNED_TO_CUSTOMER, assignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(savedCustomer.getId(), foundEdge.getCustomerId());

        Edge unassignedEdge =
                doDelete("/api/customer/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedEdge.getCustomerId().getId());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(unassignedEdge, unassignedEdge.getId(), unassignedEdge.getId(),
                tenantId, savedCustomer.getId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.UNASSIGNED_FROM_CUSTOMER,
                ActionType.UNASSIGNED_FROM_CUSTOMER, unassignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

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
        doPost("/api/customer/" + customerIdStr + "/edge/" + savedEdge.getId().getId().toString())
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

        loginTenantAdmin();

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
        int cntEntity = 178;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edges = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));
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
        int cntEntity = 143;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesTitle1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        String title2 = "Edge title 2";
        cntEntity = 75;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesTitle2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

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
        int cntEntity = 143;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesType1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        String title2 = "Edge title 2";
        String type2 = "typeB";
        cntEntity = 75;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesType2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

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

        int cntEntity = 128;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edges = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Edge(), new Edge(),
                tenantId, customerId, tenantAdminUser.getId(), tenantAdminUser.getEmail(),
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

        int cntEntity = 125;
        String title1 = "Edge title 1";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesTitle1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        cntEntity = 143;
        String title2 = "Edge title 2";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesTitle2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

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

        cntEntity = loadedEdgesTitle1.size();
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Edge(), new Edge(),
                tenantId, customerId, tenantAdminUser.getId(), tenantAdminUser.getEmail(),
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

        int cntEntity = 125;
        String title1 = "Edge title 1";
        String type1 = "typeC";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesType1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        cntEntity = 143;
        String title2 = "Edge title 2";
        String type2 = "typeD";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesType2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

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

        edgeImitator.expectMessageAmount(21);
        edgeImitator.connect();
        assertThat(edgeImitator.waitForMessages()).as("await for messages on first connect").isTrue();

        verifyFetchersMsgs(edgeImitator);
        // verify queue msgs
        Assert.assertTrue(findRuleChainMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, "Edge Root Rule Chain"));
        verifyDeviceProfileMsg(edgeImitator.getDownlinkMsgs().get(16), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default");
        verifyDeviceMsg(edgeImitator.getDownlinkMsgs().get(17), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1");
        verifyAssetProfileMsg(edgeImitator.getDownlinkMsgs().get(18), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test");
        verifyAssetProfileMsg(edgeImitator.getDownlinkMsgs().get(19), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test");
        verifyAssetMsg(edgeImitator.getDownlinkMsgs().get(20), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1");

        edgeImitator.expectMessageAmount(15);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        verifyFetchersMsgs(edgeImitator);

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

    private void verifyFetchersMsgs(EdgeImitator edgeImitator) {
        verifyQueueMsg(edgeImitator.getDownlinkMsgs().get(0), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Main");
        Assert.assertTrue(findRuleChainMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Root Rule Chain"));
        verifyAdminSettingsMsg(edgeImitator.getDownlinkMsgs().get(2), "mail", true);
        verifyAdminSettingsMsg(edgeImitator.getDownlinkMsgs().get(3), "mail", false);
        verifyAdminSettingsMsg(edgeImitator.getDownlinkMsgs().get(4), "mailTemplates", true);
        verifyAdminSettingsMsg(edgeImitator.getDownlinkMsgs().get(5), "mailTemplates", false);
        verifyDeviceProfileMsg(edgeImitator.getDownlinkMsgs().get(6), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default");
        verifyAssetProfileMsg(edgeImitator.getDownlinkMsgs().get(7), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default");
        verifyAssetProfileMsg(edgeImitator.getDownlinkMsgs().get(8), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test");
        verifyUserMsg(edgeImitator.getDownlinkMsgs().get(9), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, TENANT_ADMIN_EMAIL, Authority.TENANT_ADMIN);
        verifyCustomerMsg(edgeImitator.getDownlinkMsgs().get(10), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public");
        verifyDeviceProfileMsg(edgeImitator.getDownlinkMsgs().get(11), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default");
        verifyDeviceMsg(edgeImitator.getDownlinkMsgs().get(12), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1");
        verifyAssetProfileMsg(edgeImitator.getDownlinkMsgs().get(13), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test");
        verifyAssetMsg(edgeImitator.getDownlinkMsgs().get(14), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1");
    }

    private void verifyQueueMsg(AbstractMessage message, UpdateMsgType msgType, String name) {
        QueueUpdateMsg queueUpdateMsg = (QueueUpdateMsg) message;
        Assert.assertEquals(msgType, queueUpdateMsg.getMsgType());
        Assert.assertEquals(name, queueUpdateMsg.getName());
    }

    private boolean findRuleChainMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof RuleChainUpdateMsg) {
                RuleChainUpdateMsg ruleChainUpdateMsg = (RuleChainUpdateMsg) message;
                if (msgType.equals(ruleChainUpdateMsg.getMsgType())
                        && name.equals(ruleChainUpdateMsg.getName())
                        && ruleChainUpdateMsg.getRoot()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void verifyAdminSettingsMsg(AbstractMessage message, String key, boolean isSystem) {
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) message;
        Assert.assertEquals(key, adminSettingsUpdateMsg.getKey());
        Assert.assertEquals(isSystem, adminSettingsUpdateMsg.getIsSystem());
    }

    private void verifyDeviceProfileMsg(AbstractMessage message, UpdateMsgType msgType, String name) {
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) message;
        Assert.assertEquals(msgType, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(name, deviceProfileUpdateMsg.getName());
    }

    private void verifyDeviceMsg(AbstractMessage message, UpdateMsgType msgType, String name) {
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) message;
        Assert.assertEquals(msgType, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(name, deviceUpdateMsg.getName());
    }

    private void verifyAssetProfileMsg(AbstractMessage message, UpdateMsgType msgType, String name) {
        AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) message;
        Assert.assertEquals(msgType, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(name, assetProfileUpdateMsg.getName());
    }

    private void verifyAssetMsg(AbstractMessage message, UpdateMsgType msgType, String name) {
        AssetUpdateMsg assetUpdateMsg = (AssetUpdateMsg) message;
        Assert.assertEquals(msgType, assetUpdateMsg.getMsgType());
        Assert.assertEquals(name, assetUpdateMsg.getName());
    }

    private void verifyUserMsg(AbstractMessage message, UpdateMsgType msgType, String email, Authority authority) {
        UserUpdateMsg userUpdateMsg = (UserUpdateMsg) message;
        Assert.assertEquals(msgType, userUpdateMsg.getMsgType());
        Assert.assertEquals(email, userUpdateMsg.getEmail());
        Assert.assertEquals(authority.name(), userUpdateMsg.getAuthority());
    }

    private void verifyCustomerMsg(AbstractMessage message, UpdateMsgType msgType, String title) {
        CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) message;
        Assert.assertEquals(msgType, customerUpdateMsg.getMsgType());
        Assert.assertEquals(title, customerUpdateMsg.getTitle());
    }

    @Test
    public void testDeleteEdgeWithDeleteRelationsOk() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(tenantId, edgeId, "/api/edge/" + edgeId);
    }

    @Ignore
    @Test
    public void testDeleteEdgeExceptionWithRelationsTransactional() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(edgeDao, tenantId, edgeId, "/api/edge/" + edgeId);
    }

    private Edge savedEdge(String name) {
        Edge edge = constructEdge(name, "default");
        return doPost("/api/edge", edge, Edge.class);
    }

    @Test
    public void testGetEdgeInstallInstructions() throws Exception {
        Edge edge = constructEdge(tenantId, "Edge for Test Docker Install Instructions", "default", "7390c3a6-69b0-9910-d155-b90aca4b772e", "l7q4zsjplzwhk16geqxy");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        String installInstructions = doGet("/api/edge/instructions/" + savedEdge.getId().getId().toString(), String.class);
        Assert.assertTrue(installInstructions.contains("l7q4zsjplzwhk16geqxy"));
        Assert.assertTrue(installInstructions.contains("7390c3a6-69b0-9910-d155-b90aca4b772e"));
    }
}
