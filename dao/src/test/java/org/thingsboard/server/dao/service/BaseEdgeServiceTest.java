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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseEdgeServiceTest extends AbstractServiceTest {

    private IdComparator<Edge> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveEdge() {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("My edge");
        edge.setType("default");
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

    @Test(expected = DataValidationException.class)
    public void testSaveEdgeWithEmptyName() {
        Edge edge = new Edge();
        edge.setType("default");
        edge.setTenantId(tenantId);
        edgeService.saveEdge(edge);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveEdgeWithEmptyTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edgeService.saveEdge(edge);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveEdgeWithInvalidTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edge.setTenantId(new TenantId(Uuids.timeBased()));
        edgeService.saveEdge(edge);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignEdgeToNonExistentCustomer() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edge.setTenantId(tenantId);
        edge = edgeService.saveEdge(edge);
        try {
            edgeService.assignEdgeToCustomer(tenantId, edge.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            edgeService.deleteEdge(tenantId, edge.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignEdgeToCustomerFromDifferentTenant() {
        Edge edge = new Edge();
        edge.setName("My edge");
        edge.setType("default");
        edge.setTenantId(tenantId);
        edge = edgeService.saveEdge(edge);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customer.getId());
        } finally {
            edgeService.deleteEdge(tenantId, edge.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindEdgeById() {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("My edge");
        edge.setType("default");
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
                Edge edge = new Edge();
                edge.setTenantId(tenantId);
                edge.setName("My edge B" + i);
                edge.setType("typeB");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 7; i++) {
                Edge edge = new Edge();
                edge.setTenantId(tenantId);
                edge.setName("My edge C" + i);
                edge.setType("typeC");
                edges.add(edgeService.saveEdge(edge));
            }
            for (int i = 0; i < 9; i++) {
                Edge edge = new Edge();
                edge.setTenantId(tenantId);
                edge.setName("My edge A" + i);
                edge.setType("typeA");
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
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("My edge");
        edge.setType("default");
        Edge savedEdge = edgeService.saveEdge(edge);
        Edge foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNotNull(foundEdge);
        edgeService.deleteEdge(tenantId, savedEdge.getId());
        foundEdge = edgeService.findEdgeById(tenantId, savedEdge.getId());
        Assert.assertNull(foundEdge);
    }

    @Test
    public void testFindEdgesByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            edge.setName("Edge" + i);
            edge.setType("default");
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

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindEdgesByTenantIdAndName() {
        String title1 = "Edge title 1";
        List<Edge> edgesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType("default");
            edgesTitle1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType("default");
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
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType(type1);
            edgesType1.add(edgeService.saveEdge(edge));
        }
        String title2 = "Edge title 2";
        String type2 = "typeB";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType(type2);
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
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 278; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            edge.setName("Edge" + i);
            edge.setType("default");
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

        tenantService.deleteTenant(tenantId);
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
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType("default");
            edge = edgeService.saveEdge(edge);
            edgesTitle1.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }
        String title2 = "Edge title 2";
        List<Edge> edgesTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType("default");
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
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType(type1);
            edge = edgeService.saveEdge(edge);
            edgesType1.add(edgeService.assignEdgeToCustomer(tenantId, edge.getId(), customerId));
        }
        String title2 = "Edge title 2";
        String type2 = "typeD";
        List<Edge> edgesType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Edge edge = new Edge();
            edge.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            edge.setName(name);
            edge.setType(type2);
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

}