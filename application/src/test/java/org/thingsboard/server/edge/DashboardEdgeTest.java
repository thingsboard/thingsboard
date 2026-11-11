// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.Collections;
import java.util.List;
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
        // create dashboard entity group and assign to edge
        EntityGroup dashboardEntityGroup1 = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardGroup1", tenantId);

        // create dashboard and add to entity group 1
        edgeImitator.expectMessageAmount(1);
        Dashboard savedDashboard = saveDashboard("Edge Dashboard 1", dashboardEntityGroup1.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Dashboard dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardMsg.getTitle());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(dashboardEntityGroup1.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup1.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());

        // request dashboards by entity group id
        testDashboardEntityGroupRequestMsg(dashboardEntityGroup1.getUuidId().getMostSignificantBits(),
                dashboardEntityGroup1.getUuidId().getLeastSignificantBits(),
                savedDashboard.getId());

        edgeImitator.expectMessageAmount(2);
        savedDashboard.setMobileHide(true);
        savedDashboard.setImage(IMAGE);
        savedDashboard.setMobileOrder(MOBILE_ORDER);
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DashboardUpdateMsg> dashboardUpdateMsgOpt = edgeImitator.findMessageByType(DashboardUpdateMsg.class);
        Assert.assertTrue(dashboardUpdateMsgOpt.isPresent());
        Optional<ResourceUpdateMsg> resourceUpdateMsg = edgeImitator.findMessageByType(ResourceUpdateMsg.class);
        Assert.assertTrue(resourceUpdateMsg.isPresent());

        // add dashboard to entity group 2
        EntityGroup dashboardEntityGroup2 = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardGroup2", tenantId);
        edgeImitator.expectMessageAmount(1);
        addEntitiesToEntityGroup(Collections.singletonList(savedDashboard.getId()), dashboardEntityGroup2.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardMsg.getTitle());
        Assert.assertTrue(dashboardMsg.isMobileHide());
        Assert.assertEquals("tb-image;/api/images/tenant/edge_dashboard_1_dashboard_image.svg", dashboardMsg.getImage());
        Assert.assertEquals(MOBILE_ORDER, dashboardMsg.getMobileOrder().intValue());

        // update dashboard
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Edge Dashboard 1 Updated");
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        dashboardMsg = JacksonUtil.fromString(dashboardUpdateMsg.getEntity(), Dashboard.class, true);
        Assert.assertNotNull(dashboardMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals("Edge Dashboard 1 Updated", dashboardMsg.getTitle());

        // remove dashboard from entity group 2
        edgeImitator.expectMessageAmount(1);
        deleteEntitiesFromEntityGroup(Collections.singletonList(savedDashboard.getId()), dashboardEntityGroup2.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());

        unAssignEntityGroupFromEdge(dashboardEntityGroup2);

        // delete dashboard
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

    private void testDashboardEntityGroupRequestMsg(long msbId, long lsbId, DashboardId expectedDashboardId) throws Exception {
        EntityGroupRequestMsg.Builder entitiesGroupRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
                .setEntityGroupIdMSB(msbId)
                .setEntityGroupIdLSB(lsbId)
                .setType(EntityType.DASHBOARD.name());
        testAutoGeneratedCodeByProtobuf(entitiesGroupRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addEntityGroupEntitiesRequestMsg(entitiesGroupRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        DashboardId receivedDashboardId =
                new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        Assert.assertEquals(expectedDashboardId, receivedDashboardId);

        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);
    }

    @Test
    public void testSendDashboardToCloud() throws Exception {
        Dashboard dashboard = buildDashboardForUplinkMsg();

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
    }

    @Test
    public void testSendUnassignDashboardFromGroupOnEdgeToCloud() throws Exception {
        // create dashboard entity group and assign to edge
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardToDeleteGroup", tenantId);

        Dashboard savedDashboard = saveDashboardOnCloudAndVerifyDeliveryToEdge(dashboardEntityGroup.getId());

        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardDeleteMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        dashboardDeleteMsgBuilder.setIdMSB(savedDashboard.getUuidId().getMostSignificantBits());
        dashboardDeleteMsgBuilder.setIdLSB(savedDashboard.getUuidId().getLeastSignificantBits());
        dashboardDeleteMsgBuilder.setEntityGroupIdMSB(dashboardEntityGroup.getUuidId().getMostSignificantBits());
        dashboardDeleteMsgBuilder.setEntityGroupIdLSB(dashboardEntityGroup.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(dashboardDeleteMsgBuilder);

        upLinkMsgBuilder.addDashboardUpdateMsg(dashboardDeleteMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        DashboardInfo dashboardInfo = doGet("/api/dashboard/info/" + savedDashboard.getUuidId(), DashboardInfo.class);
        Assert.assertNotNull(dashboardInfo);
        List<DashboardInfo> edgeDashboards = doGetTypedWithPageLink("/api/entityGroup/" + dashboardEntityGroup.getUuidId() + "/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeDashboards.contains(dashboardInfo));
    }

    @Test
    public void testSendDeleteDashboardOnEdgeToCloud() throws Exception {
        // create dashboard entity group and assign to edge
        EntityGroup dashboardEntityGroup = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardToDeleteGroup", tenantId);

        Dashboard savedDashboard = saveDashboardOnCloudAndVerifyDeliveryToEdge(dashboardEntityGroup.getId());

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

    private Dashboard saveDashboardOnCloudAndVerifyDeliveryToEdge(EntityGroupId entityGroupId) throws Exception {
        // create dashboard and assign to edge
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Dashboard to delete");
        if (entityGroupId != null) {
            dashboard = doPost("/api/dashboard?entityGroupId={entityGroupId}", dashboard, Dashboard.class, entityGroupId.getId().toString());
        } else {
            dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        }
        edgeImitator.expectMessageAmount(1); // dashboard message
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DashboardUpdateMsg> dashboardUpdateMsgOpt = edgeImitator.findMessageByType(DashboardUpdateMsg.class);
        Assert.assertTrue(dashboardUpdateMsgOpt.isPresent());
        DashboardUpdateMsg dashboardUpdateMsg = dashboardUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(dashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        return dashboard;
    }

    private Dashboard buildDashboardForUplinkMsg() {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(new DashboardId(UUID.randomUUID()));
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(DASHBOARD_TITLE);
        return dashboard;
    }

}
