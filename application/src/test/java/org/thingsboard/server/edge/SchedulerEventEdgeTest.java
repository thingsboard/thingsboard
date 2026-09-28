// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerRepeat;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SchedulerEventEdgeTest extends AbstractEdgeTest {

    @Test
    public void testSchedulerEvent_tenantLevel() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent("Edge Scheduler Event", tenantId);
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        // assign to edge
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/schedulerEvent/" + savedSchedulerEvent.getUuidId(), SchedulerEventInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedSchedulerEvent, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        // update
        edgeImitator.expectMessageAmount(1);
        savedSchedulerEvent.setName("Edge Scheduler Event Updated");
        savedSchedulerEvent = doPost("/api/schedulerEvent", savedSchedulerEvent, SchedulerEvent.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedSchedulerEvent, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        // unassign from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/schedulerEvent/" + savedSchedulerEvent.getUuidId(), SchedulerEventInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareDeletedExpectedAndActual(savedSchedulerEvent);

        // delete
        doDelete("/api/schedulerEvent/" + savedSchedulerEvent.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testSchedulerEvent_customerLevel() throws Exception {
        Customer savedCustomer = createCustomerAndChangeEdgeOwner();
        SchedulerEvent schedulerEvent = createSchedulerEvent("Edge Customer Scheduler Event", savedCustomer.getId());
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        // assign to edge
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/schedulerEvent/" + savedSchedulerEvent.getUuidId(), SchedulerEventInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedSchedulerEvent, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        // update
        edgeImitator.expectMessageAmount(1);
        savedSchedulerEvent.setName("Edge Customer Scheduler Event Updated");
        savedSchedulerEvent = doPost("/api/schedulerEvent", savedSchedulerEvent, SchedulerEvent.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareExpectedAndActual(savedSchedulerEvent, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        // unassign from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/schedulerEvent/" + savedSchedulerEvent.getUuidId(), SchedulerEventInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        compareDeletedExpectedAndActual(savedSchedulerEvent);

        // delete
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/schedulerEvent/" + savedSchedulerEvent.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    public void testSchedulerEvent_toCloud_tenantLevel() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent("Edge Scheduler Event To Cloud", tenantId);
        schedulerEvent.setId(new SchedulerEventId(UUID.randomUUID()));

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + schedulerEvent.getId().getId(), SchedulerEvent.class);
        compareExpectedAndActual(schedulerEvent, foundSchedulerEvent);
        Assert.assertFalse(foundSchedulerEvent.isEnabled());

        Assert.assertEquals(1, getEdgeSchedulerEvents().getData().size());

        // update
        schedulerEvent.setName("Edge Scheduler Event To Cloud Updated");
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        foundSchedulerEvent = doGet("/api/schedulerEvent/" + schedulerEvent.getId().getId(), SchedulerEvent.class);
        compareExpectedAndActual(schedulerEvent, foundSchedulerEvent);
        Assert.assertFalse(foundSchedulerEvent.isEnabled());

        // delete from edge
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createDeleteUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        Assert.assertEquals(0, getEdgeSchedulerEvents().getData().size());

        // cleanup
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/schedulerEvent/" + schedulerEvent.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
    }

    @Test
    public void testSchedulerEvent_toCloud_customerLevel() throws Exception {
        Customer savedCustomer = createCustomerAndChangeEdgeOwner();
        SchedulerEvent schedulerEvent = createSchedulerEvent("Edge Customer Scheduler Event To Cloud", savedCustomer.getId());
        schedulerEvent.setId(new SchedulerEventId(UUID.randomUUID()));

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + schedulerEvent.getId().getId(), SchedulerEvent.class);
        compareExpectedAndActual(schedulerEvent, foundSchedulerEvent);
        Assert.assertFalse(foundSchedulerEvent.isEnabled());

        Assert.assertEquals(1, getEdgeSchedulerEvents().getData().size());

        // update
        schedulerEvent.setName("Edge Customer Scheduler Event To Cloud Updated");
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        foundSchedulerEvent = doGet("/api/schedulerEvent/" + schedulerEvent.getId().getId(), SchedulerEvent.class);
        compareExpectedAndActual(schedulerEvent, foundSchedulerEvent);
        Assert.assertFalse(foundSchedulerEvent.isEnabled());

        // delete from edge
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(createDeleteUplinkMsg(schedulerEvent));
        Assert.assertTrue(edgeImitator.waitForResponses());
        latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        Assert.assertEquals(0, getEdgeSchedulerEvents().getData().size());

        // cleanup
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/schedulerEvent/" + schedulerEvent.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer, 2);

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

    private PageData<SchedulerEventInfo> getEdgeSchedulerEvents() throws Exception {
        return doGetTyped("/api/edge/" + edge.getId().getId() + "/schedulerEvents?page=0&pageSize=100",
                new TypeReference<>() {});
    }

    private UplinkMsg createDeleteUplinkMsg(SchedulerEvent schedulerEvent) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addSchedulerEventUpdateMsg(
                EdgeMsgConstructorUtils.constructSchedulerEventDeleteMsg(schedulerEvent.getId()));
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        return uplinkMsgBuilder.build();
    }

    private UplinkMsg createUplinkMsg(SchedulerEvent schedulerEvent) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addSchedulerEventUpdateMsg(
                EdgeMsgConstructorUtils.constructSchedulerEventUpdatedMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, schedulerEvent));
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        return uplinkMsgBuilder.build();
    }

    private Customer createCustomerAndChangeEdgeOwner() throws Exception {
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        saveCustomer("Edge Sub Customer", savedCustomer.getId());

        // wait the create events of 2 roles processed by edge consumer before the owner of the edge updated
        TimeUnit.MILLISECONDS.sleep(500);

        changeEdgeOwnerToCustomer(savedCustomer);
        return savedCustomer;
    }

    private void compareExpectedAndActual(SchedulerEvent expected, UpdateMsgType expectedMsgType) {
        Optional<SchedulerEventUpdateMsg> msgOpt = edgeImitator.findMessageByType(SchedulerEventUpdateMsg.class);
        Assert.assertTrue(msgOpt.isPresent());
        SchedulerEventUpdateMsg actualMsg = msgOpt.get();
        Assert.assertEquals(expectedMsgType, actualMsg.getMsgType());
        Assert.assertEquals(expected.getUuidId().getMostSignificantBits(), actualMsg.getIdMSB());
        Assert.assertEquals(expected.getUuidId().getLeastSignificantBits(), actualMsg.getIdLSB());
        SchedulerEvent actual = JacksonUtil.fromString(actualMsg.getEntity(), SchedulerEvent.class, true);
        compareExpectedAndActual(expected, actual);
    }

    private void compareExpectedAndActual(SchedulerEvent expected, SchedulerEvent actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getType(), actual.getType());
        Assert.assertEquals(expected.getSchedule().toString(), actual.getSchedule().toString());
        Assert.assertEquals(expected.getConfiguration(), actual.getConfiguration());
        Assert.assertEquals(expected.getTenantId(), actual.getOriginatorId());
        Assert.assertEquals(expected.getCustomerId(), actual.getCustomerId());
    }

    private void compareDeletedExpectedAndActual(SchedulerEvent expected) {
        Optional<SchedulerEventUpdateMsg> msgOpt = edgeImitator.findMessageByType(SchedulerEventUpdateMsg.class);
        Assert.assertTrue(msgOpt.isPresent());
        SchedulerEventUpdateMsg actualMsg = msgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, actualMsg.getMsgType());
        Assert.assertEquals(expected.getUuidId().getMostSignificantBits(), actualMsg.getIdMSB());
        Assert.assertEquals(expected.getUuidId().getLeastSignificantBits(), actualMsg.getIdLSB());
    }

    private SchedulerEvent createSchedulerEvent(String name, EntityId ownerId) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName(name);
        schedulerEvent.setType("irrigation");
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setEnabled(true);

        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", "UTC");
        SchedulerRepeat schedulerRepeat = new MonthlyRepeat();
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);

        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.put("msgType", TbMsgType.POST_ATTRIBUTES_REQUEST.name());
        schedulerEvent.setConfiguration(configuration);

        schedulerEvent.setOriginatorId(tenantId);
        schedulerEvent.setOwnerId(ownerId);
        return schedulerEvent;
    }

}
