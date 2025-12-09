/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.edge.AbstractEdgeTest.CONNECT_MESSAGE_COUNT;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
@ContextConfiguration(classes = {EdgeControllerTest.Config.class})
@DaoSqlTest
@Slf4j
public class EdgeControllerTest extends AbstractControllerTest {

    public static final String EDGE_HOST = "localhost";
    public static final int EDGE_PORT = 7070;

    private IdComparator<Edge> idComparator = new IdComparator<>();

    ListeningExecutorService executor;

    List<ListenableFuture<Edge>> futures;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private EdgeUpgradeInstructionsService edgeUpgradeInstructionsService;

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
    public void teardownEdgeTest() {
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

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, 2);

        savedEdge.setName("My new edge");
        doPost("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UPDATED, 1);
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
        int cntEntity = 3;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("My edge B" + i, "typeB");
            doPost("/api/edge", edge, Edge.class);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Edge(), new Edge(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, cntEntity, 0);

        for (int i = 0; i < 7; i++) {
            Edge edge = constructEdge("My edge C" + i, "typeC");
            doPost("/api/edge", edge, Edge.class);
        }
        for (int i = 0; i < 9; i++) {
            Edge edge = constructEdge("My edge A" + i, "typeA");
            doPost("/api/edge", edge, Edge.class);
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

        testNotifyEntityBroadcastEntityStateChangeEventManyTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.DELETED, 1, savedEdge.getId().getId().toString());

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
        Tenant savedTenant2 = saveTenant(tenant2);
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

        deleteTenant(savedTenant2.getId());
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
        PageData<Edge> pageData;
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
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<>() {
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
                    new TypeReference<>() {
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
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                    new TypeReference<>() {
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
                    new TypeReference<>() {
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
                new TypeReference<>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                new TypeReference<>() {
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
                ActionType.ASSIGNED_TO_CUSTOMER, cntEntity, cntEntity, cntEntity * 2, "", "", "");

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
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
                    new TypeReference<>() {
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
                    new TypeReference<>() {
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
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                new TypeReference<>() {
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
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                    new TypeReference<>() {
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
                    new TypeReference<>() {
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
                new TypeReference<>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                new TypeReference<>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testSyncEdge() throws Exception {
        loginSysAdmin();
        // get jwt settings from yaml config
        JwtSettings settings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        // save jwt settings into db
        doPost("/api/admin/jwtSettings", settings).andExpect(status().isOk());
        loginTenantAdmin();

        Edge edge = doPost("/api/edge", constructEdge("Test Sync Edge", "test"), Edge.class);

        Asset asset = new Asset();
        asset.setName("Test Sync Edge Asset 1");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Device device = new Device();
        device.setName("Test Sync Edge Device 1");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        // create public customer
        //1 message
        // Customer
        doPost("/api/customer/public/device/" + savedDevice.getId().getId(), Device.class);
        doDelete("/api/customer/device/" + savedDevice.getId().getId(), Device.class);

        simulateEdgeActivation(edge);

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        EdgeImitator edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);

        // 17 connect message
        // + 1 Customer
        // + 5 fetchers messages (DeviceProfile, Device, DeviceCredentials, AssetProfile, Asset) in sync process
        // + 5 queue messages the same
        edgeImitator.expectMessageAmount(CONNECT_MESSAGE_COUNT + 11);
        edgeImitator.connect();
        edgeImitator.waitForMessages();

        verifyFetchersMsgs(edgeImitator, savedDevice);
        // verify queue msgs
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1"));
        Assert.assertTrue(popDeviceCredentialsMsg(edgeImitator.getDownlinkMsgs(), savedDevice.getId()));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1"));
        printQueueMsgsIfNotEmpty(edgeImitator);

        // 17 connect messages
        // + 1 Customer
        // + 5 fetchers messages (DeviceProfile, Device, DeviceCredentials, AssetProfile, Asset) in sync process
        edgeImitator.expectMessageAmount(CONNECT_MESSAGE_COUNT + 6);
        doPost("/api/edge/sync/" + edge.getId()).andExpect(status().isOk());
        edgeImitator.waitForMessages();

        verifyFetchersMsgs(edgeImitator, savedDevice);
        printQueueMsgsIfNotEmpty(edgeImitator);

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

    private static void printQueueMsgsIfNotEmpty(EdgeImitator edgeImitator) {
        if (!edgeImitator.getDownlinkMsgs().isEmpty()) {
            for (AbstractMessage downlinkMsg : edgeImitator.getDownlinkMsgs()) {
                log.warn("Unexpected message in the queue: {}", downlinkMsg);
            }
        }
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().isEmpty());
    }

    private RuleChainId getEdgeRootRuleChainId(EdgeImitator edgeImitator) {
        try {
            EdgeId edgeId = new EdgeId(new UUID(edgeImitator.getConfiguration().getEdgeIdMSB(), edgeImitator.getConfiguration().getEdgeIdLSB()));
            List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edgeId.getId() + "/ruleChains?",
                    new TypeReference<PageData<RuleChain>>() {
                    }, new PageLink(100)).getData();
            for (RuleChain edgeRuleChain : edgeRuleChains) {
                if (edgeRuleChain.isRoot()) {
                    return edgeRuleChain.getId();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Root rule chain not found");
    }

    private void simulateEdgeActivation(Edge edge) throws Exception {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<RuleChain> ruleChains = getEdgeRuleChains(edge.getId());
                    return ruleChains.size() == 1 && ruleChains.get(0).getId().equals(edge.getRootRuleChainId());
                });

        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + AttributeScope.SERVER_SCOPE, attributes);
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> values = doGetAsyncTyped("/api/plugins/telemetry/EDGE/" + edge.getId() +
                            "/values/attributes/SERVER_SCOPE", new TypeReference<>() {});
                    Optional<Map<String, Object>> activeAttrOpt = values.stream().filter(att -> att.get("key").equals("active")).findFirst();
                    if (activeAttrOpt.isEmpty()) {
                        return false;
                    }
                    List<RuleChain> ruleChains = getEdgeRuleChains(edge.getId());
                    Map<String, Object> activeAttr = activeAttrOpt.get();
                    return "true".equals(activeAttr.get("value").toString()) && ruleChains.size() == 1;
                });
    }

    private void verifyFetchersMsgs(EdgeImitator edgeImitator, Device savedDevice) {
        Assert.assertTrue(popQueueMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Main"));
        Assert.assertTrue(popRuleChainMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Root Rule Chain"));
        Assert.assertTrue(popRuleChainMetadataMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, getEdgeRootRuleChainId(edgeImitator)));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "general"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "mail"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "connectivity"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "jwt"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popUserCredentialsMsg(edgeImitator.getDownlinkMsgs(), currentUserId));
        Assert.assertTrue(popUserMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, TENANT_ADMIN_EMAIL, Authority.TENANT_ADMIN));
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1"));
        Assert.assertTrue(popDeviceCredentialsMsg(edgeImitator.getDownlinkMsgs(), savedDevice.getId()));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1"));
        Assert.assertTrue(popTenantMsg(edgeImitator.getDownlinkMsgs(), tenantId));
        Assert.assertTrue(popTenantProfileMsg(edgeImitator.getDownlinkMsgs(), tenantProfileId));
        Assert.assertTrue(popSyncCompletedMsg(edgeImitator.getDownlinkMsgs()));
    }

    private boolean popQueueMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof QueueUpdateMsg queueUpdateMsg) {
                Queue queue = JacksonUtil.fromString(queueUpdateMsg.getEntity(), Queue.class, true);
                Assert.assertNotNull(queue);
                if (msgType.equals(queueUpdateMsg.getMsgType()) && name.equals(queue.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popRuleChainMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof RuleChainUpdateMsg ruleChainUpdateMsg) {
                RuleChain ruleChain = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
                Assert.assertNotNull(ruleChain);
                if (msgType.equals(ruleChainUpdateMsg.getMsgType())
                        && name.equals(ruleChain.getName())
                        && ruleChain.isRoot()) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popRuleChainMetadataMsg(List<AbstractMessage> messages, UpdateMsgType msgType, RuleChainId ruleChainId) {
        for (AbstractMessage message : messages) {
            if (message instanceof RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
                RuleChainMetaData ruleChainMetaData = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
                Assert.assertNotNull(ruleChainMetaData);
                if (msgType.equals(ruleChainMetadataUpdateMsg.getMsgType())
                        && ruleChainId.equals(ruleChainMetaData.getRuleChainId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAdminSettingsMsg(List<AbstractMessage> messages, String key) {
        for (AbstractMessage message : messages) {
            if (message instanceof AdminSettingsUpdateMsg adminSettingsUpdateMsg) {
                AdminSettings adminSettings = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
                Assert.assertNotNull(adminSettings);
                if (key.equals(adminSettings.getKey())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popDeviceProfileMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
                DeviceProfile deviceProfile = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
                Assert.assertNotNull(deviceProfile);
                if (msgType.equals(deviceProfileUpdateMsg.getMsgType())
                        && name.equals(deviceProfile.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popDeviceMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof DeviceUpdateMsg deviceUpdateMsg) {
                Device device = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
                Assert.assertNotNull(device);
                if (msgType.equals(deviceUpdateMsg.getMsgType())
                        && name.equals(device.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popDeviceCredentialsMsg(List<AbstractMessage> messages, DeviceId deviceId) {
        for (AbstractMessage message : messages) {
            if (message instanceof DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
                DeviceCredentials deviceCredentials = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
                Assert.assertNotNull(deviceCredentials);
                if (deviceId.equals(deviceCredentials.getDeviceId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAssetProfileMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof AssetProfileUpdateMsg assetProfileUpdateMsg) {
                AssetProfile assetProfile = JacksonUtil.fromString(assetProfileUpdateMsg.getEntity(), AssetProfile.class, true);
                Assert.assertNotNull(assetProfile);
                if (msgType.equals(assetProfileUpdateMsg.getMsgType())
                        && name.equals(assetProfile.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAssetMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof AssetUpdateMsg assetUpdateMsg) {
                Asset asset = JacksonUtil.fromString(assetUpdateMsg.getEntity(), Asset.class, true);
                Assert.assertNotNull(asset);
                if (msgType.equals(assetUpdateMsg.getMsgType())
                        && name.equals(asset.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popUserCredentialsMsg(List<AbstractMessage> messages, UserId userId) {
        for (AbstractMessage message : messages) {
            if (message instanceof UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
                UserCredentials userCredentials = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
                Assert.assertNotNull(userCredentials);
                if (userId.equals(userCredentials.getUserId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popUserMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String email, Authority authority) {
        for (AbstractMessage message : messages) {
            if (message instanceof UserUpdateMsg userUpdateMsg) {
                User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
                Assert.assertNotNull(user);
                if (msgType.equals(userUpdateMsg.getMsgType())
                        && email.equals(user.getEmail())
                        && authority.equals(user.getAuthority())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popCustomerMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String title) {
        for (AbstractMessage message : messages) {
            if (message instanceof CustomerUpdateMsg customerUpdateMsg) {
                Customer customer = JacksonUtil.fromString(customerUpdateMsg.getEntity(), Customer.class, true);
                Assert.assertNotNull(customer);
                if (msgType.equals(customerUpdateMsg.getMsgType())
                        && title.equals(customer.getTitle())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popTenantMsg(List<AbstractMessage> messages, TenantId tenantId1) {
        for (AbstractMessage message : messages) {
            if (message instanceof TenantUpdateMsg tenantUpdateMsg) {
                Tenant tenant = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
                Assert.assertNotNull(tenant);
                if (UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(tenantUpdateMsg.getMsgType())
                        && tenantId1.equals(tenant.getId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popTenantProfileMsg(List<AbstractMessage> messages, TenantProfileId tenantProfileId) {
        for (AbstractMessage message : messages) {
            if (message instanceof TenantProfileUpdateMsg tenantProfileUpdateMsg) {
                TenantProfile tenantProfile = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
                Assert.assertNotNull(tenantProfile);
                if (UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(tenantProfileUpdateMsg.getMsgType())
                        && tenantProfileId.equals(tenantProfile.getId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popSyncCompletedMsg(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            if (message instanceof SyncCompletedMsg) {
                messages.remove(message);
                return true;
            }
        }
        return false;
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

    private List<RuleChain> getEdgeRuleChains(EdgeId edgeId) throws Exception {
        return doGetTypedWithTimePageLink("/api/edge/" + edgeId + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {
                }, new TimePageLink(10)).getData();
    }

    @Test
    public void testGetEdgeInstallInstructions() throws Exception {
        Edge edge = constructEdge(tenantId, "Edge for Test Docker Install Instructions", "default", "7390c3a6-69b0-9910-d155-b90aca4b772e", "l7q4zsjplzwhk16geqxy");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        String installInstructions = doGet("/api/edge/instructions/install/" + savedEdge.getId().getId().toString() + "/docker", String.class);
        Assert.assertTrue(installInstructions.contains("l7q4zsjplzwhk16geqxy"));
        Assert.assertTrue(installInstructions.contains("7390c3a6-69b0-9910-d155-b90aca4b772e"));
    }

    @Test
    public void testGetEdgeUpgradeInstructions() throws Exception {
        // UpdateInfo config is updating from the Thingsboard Update server
        HashMap<String, EdgeUpgradeInfo> upgradeInfoHashMap = new HashMap<>();
        upgradeInfoHashMap.put("3.6.0", new EdgeUpgradeInfo(true, "3.6.1"));
        upgradeInfoHashMap.put("3.6.1", new EdgeUpgradeInfo(true, "3.6.2"));
        upgradeInfoHashMap.put("3.6.2", new EdgeUpgradeInfo(true, null));
        edgeUpgradeInstructionsService.updateInstructionMap(upgradeInfoHashMap);
        Edge edge = constructEdge("Edge for Test Docker Upgrade Instructions", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        String body = "{\"edgeVersion\": \"V_3_6_0\"}";
        doPostAsync("/api/plugins/telemetry/EDGE/" + savedEdge.getId().getId() + "/attributes/SERVER_SCOPE", body, String.class, status().isOk());
        String upgradeInstructions = doGet("/api/edge/instructions/upgrade/" + EdgeVersion.V_3_6_0.name() + "/docker", String.class);
        Assert.assertTrue(upgradeInstructions.contains("Upgrading to 3.6.1EDGE"));
        Assert.assertTrue(upgradeInstructions.contains("Upgrading to 3.6.2EDGE"));
    }

    @Test
    public void testIsEdgeUpgradeAvailable() throws Exception {
        Edge edge = constructEdge("Edge Upgrade Available", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        // Test 3.5.0 Edge - upgrade not available
        String body = "{\"edgeVersion\": \"V_3_5_0\"}";
        doPostAsync("/api/plugins/telemetry/EDGE/" + savedEdge.getId().getId() + "/attributes/SERVER_SCOPE", body, String.class, status().isOk());
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.0");
        Assert.assertFalse(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.2");
        Assert.assertFalse(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.2.7");
        Assert.assertFalse(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));

        // Test 3.6.0 Edge - upgrade available
        body = "{\"edgeVersion\": \"V_3_6_0\"}";
        doPostAsync("/api/plugins/telemetry/EDGE/" + savedEdge.getId().getId() + "/attributes/SERVER_SCOPE", body, String.class, status().isOk());
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.0");
        Assert.assertFalse(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.1.5");
        Assert.assertTrue(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.2");
        Assert.assertTrue(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));

        // Test 3.6.1 Edge - upgrade available
        body = "{\"edgeVersion\": \"V_3_6_1\"}";
        doPostAsync("/api/plugins/telemetry/EDGE/" + savedEdge.getId().getId() + "/attributes/SERVER_SCOPE", body, String.class, status().isOk());
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.1");
        Assert.assertFalse(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.2");
        Assert.assertTrue(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
        edgeUpgradeInstructionsService.setPlatformEdgeVersion("3.6.2.6");
        Assert.assertTrue(edgeUpgradeInstructionsService.isUpgradeAvailable(savedEdge.getTenantId(), savedEdge.getId()));
    }

}
