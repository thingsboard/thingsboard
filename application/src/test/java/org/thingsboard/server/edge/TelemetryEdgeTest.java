// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
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

        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

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
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

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
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
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

        Device device = saveDevice(StringUtils.randomAlphanumeric(15), THERMOSTAT_DEVICE_PROFILE_NAME);

        edgeImitator.setRandomFailuresOnTimeseriesDownlink(true);
        // imitator will generate failure in 100% of timeseries cases
        edgeImitator.setFailureProbability(100);
        edgeImitator.expectMessageAmount(numberOfMsgsToSend * 2);
        for (int idx = 1; idx <= numberOfMsgsToSend; idx++) {
            String timeseriesData = "{\"data\":{\"idx2\":" + idx + "},\"ts\":" + System.currentTimeMillis() + "}";
            JsonNode timeseriesEntityData = JacksonUtil.toJsonNode(timeseriesData);
            EdgeEvent failedEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.TIMESERIES_UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, timeseriesEntityData);
            edgeEventService.saveAsync(failedEdgeEvent).get();

            // add unique body to avoid merge and filter by device id in edge service (mergeAndFilterDownlinkDuplicates)
            JsonNode uniqueBody = JacksonUtil.toJsonNode("{\"idx\":" + idx + "}");
            EdgeEvent successEdgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.UPDATED,
                    device.getId().getId(), EdgeEventType.DEVICE, uniqueBody);
            edgeEventService.saveAsync(successEdgeEvent).get();
        }

        Assert.assertTrue(edgeImitator.waitForMessages(180));

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
        Asset savedAsset = saveAssetOnCloudAndVerifyDeliveryToEdge();

        final String attributeKey = "key1";
        ObjectNode attributesData = JacksonUtil.newObjectNode();
        attributesData.put(attributeKey, "value1");
        doPost("/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributesData);

        // Wait before device attributes saved to database before deleting them
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
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
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/ASSET/" + savedAsset.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && actualKeys.isEmpty();
                });
    }

    @Test
    public void testSendTelemetryForDeviceOfDifferentTenantToCloud() throws Exception {
        Device device = saveDifferentTenantDevice();
        loginDifferentTenant();
        String attributesUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/";
        doPost(attributesUrl + DataConstants.SHARED_SCOPE, JacksonUtil.toJsonNode("{\"key1\":\"value1\"}"));
        doPost(attributesUrl + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"key2\":\"value2\"}"));
        loginTenantAdmin();

        TransportProtos.PostAttributeMsg attributesMsg = TransportProtos.PostAttributeMsg.newBuilder()
                .addKv(TransportProtos.KeyValueProto.newBuilder()
                        .setKey("key1")
                        .setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV("updatedValue1").build())
                .build();
        EntityDataProto entityData = EntityDataProto.newBuilder()
                .setEntityIdMSB(device.getUuidId().getMostSignificantBits())
                .setEntityIdLSB(device.getUuidId().getLeastSignificantBits())
                .setEntityType(device.getId().getEntityType().name())
                .setAttributesUpdatedMsg(attributesMsg)
                .setPostAttributeScope(DataConstants.SHARED_SCOPE)
                .setAttributeDeleteMsg(AttributeDeleteMsg.newBuilder()
                        .setScope(DataConstants.SERVER_SCOPE)
                        .addAttributeNames("key2").build())
                .build();
        sendUplinkMsgAndWaitForResponse(uplinkMsgBuilder -> uplinkMsgBuilder.addEntityData(entityData));

        loginDifferentTenant();
        String valuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/attributes/";
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode sharedAttributes = doGetAsync(valuesUrl + DataConstants.SHARED_SCOPE, JsonNode.class);
                    Assert.assertEquals("value1", sharedAttributes.get(0).get("value").asText());
                    JsonNode serverAttributes = doGetAsync(valuesUrl + DataConstants.SERVER_SCOPE, JsonNode.class);
                    Assert.assertTrue(serverAttributes.findValuesAsText("key").contains("key2"));
                });
    }

    @Test
    public void testSendEntityGroupAttributesAndTelemetryToCloud() throws Exception {
        EntityGroup entityGroup = createEntityGroupAndAssignToEdge(EntityType.DEVICE, "TelemetryDeviceGroup", tenantId);

        long ts = System.currentTimeMillis();
        EntityDataProto entityData = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityGroup.getUuidId().getMostSignificantBits())
                .setEntityIdLSB(entityGroup.getUuidId().getLeastSignificantBits())
                .setEntityType(entityGroup.getId().getEntityType().name())
                .setAttributesUpdatedMsg(TransportProtos.PostAttributeMsg.newBuilder()
                        .addKv(TransportProtos.KeyValueProto.newBuilder()
                                .setKey("groupKey")
                                .setType(TransportProtos.KeyValueType.STRING_V)
                                .setStringV("groupValue").build())
                        .build())
                .setPostAttributeScope(DataConstants.SERVER_SCOPE)
                .setPostTelemetryMsg(TransportProtos.PostTelemetryMsg.newBuilder()
                        .addTsKvList(TransportProtos.TsKvListProto.newBuilder()
                                .setTs(ts)
                                .addKv(TransportProtos.KeyValueProto.newBuilder()
                                        .setKey("groupTemperature")
                                        .setType(TransportProtos.KeyValueType.LONG_V)
                                        .setLongV(25).build())
                                .build())
                        .build())
                .build();
        sendUplinkMsgAndWaitForResponse(uplinkMsgBuilder -> uplinkMsgBuilder.addEntityData(entityData));

        String valuesUrl = "/api/plugins/telemetry/ENTITY_GROUP/" + entityGroup.getUuidId() + "/values/";
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode attributes = doGetAsync(valuesUrl + "attributes/" + DataConstants.SERVER_SCOPE, JsonNode.class);
                    Assert.assertEquals(1, attributes.size());
                    Assert.assertEquals("groupKey", attributes.get(0).get("key").asText());
                    Assert.assertEquals("groupValue", attributes.get(0).get("value").asText());
                    JsonNode timeseries = doGetAsync(valuesUrl + "timeseries?keys=groupTemperature", JsonNode.class);
                    Assert.assertEquals(1, timeseries.get("groupTemperature").size());
                    Assert.assertEquals(ts, timeseries.get("groupTemperature").get(0).get("ts").asLong());
                    Assert.assertEquals("25", timeseries.get("groupTemperature").get(0).get("value").asText());
                });
    }

    @Test
    public void testTypeCastDisabledKeys_edgeEventKeepsStringsAndSendsKeysToEdge() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        String attributesBody = "{\"scope\":\"SERVER_SCOPE\",\"ts\":1000,\"kv\":{\"joinEui\":\"0000000000000000\",\"hardwareVersion\":\"1.0\"},"
                + "\"typeCastDisabledKeys\":[\"joinEui\",\"hardwareVersion\"]}";

        EntityDataProto attributesUpdated = sendEdgeEventAndGetEntityData(device, EdgeEventActionType.ATTRIBUTES_UPDATED, attributesBody);
        assertStringKv(attributesUpdated.getAttributesUpdatedMsg().getKvList(), "joinEui", "0000000000000000");
        assertStringKv(attributesUpdated.getAttributesUpdatedMsg().getKvList(), "hardwareVersion", "1.0");
        Assert.assertEquals(List.of("hardwareVersion", "joinEui"), attributesUpdated.getAttributesTypeCastDisabledKeysList().stream().sorted().toList());

        EntityDataProto postAttributes = sendEdgeEventAndGetEntityData(device, EdgeEventActionType.POST_ATTRIBUTES, attributesBody);
        assertStringKv(postAttributes.getPostAttributesMsg().getKvList(), "joinEui", "0000000000000000");
        assertStringKv(postAttributes.getPostAttributesMsg().getKvList(), "hardwareVersion", "1.0");
        Assert.assertEquals(List.of("hardwareVersion", "joinEui"), postAttributes.getAttributesTypeCastDisabledKeysList().stream().sorted().toList());

        EntityDataProto timeseries = sendEdgeEventAndGetEntityData(device, EdgeEventActionType.TIMESERIES_UPDATED,
                "{\"ts\":1000,\"data\":{\"devAddr\":\"00123456\"},\"typeCastDisabledKeys\":[\"devAddr\"]}");
        Assert.assertEquals(1000L, timeseries.getPostTelemetryMsg().getTsKvList(0).getTs());
        assertStringKv(timeseries.getPostTelemetryMsg().getTsKvList(0).getKvList(), "devAddr", "00123456");
        Assert.assertEquals(List.of("devAddr"), timeseries.getTelemetryTypeCastDisabledKeysList());
    }

    @Test
    public void testTypeCastDisabledKeys_edgeEventWithoutKeysIsCastAsBefore() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        EntityDataProto attributesUpdated = sendEdgeEventAndGetEntityData(device, EdgeEventActionType.ATTRIBUTES_UPDATED,
                "{\"scope\":\"SERVER_SCOPE\",\"ts\":1000,\"kv\":{\"joinEui\":\"0000000000000000\"}}");

        TransportProtos.KeyValueProto joinEui = attributesUpdated.getAttributesUpdatedMsg().getKv(0);
        Assert.assertEquals(TransportProtos.KeyValueType.LONG_V, joinEui.getType());
        Assert.assertEquals(0L, joinEui.getLongV());
        Assert.assertEquals(0, attributesUpdated.getAttributesTypeCastDisabledKeysCount());
    }

    @Test
    public void testSendTypeCastDisabledKeysToCloud_valuesAreSavedAsStrings() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        long ts = System.currentTimeMillis();

        // attributes updated: saved by the edge processor itself; telemetry: saved by the rule engine
        sendUplinkMsgAndWaitForResponse(uplinkMsgBuilder -> uplinkMsgBuilder.addEntityData(entityDataBuilder(device)
                .setAttributesUpdatedMsg(stringAttributes("joinEui", "0000000000000000", "hardwareVersion", "1.0"))
                .setPostAttributeScope(DataConstants.SERVER_SCOPE)
                .addAllAttributesTypeCastDisabledKeys(List.of("joinEui", "hardwareVersion"))
                .setPostTelemetryMsg(stringTelemetry(ts, "devAddr", "00123456"))
                .addTelemetryTypeCastDisabledKeys("devAddr")
                .build()));
        // post attributes: saved by the rule engine
        sendUplinkMsgAndWaitForResponse(uplinkMsgBuilder -> uplinkMsgBuilder.addEntityData(entityDataBuilder(device)
                .setPostAttributesMsg(stringAttributes("eui", "0004000000000001", "firmwareVersion", "2.0"))
                .setPostAttributeScope(DataConstants.CLIENT_SCOPE)
                .addAllAttributesTypeCastDisabledKeys(List.of("eui", "firmwareVersion"))
                .build()));

        String valuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/";
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode serverAttributes = doGetAsync(valuesUrl + "attributes/" + DataConstants.SERVER_SCOPE, JsonNode.class);
                    assertStringAttribute(serverAttributes, "joinEui", "0000000000000000");
                    assertStringAttribute(serverAttributes, "hardwareVersion", "1.0");
                    JsonNode clientAttributes = doGetAsync(valuesUrl + "attributes/" + DataConstants.CLIENT_SCOPE, JsonNode.class);
                    assertStringAttribute(clientAttributes, "eui", "0004000000000001");
                    assertStringAttribute(clientAttributes, "firmwareVersion", "2.0");
                    JsonNode timeseries = doGetAsync(valuesUrl + "timeseries?keys=devAddr", JsonNode.class);
                    Assert.assertEquals("00123456", timeseries.get("devAddr").get(0).get("value").asText());
                });
    }

    @Test
    public void testSendStringsWithoutTypeCastDisabledKeysToCloud_valuesAreCastAsBefore() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        long ts = System.currentTimeMillis();

        // what an edge without the type cast disabled keys sends
        sendUplinkMsgAndWaitForResponse(uplinkMsgBuilder -> uplinkMsgBuilder.addEntityData(entityDataBuilder(device)
                .setAttributesUpdatedMsg(stringAttributes("joinEui", "0000000000000000", "hardwareVersion", "1.0"))
                .setPostAttributeScope(DataConstants.SERVER_SCOPE)
                .setPostTelemetryMsg(stringTelemetry(ts, "devAddr", "00123456"))
                .build()));

        String valuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/";
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    JsonNode serverAttributes = doGetAsync(valuesUrl + "attributes/" + DataConstants.SERVER_SCOPE, JsonNode.class);
                    JsonNode joinEui = findAttributeValue(serverAttributes, "joinEui");
                    Assert.assertTrue("joinEui must be a number: " + joinEui, joinEui.isNumber());
                    Assert.assertEquals(0L, joinEui.asLong());
                    JsonNode hardwareVersion = findAttributeValue(serverAttributes, "hardwareVersion");
                    Assert.assertTrue("hardwareVersion must be a number: " + hardwareVersion, hardwareVersion.isNumber());
                    JsonNode timeseries = doGetAsync(valuesUrl + "timeseries?keys=devAddr", JsonNode.class);
                    Assert.assertEquals("123456", timeseries.get("devAddr").get(0).get("value").asText());
                });
    }

    private EntityDataProto sendEdgeEventAndGetEntityData(Device device, EdgeEventActionType action, String body) throws Exception {
        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), action, device.getId().getId(), EdgeEventType.DEVICE, JacksonUtil.toJsonNode(body));
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        return (EntityDataProto) latestMessage;
    }

    private static EntityDataProto.Builder entityDataBuilder(Device device) {
        return EntityDataProto.newBuilder()
                .setEntityIdMSB(device.getUuidId().getMostSignificantBits())
                .setEntityIdLSB(device.getUuidId().getLeastSignificantBits())
                .setEntityType(device.getId().getEntityType().name());
    }

    private static TransportProtos.PostAttributeMsg stringAttributes(String... keysAndValues) {
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            builder.addKv(stringKv(keysAndValues[i], keysAndValues[i + 1]));
        }
        return builder.build();
    }

    private static TransportProtos.PostTelemetryMsg stringTelemetry(long ts, String key, String value) {
        return TransportProtos.PostTelemetryMsg.newBuilder()
                .addTsKvList(TransportProtos.TsKvListProto.newBuilder().setTs(ts).addKv(stringKv(key, value)).build())
                .build();
    }

    private static TransportProtos.KeyValueProto stringKv(String key, String value) {
        return TransportProtos.KeyValueProto.newBuilder().setKey(key).setType(TransportProtos.KeyValueType.STRING_V).setStringV(value).build();
    }

    private static void assertStringKv(List<TransportProtos.KeyValueProto> kvList, String key, String expectedValue) {
        TransportProtos.KeyValueProto kv = kvList.stream().filter(item -> item.getKey().equals(key)).findFirst()
                .orElseThrow(() -> new AssertionError("No value for key " + key));
        Assert.assertEquals(key, TransportProtos.KeyValueType.STRING_V, kv.getType());
        Assert.assertEquals(expectedValue, kv.getStringV());
    }

    private static void assertStringAttribute(JsonNode attributes, String key, String expectedValue) {
        JsonNode value = findAttributeValue(attributes, key);
        Assert.assertTrue(key + " must be a string: " + value, value.isTextual());
        Assert.assertEquals(expectedValue, value.asText());
    }

    private static JsonNode findAttributeValue(JsonNode attributes, String key) {
        for (JsonNode attribute : attributes) {
            if (key.equals(attribute.get("key").asText())) {
                return attribute.get("value");
            }
        }
        throw new AssertionError("No attribute " + key + " in " + attributes);
    }

}
