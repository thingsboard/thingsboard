/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class DashboardEdgeTest extends AbstractEdgeTest {

    private static final int MOBILE_ORDER = 5;
    private static final String IMAGE = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4Ij48cGF0aCBkPSJNMTMuMjMgMTAuNTZWMTBjLTEuOTQgMC0zLjk5LjM5LTMuOTkgMi42NyAwIDEuMTYuNjEgMS45NSAxLjYzIDEuOTUuNzYgMCAxLjQzLS40NyAxLjg2LTEuMjIuNTItLjkzLjUtMS44LjUtMi44NG0yLjcgNi41M2MtLjE4LjE2LS40My4xNy0uNjMuMDYtLjg5LS43NC0xLjA1LTEuMDgtMS41NC0xLjc5LTEuNDcgMS41LTIuNTEgMS45NS00LjQyIDEuOTUtMi4yNSAwLTQuMDEtMS4zOS00LjAxLTQuMTcgMC0yLjE4IDEuMTctMy42NCAyLjg2LTQuMzggMS40Ni0uNjQgMy40OS0uNzYgNS4wNC0uOTNWNy41YzAtLjY2LjA1LTEuNDEtLjMzLTEuOTYtLjMyLS40OS0uOTUtLjctMS41LS43LTEuMDIgMC0xLjkzLjUzLTIuMTUgMS42MS0uMDUuMjQtLjI1LjQ4LS40Ny40OWwtMi42LS4yOGMtLjIyLS4wNS0uNDYtLjIyLS40LS41Ni42LTMuMTUgMy40NS00LjEgNi00LjEgMS4zIDAgMyAuMzUgNC4wMyAxLjMzQzE3LjExIDQuNTUgMTcgNi4xOCAxNyA3Ljk1djQuMTdjMCAxLjI1LjUgMS44MSAxIDIuNDguMTcuMjUuMjEuNTQgMCAuNzFsLTIuMDYgMS43OGgtLjAxIj48L3BhdGg+PHBhdGggZD0iTTIwLjE2IDE5LjU0QzE4IDIxLjE0IDE0LjgyIDIyIDEyLjEgMjJjLTMuODEgMC03LjI1LTEuNDEtOS44NS0zLjc2LS4yLS4xOC0uMDItLjQzLjI1LS4yOSAyLjc4IDEuNjMgNi4yNSAyLjYxIDkuODMgMi42MSAyLjQxIDAgNS4wNy0uNSA3LjUxLTEuNTMuMzctLjE2LjY2LjI0LjMyLjUxIj48L3BhdGg+PHBhdGggZD0iTTIxLjA3IDE4LjVjLS4yOC0uMzYtMS44NS0uMTctMi41Ny0uMDgtLjE5LjAyLS4yMi0uMTYtLjAzLS4zIDEuMjQtLjg4IDMuMjktLjYyIDMuNTMtLjMzLjI0LjMtLjA3IDIuMzUtMS4yNCAzLjMyLS4xOC4xNi0uMzUuMDctLjI2LS4xMS4yNi0uNjcuODUtMi4xNC41Ny0yLjV6Ij48L3BhdGg+PC9zdmc+";

    private static final String DASHBOARD_TITLE = "Edge Test Dashboard";

    @Test
    public void testDashboards() throws Exception {
        // create dashboard and assign to edge
        edgeImitator.expectMessageAmount(2);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(DASHBOARD_TITLE);
        dashboard.setMobileHide(true);
        dashboard.setImage(IMAGE);
        dashboard.setMobileOrder(MOBILE_ORDER);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DashboardUpdateMsg> dashboardUpdateMsgOpt = edgeImitator.findMessageByType(DashboardUpdateMsg.class);
        Assert.assertTrue(dashboardUpdateMsgOpt.isPresent());
        DashboardUpdateMsg dashboardUpdateMsg = dashboardUpdateMsgOpt.get();
        Dashboard dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard, dashboardMsg);
        Assert.assertEquals("tb-image;/api/images/tenant/edge_test_dashboard_dashboard_image.svg", dashboardMsg.getImage());
        Assert.assertEquals(MOBILE_ORDER, dashboardMsg.getMobileOrder().intValue());
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);

        Optional<ResourceUpdateMsg> resourceUpdateMsg = edgeImitator.findMessageByType(ResourceUpdateMsg.class);
        Assert.assertTrue(resourceUpdateMsg.isPresent());

        // update dashboard
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Updated Edge Test Dashboard");
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardMsg.getTitle());

        // unassign dashboard from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());

        // delete dashboard - message expected, it was sent to all edges
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));

        // create dashboard #2 and assign to edge
        edgeImitator.expectMessageAmount(1);
        dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard #2");
        savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardMsg.getTitle());

        // assign dashboard #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertNotNull(dashboardMsg.getAssignedCustomers());
        Assert.assertFalse(dashboardMsg.getAssignedCustomers().isEmpty());
        Assert.assertTrue(dashboardMsg.getAssignedCustomers().contains(new ShortCustomerInfo(savedCustomer.getId(), customer.getTitle(), customer.isPublic())));

        // unassign dashboard #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertNotNull(dashboardMsg.getAssignedCustomers());
        Assert.assertTrue(dashboardMsg.getAssignedCustomers().isEmpty());

        // delete dashboard #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
    }

    @Test
    public void testSendDashboardToCloud() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        // assign edge to customer
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId() + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<EdgeConfiguration> edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        EdgeConfiguration edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), edgeConfiguration.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), edgeConfiguration.getCustomerIdLSB());
        Optional<CustomerUpdateMsg> customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        CustomerUpdateMsg customerUpdateMsg = customerUpdateOpt.get();
        Customer customerMsg = JacksonUtil.fromString(customerUpdateMsg.getEntity(), Customer.class, true);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer, customerMsg);

        Dashboard dashboard = buildDashboardForUplinkMsg(savedCustomer);

        // create dashboard on edge
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardUpdateMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardUpdateMsgBuilder.setIdMSB(dashboard.getUuidId().getMostSignificantBits());
        dashboardUpdateMsgBuilder.setIdLSB(dashboard.getUuidId().getLeastSignificantBits());
        dashboardUpdateMsgBuilder.setEntity(JacksonUtil.toString(dashboard));
        dashboardUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsgBuilder);
        uplinkMsgBuilder.addDashboardUpdateMsg(dashboardUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        Dashboard foundDashboard = doGet("/api/dashboard/" + dashboard.getUuidId(), Dashboard.class);
        Assert.assertNotNull(foundDashboard);
        Assert.assertEquals(DASHBOARD_TITLE, foundDashboard.getName());

        PageData<DashboardInfo> pageData = doGetTypedWithPageLink("/api/customer/" + savedCustomer.getId().toString() + "/dashboards?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(DASHBOARD_TITLE, pageData.getData().get(0).getTitle());

        dashboard.setTitle(DASHBOARD_TITLE + " Updated");
        dashboard.setAssignedCustomers(null);
        dashboardUpdateMsgBuilder.setEntity(JacksonUtil.toString(dashboard));
        dashboardUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addDashboardUpdateMsg(dashboardUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        foundDashboard = doGet("/api/dashboard/" + dashboard.getUuidId(), Dashboard.class);
        Assert.assertEquals(DASHBOARD_TITLE + " Updated", foundDashboard.getName());

        // unassign edge from customer
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/customer/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB())));
        customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        customerUpdateMsg = customerUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
    }

    @Test
    public void testSendDeleteDashboardOnEdgeToCloud() throws Exception {
        Dashboard savedDashboard = saveDashboardOnCloudAndVerifyDeliveryToEdge();

        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardDeleteMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        dashboardDeleteMsgBuilder.setIdMSB(savedDashboard.getUuidId().getMostSignificantBits());
        dashboardDeleteMsgBuilder.setIdLSB(savedDashboard.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(dashboardDeleteMsgBuilder);

        upLinkMsgBuilder.addDashboardUpdateMsg(dashboardDeleteMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                doGet("/api/dashboard/info/" + savedDashboard.getUuidId(), DashboardInfo.class, status().isNotFound())
        );
    }

    private Dashboard saveDashboardOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create dashboard and assign to edge
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(StringUtils.randomAlphanumeric(15));
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        edgeImitator.expectMessageAmount(1); // dashboard message
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DashboardUpdateMsg> dashboardUpdateMsgOpt = edgeImitator.findMessageByType(DashboardUpdateMsg.class);
        Assert.assertTrue(dashboardUpdateMsgOpt.isPresent());
        DashboardUpdateMsg dashboardUpdateMsg = dashboardUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        return savedDashboard;
    }

    private Dashboard buildDashboardForUplinkMsg(Customer savedCustomer) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(new DashboardId(UUID.randomUUID()));
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(DASHBOARD_TITLE);
        dashboard.setAssignedCustomers(Sets.newHashSet(new ShortCustomerInfo(savedCustomer.getId(), savedCustomer.getTitle(), savedCustomer.isPublic())));
        return dashboard;
    }

}
