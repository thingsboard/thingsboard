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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class EdgeServiceTest extends AbstractServiceTest {

    @Autowired
    TbTenantProfileCache tbTenantProfileCache;
    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    CustomerService customerService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    RuleChainService ruleChainService;

    private final IdComparator<Edge> idComparator = new IdComparator<>();

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfiles(tenantId);
    }

    @Test
    public void testSaveEdge() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(edge.getTenantId(), savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        savedEdge.setName("My new edge");

        edgeService.saveEdge(savedEdge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testSaveEdgeWithEmptyName() {
        Edge edge = new Edge();
        edge.setType("default");
        edge.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgeWithEmptyTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgeWithInvalidTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edge.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(edge);
        });
    }

    @Test
    public void testSaveEdgesWithInfiniteMaxEdgeLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxEdges(Long.MAX_VALUE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Edge savedEdge = edgeService.saveEdge(constructEdge("My edge", "default"));
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testSaveEdgesWithMaxEdgeOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxEdges(1).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);
        tbTenantProfileCache.evict(defaultTenantProfile.getId());

        Assert.assertEquals(0, edgeService.countByTenantId(tenantId));

        Edge savedEdge = edgeService.saveEdge(constructEdge("My first edge", "default"));
        Assert.assertEquals(1, edgeService.countByTenantId(tenantId));

        Assertions.assertThrows(DataValidationException.class, () -> {
            edgeService.saveEdge(constructEdge("My second edge that out of maxEdgeCount limit", "default"));
        });
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testAssignEdgeToNonExistentCustomer() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                edgeService.assignEdgeToCustomer(tenantId, savedEdge.getId(), new CustomerId(Uuids.timeBased()));
            });
        } finally {
            edgeService.deleteEdge(tenantId, savedEdge.getId());
        }
    }

    @Test
    public void testAssignEdgeToCustomerFromDifferentTenant() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                edgeService.assignEdgeToCustomer(tenantId, savedEdge.getId(), savedCustomer.getId());
            });
        } finally {
            edgeService.deleteEdge(tenantId, savedEdge.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindEdgeById() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNotNull(foundEdge);
        Assert.assertEquals(savedEdge, foundEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFindEdgeTypesByTenantId() throws Exception {
        List<Edge> edges = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Edge edge = constructEdge("My edge B" + i, "typeB");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 7; i++) {
                Edge edge = constructEdge("My edge C" + i, "typeC");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 9; i++) {
                Edge edge = constructEdge("My edge A" + i, "typeA");
                edges.add(edgeService.saveEdge(edge));
            }
            List<EntitySubtype> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId).get();
            Assert.assertNotNull(edgeTypes);
            Assert.assertEquals(3, edgeTypes.size());
            Assert.assertEquals("typeA", edgeTypes.get(0).getType());
            Assert.assertEquals("typeB", edgeTypes.get(1).getType());
            Assert.assertEquals("typeC", edgeTypes.get(2).getType());
        } finally {
            edges.forEach((edge) -> {
                edgeService.deleteEdge(tenantId, edge.getId());
            });
        }
    }

    @Test
    public void testDeleteEdge() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNotNull(foundEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
        foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNull(foundEdge);
    }

    @Test
    public void testFindEdgesByTenantId() {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Edge edge = constructEdge(tenantId, "Edge " + i, "default");
            edges.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        Assert.assertEquals(edges, loadedEdges);

        edgeService.deleteEdgesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindEdgesByTenantIdAndName() {
        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edgesTitle2.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
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
            pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        for (Edge edge : loadedEdgesTitle1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = edgeService.findEdgesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindEdgesByTenantIdAndType() {
        String title1 = "Edge title 1";
        String type1 = "typeA";
        List<Edge> edgesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            edgesType1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        String type2 = "typeB";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            edgesType2.add(edgeService.saveEdge(edge));
        }

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0 , title1);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type1, pageLink);
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
            pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type2, pageLink);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType2, idComparator);
        Collections.sort(loadedEdgesType2, idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = edgeService.findEdgesByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindEdgesByTenantIdAndCustomerId() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 278; i++) {
            Edge edge = constructEdge(tenantId, "Edge" + i, "default");
            edge = edgeService.saveEdge(edge);
            edges.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edges, idComparator);
        Collections.sort(loadedEdges, idComparator);

        Assert.assertEquals(edges, loadedEdges);

        edgeService.unassignCustomerEdges(tenantId, customerId);

        pageLink = new PageLink(33);
        pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindEdgesByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edge = edgeService.saveEdge(edge);
            edgesTitle1.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            edge = edgeService.saveEdge(edge);
            edgesTitle2.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
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
            pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        for (Edge edge : loadedEdgesTitle1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testFindEdgesByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Edge title 1";
        String type1 = "typeC";
        List<Edge> edgesType1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            edge = edgeService.saveEdge(edge);
            edgesType1.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }
        String title2 = "Edge title 2";
        String type2 = "typeD";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            edge = edgeService.saveEdge(edge);
            edgesType2.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Edge> pageData = null;
        do {
            pageData = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
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
            pageData = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesType2, idComparator);
        Collections.sort(loadedEdgesType2, idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4);
        pageData = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            edgeService.deleteEdge(tenantId, edge.getId());
        }

        pageLink = new PageLink(4);
        pageData = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    private Edge constructEdge(String name, String type) {
        return constructEdge(tenantId, name, type);
    }

    @Test
    public void testCleanCacheIfEdgeRenamed() {
        String edgeNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String edgeNameAfterRename = StringUtils.randomAlphanumeric(15);

        Edge edge = constructEdge(tenantId, edgeNameBeforeRename, "default");
        edgeService.saveEdge(edge);

        Edge savedEdge = edgeService.findEdgeByTenantIdAndName(tenantId, edgeNameBeforeRename);

        savedEdge.setName(edgeNameAfterRename);
        edgeService.saveEdge(savedEdge);

        Edge renamedEdge = edgeService.findEdgeByTenantIdAndName(tenantId, edgeNameBeforeRename);

        Assert.assertNull("Can't find edge by name in cache if it was renamed", renamedEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    @Test
    public void testFindMissingToRelatedRuleChains() {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #1");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain1 = ruleChainService.saveRuleChain(ruleChain);

        ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #2");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain2 = ruleChainService.saveRuleChain(ruleChain);

        ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Rule Chain #3");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain ruleChain3 = ruleChainService.saveRuleChain(ruleChain);

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Input rule node 1");
        ruleNode1.setType("org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("ruleChainId", ruleChain1.getUuidId().toString());
        ruleNode1.setConfiguration(configuration);

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Input rule node 2");
        ruleNode2.setType("org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        configuration = JacksonUtil.newObjectNode();
        configuration.put("ruleChainId", ruleChain2.getUuidId().toString());
        ruleNode2.setConfiguration(configuration);

        RuleChainMetaData ruleChainMetaData3 = new RuleChainMetaData();
        ruleChainMetaData3.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        ruleChainMetaData3.setFirstNodeIndex(0);
        ruleChainMetaData3.setRuleChainId(ruleChain3.getId());
        ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData3, Function.identity());

        ruleChainService.assignRuleChainToEdge(tenantId, ruleChain3.getId(), savedEdge.getId());

        String missingToRelatedRuleChains = edgeService.findMissingToRelatedRuleChains(tenantId,
                savedEdge.getId(),
                "org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
        Assert.assertEquals("{\"Rule Chain #3\":[\"Rule Chain #1\",\"Rule Chain #2\"]}", missingToRelatedRuleChains);
    }

    @Test
    public void testFindEdgesByTenantProfileId() {
        Tenant tenant1 = createTenant();
        Tenant tenant2 = createTenant();
        Assert.assertNotNull(tenant1);
        Assert.assertNotNull(tenant2);

        Edge edge1 = constructEdge(tenant1.getId(), "Tenant1 edge", "default");
        Edge edge2 = constructEdge(tenant2.getId(), "Tenant2 edge", "default");
        Edge savedEdge1 = edgeService.saveEdge(edge1);
        Edge savedEdge2 = edgeService.saveEdge(edge2);
        Assert.assertNotNull(savedEdge1);
        Assert.assertNotNull(savedEdge2);
        Assert.assertEquals(tenant1.getTenantProfileId(), tenant2.getTenantProfileId());

        PageData<Edge> edgesPageData = edgeService.findEdgesByTenantProfileId(tenant2.getTenantProfileId(),
                new PageLink(1000));
        Assert.assertEquals(2, edgesPageData.getTotalElements());
        tenantService.deleteTenant(tenant1.getId());
        tenantService.deleteTenant(tenant2.getId());
    }

}
