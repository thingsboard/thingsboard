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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.RpcRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TelemetryEdgeTest extends AbstractEdgeTest {

    @Test
    public void testTimeseriesWithFailures() throws Exception {
        int numberOfTimeseriesToSend = 333;
        int numberOfHighPriorityRpcRequests = 13;
        int frequencyOfRpcRequestsPerTimeseries = 25;

        Device device = findDeviceByName("Edge Device 1");

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 5% of cases
        edgeImitator.setFailureProbability(5.0);
        edgeImitator.expectMessageAmount(numberOfTimeseriesToSend + numberOfHighPriorityRpcRequests);
        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
            EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(edgeEvent).get();
            if (idx % frequencyOfRpcRequestsPerTimeseries == 0) {
                doPostAsync(
                        "/api/rpc/oneway/" + device.getId().getId().toString(),
                        JacksonUtil.toString(createDefaultRpc(idx)),
                        String.class,
                        status().isOk());
            }
        }

        Assert.assertTrue(edgeImitator.waitForMessages(120));

        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
        Assert.assertEquals(numberOfTimeseriesToSend, allTelemetryMsgs.size());

        for (int idx = 1; idx <= numberOfTimeseriesToSend; idx++) {
            Assert.assertTrue(isIdxExistsInTheDownlinkList(idx, allTelemetryMsgs));
        }

        List<DeviceRpcCallMsg> allDeviceRpcCallMsgs = edgeImitator.findAllMessagesByType(DeviceRpcCallMsg.class);
        Assert.assertEquals(numberOfHighPriorityRpcRequests, allDeviceRpcCallMsgs.size());

        for (int idx = 1; idx <= numberOfHighPriorityRpcRequests; idx++) {
            int pinValue = idx * frequencyOfRpcRequestsPerTimeseries;
            Assert.assertTrue(isPinValueExistsInTheRpcDownlinkList(pinValue, allDeviceRpcCallMsgs));
        }

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    @Test
    public void testAttributes() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        testAttributesUpdatedMsg(device.getId());
        testPostAttributesMsg(device);
        testAttributesDeleteMsg(device);
    }

    private void testPostAttributesMsg(Device device) throws Exception {
        String postAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key2\":\"value2\"}}";
        JsonNode postAttributesEntityData = JacksonUtil.toJsonNode(postAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.POST_ATTRIBUTES, device.getId().getId(), EdgeEventType.DEVICE, postAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasPostAttributesMsg());

        TransportProtos.PostAttributeMsg postAttributesMsg = latestEntityDataMsg.getPostAttributesMsg();
        Assert.assertEquals(1, postAttributesMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = postAttributesMsg.getKv(0);
        Assert.assertEquals("key2", keyValueProto.getKey());
        Assert.assertEquals("value2", keyValueProto.getStringV());
    }

    private void testAttributesDeleteMsg(Device device) throws Exception {
        String deleteAttributesData = "{\"scope\":\"SERVER_SCOPE\",\"keys\":[\"key1\",\"key2\"]}";
        JsonNode deleteAttributesEntityData = JacksonUtil.toJsonNode(deleteAttributesData);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_DELETED, device.getId().getId(), EdgeEventType.DEVICE, deleteAttributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());

        Assert.assertTrue(latestEntityDataMsg.hasAttributeDeleteMsg());

        AttributeDeleteMsg attributeDeleteMsg = latestEntityDataMsg.getAttributeDeleteMsg();
        Assert.assertEquals(attributeDeleteMsg.getScope(), deleteAttributesEntityData.get("scope").asText());

        Assert.assertEquals(2, attributeDeleteMsg.getAttributeNamesCount());
        Assert.assertEquals("key1", attributeDeleteMsg.getAttributeNames(0));
        Assert.assertEquals("key2", attributeDeleteMsg.getAttributeNames(1));
    }

    @Test
    public void testTimeseries() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        String timeseriesData = "{\"data\":{\"temperature\":25},\"ts\":" + System.currentTimeMillis() + "}";
        JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
        edgeImitator.expectMessageAmount(1);
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED, device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(latestEntityDataMsg.getEntityIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(latestEntityDataMsg.getEntityType(), device.getId().getEntityType().name());
        Assert.assertTrue(latestEntityDataMsg.hasPostTelemetryMsg());

        TransportProtos.PostTelemetryMsg postTelemetryMsg = latestEntityDataMsg.getPostTelemetryMsg();
        Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
        TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
        Assert.assertEquals(timeseriesEntityData.get("ts").asLong(), tsKvListProto.getTs());
        Assert.assertEquals(1, tsKvListProto.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
        Assert.assertEquals("temperature", keyValueProto.getKey());
        Assert.assertEquals(25, keyValueProto.getLongV());
    }

    private boolean isIdxExistsInTheDownlinkList(int idx, List<EntityDataProto> allTelemetryMsgs) {
        for (EntityDataProto proto : allTelemetryMsgs) {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = proto.getPostTelemetryMsg();
            Assert.assertEquals(1, postTelemetryMsg.getTsKvListCount());
            TransportProtos.TsKvListProto tsKvListProto = postTelemetryMsg.getTsKvList(0);
            Assert.assertEquals(1, tsKvListProto.getKvCount());
            TransportProtos.KeyValueProto keyValueProto = tsKvListProto.getKv(0);
            Assert.assertEquals("idx", keyValueProto.getKey());
            if (keyValueProto.getLongV() == idx) {
                return true;
            }
        }
        return false;
    }

    private boolean isPinValueExistsInTheRpcDownlinkList(int idx, List<DeviceRpcCallMsg> rpcDownlinkMsgs) {
        for (DeviceRpcCallMsg proto : rpcDownlinkMsgs) {
            RpcRequestMsg rpcRequestMsg = proto.getRequestMsg();
            if ((JacksonUtil.toJsonNode(rpcRequestMsg.getParams())).get("value").intValue() == idx) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testTimeseriesDeliveryFailuresForever_deliverOnlyDeviceUpdateMsgs() throws Exception {
        int numberOfMsgsToSend = 100;

        Device device = findDeviceByName("Edge Device 1");

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 100% of timeseries cases
        edgeImitator.setFailureProbability(100);
        edgeImitator.expectMessageAmount(numberOfMsgsToSend * 2);
        for (int idx = 1; idx <= numberOfMsgsToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
            EdgeEvent failedEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(failedEdgeEvent).get();

            EdgeEvent successEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, null);
            edgeEventService.saveAsync(successEdgeEvent).get();
        }

        Assert.assertTrue(edgeImitator.waitForMessages(120));

        List<EntityDataProto> allTelemetryMsgs = edgeImitator.findAllMessagesByType(EntityDataProto.class);
        Assert.assertTrue(allTelemetryMsgs.isEmpty());

        List<DeviceUpdateMsg> deviceUpdateMsgs = edgeImitator.findAllMessagesByType(DeviceUpdateMsg.class);
        Assert.assertEquals(numberOfMsgsToSend, deviceUpdateMsgs.size());

        List<DeviceCredentialsUpdateMsg> deviceCredentialsUpdateMsgs = edgeImitator.findAllMessagesByType(DeviceCredentialsUpdateMsg.class);
        Assert.assertEquals(numberOfMsgsToSend, deviceCredentialsUpdateMsgs.size());

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(false);
    }

    @Test
    public void testAttributesUpdatedMsg_userEntity() throws Exception {
        testAttributesUpdatedMsg(tenantAdminUserId);
    }

    private void testAttributesUpdatedMsg(EntityId entityId) throws Exception {
        String attributesData = "{\"scope\":\"SERVER_SCOPE\",\"kv\":{\"key1\":\"value1\"}}";
        JsonNode attributesEntityData = JacksonUtil.toJsonNode(attributesData);
        EdgeEvent edgeEvent1 = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.ATTRIBUTES_UPDATED, entityId.getId(), EdgeEventType.valueOf(entityId.getEntityType().name()), attributesEntityData);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent1).get();
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(entityId.getId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(entityId.getId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(entityId.getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals("SERVER_SCOPE", latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();
        Assert.assertEquals(1, attributesUpdatedMsg.getKvCount());
        TransportProtos.KeyValueProto keyValueProto = attributesUpdatedMsg.getKv(0);
        Assert.assertEquals("key1", keyValueProto.getKey());
        Assert.assertEquals("value1", keyValueProto.getStringV());
    }

    @Test
    public void testSendAttributesDeleteRequestToCloud_nonDeviceEntity() throws Exception {
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Delete Attribute Test");
        doPost("/api/edge/" + edge.getUuidId() + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        final String attributeKey = "key1";
        ObjectNode attributesData = JacksonUtil.newObjectNode();
        attributesData.put(attributeKey, "value1");
        doPost("/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributesData);

        // Wait before device attributes saved to database before deleting them
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains(attributeKey);
                });

        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(savedAsset.getUuidId().getMostSignificantBits())
                .setEntityIdLSB(savedAsset.getUuidId().getLeastSignificantBits())
                .setEntityType(savedAsset.getId().getEntityType().name());
        AttributeDeleteMsg.Builder attributeDeleteMsg = AttributeDeleteMsg.newBuilder();
        attributeDeleteMsg.setScope(DataConstants.SERVER_SCOPE);
        attributeDeleteMsg.addAllAttributeNames(List.of(attributeKey));
        attributeDeleteMsg.build();
        builder.setAttributeDeleteMsg(attributeDeleteMsg);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.addEntityData(builder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && actualKeys.isEmpty();
                });
    }

}
