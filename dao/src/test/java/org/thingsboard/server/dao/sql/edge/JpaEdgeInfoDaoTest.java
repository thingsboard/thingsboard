// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.edge.EdgeInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaEdgeInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EdgeInfoDao edgeInfoDao;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private CustomerDao customerDao;

    private List<Edge> edges = new ArrayList<>();

    @After
    public void tearDown() {
        for (Edge edge : edges) {
            edgeDao.removeById(edge.getTenantId(), edge.getUuidId());
        }
        edges.clear();
    }

    @Test
    public void testFindEdgeInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            edges.add(createEdge(tenantId1, null, i));
            edges.add(createEdge(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "EDGE");
        PageData<EdgeInfo> edgeInfos1 = edgeInfoDao.findEdgesByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, edgeInfos1.getData().size());

        PageData<EdgeInfo> edgeInfos2 = edgeInfoDao.findEdgesByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, edgeInfos2.getData().size());
    }

    @Test
    public void testFindEdgeInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            edges.add(createEdge(tenantId1, customer1.getUuidId(), i));
            edges.add(createEdge(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "EDGE", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<EdgeInfo> edgeInfos1 = edgeInfoDao.findEdgesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, edgeInfos1.getData().size());
        edgeInfos1.getData().forEach(edgeInfo -> Assert.assertNotEquals("CUSTOMER_0", edgeInfo.getOwnerName()));

        PageData<EdgeInfo> edgeInfos2 = edgeInfoDao.findEdgesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, edgeInfos2.getData().size());

        PageData<EdgeInfo> edgeInfos3 = edgeInfoDao.findEdgesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, edgeInfos3.getData().size());
    }

    private Edge createEdge(UUID tenantId, UUID customerId, int index) {
        return this.createEdge(tenantId, customerId, null, index);
    }

    private Edge createEdge(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        Edge edge = new Edge();
        edge.setId(new EdgeId(Uuids.timeBased()));
        edge.setTenantId(TenantId.fromUUID(tenantId));
        edge.setCustomerId(new CustomerId(customerId));
        edge.setName("EDGE_" + index);
        edge.setType(type);
        return edgeDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, edge);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

}
