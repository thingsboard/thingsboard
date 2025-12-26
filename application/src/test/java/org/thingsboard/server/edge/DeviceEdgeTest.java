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
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.protobuf.AbstractMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.RpcResponseMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.gen.edge.v1.UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;

@TestPropertySource(properties = {
        "transport.mqtt.enabled=true"
})
@DaoSqlTest
public class DeviceEdgeTest extends AbstractEdgeTest {

    private static final String DEFAULT_DEVICE_TYPE = "default";

    @Autowired
    protected EdgeService edgeService;

    @Test
    public void testDevices() throws Exception {
        // create device and assign to edge; update device
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // unassign device from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        // delete device - message expected, message send to all edges
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));

        // create device #2 and assign to edge
        edgeImitator.expectMessageAmount(2);
        savedDevice = saveDevice("Edge Device 3", DEFAULT_DEVICE_TYPE);
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Device deviceFromMsg = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
        Assert.assertNotNull(deviceFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice, deviceFromMsg);
        Assert.assertEquals(savedDevice.getId(), deviceFromMsg.getId());
        Assert.assertEquals(savedDevice.getName(), deviceFromMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceFromMsg.getType());

        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getDeviceProfileId().getId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getDeviceProfileId().getId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        // assign device #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        deviceUpdateMsg = deviceUpdateMsgOpt.get();
        deviceFromMsg = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
        Assert.assertNotNull(deviceFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getId(), deviceFromMsg.getCustomerId());

        // unassign device #2 from customer
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/customer/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        deviceUpdateMsg = deviceUpdateMsgOpt.get();
        deviceFromMsg = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
        Assert.assertNotNull(deviceFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(deviceFromMsg.getCustomerId().getId().getMostSignificantBits(), deviceFromMsg.getCustomerId().getId().getLeastSignificantBits())));

        // delete device #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

    }

    @Test
    public void testUpdateDeviceCredentials() throws Exception {
        // create device and assign to edge; update device
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        verifyUpdateFirmwareIdSoftwareIdAndDeviceData(savedDevice);

        // update device credentials - ACCESS_TOKEN
        edgeImitator.expectMessageAmount(1);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");
        deviceCredentials = doPost("/api/device/credentials", deviceCredentials, DeviceCredentials.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        DeviceCredentials deviceCredentialsMsg = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
        Assert.assertEquals(deviceCredentials, deviceCredentialsMsg);

        // update device credentials - X509_CERTIFICATE
        edgeImitator.expectMessageAmount(1);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        deviceCredentials.setCredentialsId(null);
        deviceCredentials.setCredentialsValue("-----BEGIN RSA PRIVATE KEY-----");
        deviceCredentials = doPost("/api/device/credentials", deviceCredentials, DeviceCredentials.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        deviceCredentialsMsg = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
        Assert.assertNotNull(deviceCredentialsMsg);
        Assert.assertEquals(deviceCredentials.getCredentialsType(), deviceCredentialsMsg.getCredentialsType());
        Assert.assertFalse(deviceCredentialsMsg.getCredentialsId().isEmpty());
        Assert.assertNotNull(deviceCredentialsMsg.getCredentialsValue());
        Assert.assertEquals(deviceCredentials.getCredentialsValue(), deviceCredentialsMsg.getCredentialsValue());
    }

    private void verifyUpdateFirmwareIdSoftwareIdAndDeviceData(Device savedDevice) throws InterruptedException {
        // create ota packages
        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(thermostatDeviceProfile.getId(), OtaPackageType.FIRMWARE);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo softwareOtaPackageInfo = saveOtaPackageInfo(thermostatDeviceProfile.getId(), OtaPackageType.SOFTWARE);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // update device
        edgeImitator.expectMessageAmount(2);
        savedDevice.setFirmwareId(firmwareOtaPackageInfo.getId());
        savedDevice.setSoftwareId(softwareOtaPackageInfo.getId());

        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        MqttDeviceTransportConfiguration transportConfiguration = new MqttDeviceTransportConfiguration();
        transportConfiguration.getProperties().put("topic", "tb_rule_engine.thermostat");
        deviceData.setTransportConfiguration(transportConfiguration);
        savedDevice.setDeviceData(deviceData);

        savedDevice = doPost("/api/device", savedDevice, Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Device deviceMsg = JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
        Assert.assertNotNull(deviceMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice, deviceMsg);
        Assert.assertEquals(firmwareOtaPackageInfo.getId(), deviceMsg.getFirmwareId());
        Assert.assertEquals(softwareOtaPackageInfo.getId(), deviceMsg.getSoftwareId());
        deviceData = deviceMsg.getDeviceData();
        Assert.assertTrue(deviceData.getTransportConfiguration() instanceof MqttDeviceTransportConfiguration);
        MqttDeviceTransportConfiguration mqttDeviceTransportConfiguration =
                (MqttDeviceTransportConfiguration) deviceData.getTransportConfiguration();
        Assert.assertEquals("tb_rule_engine.thermostat", mqttDeviceTransportConfiguration.getProperties().get("topic"));
    }

    @Test
    public void testDeviceReachedMaximumAllowedOnCloud() throws Exception {
        // update tenant profile configuration
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId.getId(), TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        profileConfiguration.setMaxDevices(1);
        tenantProfile.getProfileData().setConfiguration(profileConfiguration);
        doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        loginTenantAdmin();

        Device device = buildDeviceForUplinkMsg("Edge Device", DEFAULT_DEVICE_TYPE);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(device.getUuidId().getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceUpdateMsgBuilder.setEntity(JacksonUtil.toString(device));
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());
    }

    @Test
    public void testSendDeviceRpcResponseToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceRpcCallMsg.Builder deviceRpcCallResponseBuilder = DeviceRpcCallMsg.newBuilder();
        deviceRpcCallResponseBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceRpcCallResponseBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceRpcCallResponseBuilder.setOneway(true);
        deviceRpcCallResponseBuilder.setRequestId(0);
        deviceRpcCallResponseBuilder.setExpirationTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        RpcResponseMsg.Builder responseBuilder =
                RpcResponseMsg.newBuilder().setResponse("{}");
        testAutoGeneratedCodeByProtobuf(responseBuilder);

        deviceRpcCallResponseBuilder.setResponseMsg(responseBuilder.build());
        testAutoGeneratedCodeByProtobuf(deviceRpcCallResponseBuilder);

        uplinkMsgBuilder.addDeviceRpcCallMsg(deviceRpcCallResponseBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    @Test
    public void testSendDeviceCredentialsUpdateToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        DeviceCredentials deviceCredentials = buildDeviceCredentialsForUplinkMsg(device.getId());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsUpdateMsg.Builder deviceCredentialsUpdateMsgBuilder = DeviceCredentialsUpdateMsg.newBuilder();
        deviceCredentialsUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceCredentials));
        testAutoGeneratedCodeByProtobuf(deviceCredentialsUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    @Test
    public void testSendDeviceCredentialsRequestToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getUuidId() + "/credentials", DeviceCredentials.class);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsRequestMsg.Builder deviceCredentialsRequestMsgBuilder = DeviceCredentialsRequestMsg.newBuilder();
        deviceCredentialsRequestMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsRequestMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        DeviceCredentials deviceCredentialsMsg = JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
        Assert.assertNotNull(deviceCredentialsMsg);
        Assert.assertEquals(device.getId(), deviceCredentialsMsg.getDeviceId());
        Assert.assertEquals(deviceCredentials, deviceCredentialsMsg);
    }

    @Test
    public void testSendAttributesRequestToCloud() throws Exception {
        Device device = findDeviceByName("Edge Device 1");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"key1\":\"value1\"}",
                "key1", "value1");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"inactivityTimeout\":3600000}",
                "inactivityTimeout", "3600000");
        sendAttributesRequestAndVerify(device, DataConstants.SHARED_SCOPE, "{\"key2\":\"value2\"}",
                "key2", "value2");
        sendAttributesRequestAndVerify(device, DataConstants.SERVER_SCOPE, "{\"jsonKey\":{\"nestedJsonKey\":\"nestedJsonValue\"}}",
                "jsonKey", "{\"nestedJsonKey\":\"nestedJsonValue\"}");

        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/" + DataConstants.SERVER_SCOPE, "keys", "key1, inactivityTimeout, jsonKey");
        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/" + DataConstants.SHARED_SCOPE, "keys", "key2");
    }

    @Test
    public void testSendDeleteDeviceOnEdgeToCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceDeleteMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceDeleteMsgBuilder.setMsgType(ENTITY_DELETED_RPC_MESSAGE);
        deviceDeleteMsgBuilder.setIdMSB(savedDevice.getId().getId().getMostSignificantBits());
        deviceDeleteMsgBuilder.setIdLSB(savedDevice.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceDeleteMsgBuilder);

        upLinkMsgBuilder.addDeviceUpdateMsg(deviceDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                doGet("/api/device/info/" + savedDevice.getUuidId(), DeviceInfo.class, status().isNotFound())
        );
    }

    @Test
    public void testSendTelemetryToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        edgeImitator.expectResponsesAmount(2);

        JsonObject data = new JsonObject();
        String timeseriesKey = "key";
        String timeseriesValue = "25";
        data.addProperty(timeseriesKey, timeseriesValue);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data, System.currentTimeMillis()));
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(entityDataBuilder);
        uplinkMsgBuilder.addEntityData(entityDataBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        JsonObject attributesData = new JsonObject();
        String attributesKey = "test_attr";
        String attributesValue = "test_value";
        attributesData.addProperty(attributesKey, attributesValue);
        UplinkMsg.Builder uplinkMsgBuilder2 = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder2 = EntityDataProto.newBuilder();
        entityDataBuilder2.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder2.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder2.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder2.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder2.setPostAttributeScope(DataConstants.SERVER_SCOPE);

        uplinkMsgBuilder2.addEntityData(entityDataBuilder2.build());

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder2.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> loadDeviceTimeseries(device, timeseriesKey).containsKey(timeseriesKey));

        Map<String, List<Map<String, String>>> timeseries = loadDeviceTimeseries(device, timeseriesKey);
        Assert.assertTrue(timeseries.containsKey(timeseriesKey));
        Assert.assertEquals(1, timeseries.get(timeseriesKey).size());
        Assert.assertEquals(timeseriesValue, timeseries.get(timeseriesKey).get(0).get("value"));

        String attributeValuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/" + DataConstants.SERVER_SCOPE;
        List<Map<String, String>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {
        });

        Assert.assertEquals(3, attributes.size());

        Optional<Map<String, String>> activeAttributeOpt = getAttributeByKey("active", attributes);
        Assert.assertTrue(activeAttributeOpt.isPresent());
        Map<String, String> activeAttribute = activeAttributeOpt.get();
        Assert.assertEquals("true", activeAttribute.get("value"));

        Optional<Map<String, String>> customAttributeOpt = getAttributeByKey(attributesKey, attributes);
        Assert.assertTrue(customAttributeOpt.isPresent());
        Map<String, String> customAttribute = customAttributeOpt.get();
        Assert.assertEquals(attributesValue, customAttribute.get("value"));

        doDelete("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/SERVER_SCOPE?keys=" + attributesKey, String.class);
    }

    @Test
    public void testSendOutdatedAttributeToCloud() throws Exception {
        long ts = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        edgeImitator.expectResponsesAmount(1);

        ObjectNode attributesNode = JacksonUtil.newObjectNode();
        String originalValue = "original_value";
        attributesNode.put("test_attr", originalValue);
        doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/attributes/SERVER_SCOPE", attributesNode);

        // Wait before device attributes saved to database
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/attributes/" + DataConstants.SERVER_SCOPE;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains("test_attr");
                });

        JsonObject attributesData = new JsonObject();
        // incorrect msg, will not be saved, because of ts is lower than for already existing
        String attributesKey = "test_attr";
        String attributeValueIncorrect = "test_value";
        // correct msg, will be saved, no ts issue
        String attributeKey2 = "test_attr2";
        String attributeValue2Correct = "test_value2";
        attributesData.addProperty(attributesKey, attributeValueIncorrect);
        attributesData.addProperty(attributeKey2, attributeValue2Correct);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder.setPostAttributeScope(DataConstants.SERVER_SCOPE);
        entityDataBuilder.setAttributeTs(ts);

        uplinkMsgBuilder.addEntityData(entityDataBuilder.build());

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        String attributeValuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/" + DataConstants.SERVER_SCOPE;
        List<Map<String, String>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {
        });

        Optional<Map<String, String>> customAttributeOpt = getAttributeByKey(attributesKey, attributes);
        Assert.assertTrue(customAttributeOpt.isPresent());
        Map<String, String> customAttribute = customAttributeOpt.get();
        Assert.assertNotEquals(attributeValueIncorrect, customAttribute.get("value"));
        Assert.assertEquals(originalValue, customAttribute.get("value"));

        customAttributeOpt = getAttributeByKey(attributeKey2, attributes);
        Assert.assertTrue(customAttributeOpt.isPresent());
        customAttribute = customAttributeOpt.get();
        Assert.assertEquals(attributeValue2Correct, customAttribute.get("value"));

        doDelete("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/SERVER_SCOPE?keys=" + attributesKey, String.class);
    }

    @Test
    public void testSendDeviceToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        String deviceOnCloudName = StringUtils.randomAlphanumeric(15);
        Device deviceOnCloud = saveDevice(deviceOnCloudName, DEFAULT_DEVICE_TYPE);

        Device deviceMsg = buildDeviceForUplinkMsg(deviceOnCloudName, "test");

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(deviceMsg.getUuidId().getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(deviceMsg.getUuidId().getLeastSignificantBits());
        deviceUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceMsg));
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg latestDeviceUpdateMsg = deviceUpdateMsgOpt.get();
        Device deviceLatestMsg = JacksonUtil.fromString(latestDeviceUpdateMsg.getEntity(), Device.class, true);
        Assert.assertNotNull(deviceLatestMsg);
        Assert.assertNotEquals(deviceOnCloudName, deviceLatestMsg.getName());

        UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Assert.assertNotEquals(deviceOnCloud.getId().getId(), newDeviceId);

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());
    }

    @Test
    public void testSendDeviceToCloud() throws Exception {
        String deviceName = "Edge Device 2";
        Device deviceMsg = buildDeviceForUplinkMsg(deviceName, "test");

        // create device on edge
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(deviceMsg.getUuidId().getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(deviceMsg.getUuidId().getLeastSignificantBits());
        deviceUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceMsg));
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Device device = doGet("/api/device/" + deviceMsg.getId().getId(), Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals(deviceName, device.getName());

        // update device on edge
        deviceMsg.setName(deviceName + " Updated");
        uplinkMsgBuilder = UplinkMsg.newBuilder();
        deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(deviceMsg.getUuidId().getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(deviceMsg.getUuidId().getLeastSignificantBits());
        deviceUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceMsg));
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        device = doGet("/api/device/" + deviceMsg.getId().getId(), Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals(deviceName + " Updated", device.getName());
    }

    @Test
    public void testRpcCall() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("requestId", new Random().nextInt());
        body.put("requestUUID", Uuids.timeBased().toString());
        body.put("oneway", false);
        body.put("expirationTime", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        body.put("method", "test_method");
        body.put("params", "{\"param1\":\"value1\"}");
        body.put("persisted", true);
        body.put("retries", 2);

        EdgeEvent edgeEvent = constructEdgeEvent(tenantId, edge.getId(), EdgeEventActionType.RPC_CALL,
                device.getId().getId(), EdgeEventType.DEVICE, body);
        edgeImitator.expectMessageAmount(1);
        edgeEventService.saveAsync(edgeEvent).get();
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceRpcCallMsg);
        DeviceRpcCallMsg latestDeviceRpcCallMsg = (DeviceRpcCallMsg) latestMessage;
        Assert.assertEquals("test_method", latestDeviceRpcCallMsg.getRequestMsg().getMethod());
        Assert.assertTrue(latestDeviceRpcCallMsg.getPersisted());
        Assert.assertEquals(2, latestDeviceRpcCallMsg.getRetries());
    }

    private void sendAttributesRequestAndVerify(Device device, String scope, String attributesDataStr, String expectedKey,
                                                String expectedValue) throws Exception {
        JsonNode attributesData = JacksonUtil.toJsonNode(attributesDataStr);

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + scope,
                attributesData);

        // Wait before device attributes saved to database before requesting them from edge
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/attributes/" + scope;
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains(expectedKey);
                });

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AttributesRequestMsg.Builder attributesRequestMsgBuilder = AttributesRequestMsg.newBuilder();
        attributesRequestMsgBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        attributesRequestMsgBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        attributesRequestMsgBuilder.setEntityType(EntityType.DEVICE.name());
        attributesRequestMsgBuilder.setScope(scope);
        testAutoGeneratedCodeByProtobuf(attributesRequestMsgBuilder);
        uplinkMsgBuilder.addAttributesRequestMsg(attributesRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertEquals(scope, latestEntityDataMsg.getPostAttributeScope());
        Assert.assertTrue(latestEntityDataMsg.hasAttributesUpdatedMsg());

        TransportProtos.PostAttributeMsg attributesUpdatedMsg = latestEntityDataMsg.getAttributesUpdatedMsg();

        boolean found = false;
        for (TransportProtos.KeyValueProto keyValueProto : attributesUpdatedMsg.getKvList()) {
            if (keyValueProto.getKey().equals(expectedKey)) {
                Assert.assertEquals(expectedKey, keyValueProto.getKey());
                switch (keyValueProto.getType()) {
                    case STRING_V -> Assert.assertEquals(expectedValue, keyValueProto.getStringV());
                    case LONG_V -> Assert.assertEquals(Long.parseLong(expectedValue), keyValueProto.getLongV());
                    case JSON_V -> Assert.assertEquals(JacksonUtil.toJsonNode(expectedValue), JacksonUtil.toJsonNode(keyValueProto.getJsonV()));
                    default -> Assert.fail("Unexpected data type: " + keyValueProto.getType());
                }
                found = true;
            }
        }
        Assert.assertTrue("Expected key and value must be found", found);
    }

    private Optional<Map<String, String>> getAttributeByKey(String key, List<Map<String, String>> attributes) {
        return attributes.stream().filter(kv -> kv.get("key").equals(key)).findFirst();
    }

    private Map<String, List<Map<String, String>>> loadDeviceTimeseries(Device device, String timeseriesKey) throws Exception {
        return doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/values/timeseries?keys=" + timeseriesKey,
                new TypeReference<>() {
                });
    }

    @Test
    public void sendUpdateSharedAttributeToCloudAndValidateDeviceSubscription() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        DeviceCredentials deviceCredentials = doGet("/api/device/" + device.getUuidId() + "/credentials", DeviceCredentials.class);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(deviceCredentials.getCredentialsId());
        MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);

        client.subscribeAndWait("v1/devices/me/attributes", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(device.getId(), FeatureType.ATTRIBUTES, 1);

        edgeImitator.expectResponsesAmount(1);

        JsonObject attributesData = new JsonObject();
        String attrKey = "sharedAttrName";
        String attrValue = "sharedAttrValue";
        attributesData.addProperty(attrKey, attrValue);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder();
        entityDataBuilder.setEntityType(device.getId().getEntityType().name());
        entityDataBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        entityDataBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        entityDataBuilder.setAttributesUpdatedMsg(JsonConverter.convertToAttributesProto(attributesData));
        entityDataBuilder.setPostAttributeScope(DataConstants.SHARED_SCOPE);
        uplinkMsgBuilder.addEntityData(entityDataBuilder.build());

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        Assert.assertTrue(onUpdateCallback.getSubscribeLatch().await(TIMEOUT, TimeUnit.SECONDS));

        Assert.assertEquals(JacksonUtil.newObjectNode().put(attrKey, attrValue),
                JacksonUtil.fromBytes(onUpdateCallback.getPayloadBytes()));

        client.disconnect();
    }

    @Test
    public void testVerifyDeliveryOfLatestTimeseriesOnAttributesRequest() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        JsonNode timeseriesData = JacksonUtil.toJsonNode("{\"temperature\":25, \"isEnabled\": true}");

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE,
                timeseriesData);

        // Wait before device timeseries saved to database before requesting them from edge
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/timeseries";
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {
                    });
                    return actualKeys != null && !actualKeys.isEmpty() && actualKeys.contains("temperature");
                });

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AttributesRequestMsg.Builder attributesRequestMsgBuilder = AttributesRequestMsg.newBuilder();
        attributesRequestMsgBuilder.setEntityIdMSB(device.getUuidId().getMostSignificantBits());
        attributesRequestMsgBuilder.setEntityIdLSB(device.getUuidId().getLeastSignificantBits());
        attributesRequestMsgBuilder.setEntityType(EntityType.DEVICE.name());
        attributesRequestMsgBuilder.setScope(DataConstants.SERVER_SCOPE);
        uplinkMsgBuilder.addAttributesRequestMsg(attributesRequestMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof EntityDataProto);
        EntityDataProto latestEntityDataMsg = (EntityDataProto) latestMessage;
        Assert.assertEquals(device.getUuidId().getMostSignificantBits(), latestEntityDataMsg.getEntityIdMSB());
        Assert.assertEquals(device.getUuidId().getLeastSignificantBits(), latestEntityDataMsg.getEntityIdLSB());
        Assert.assertEquals(device.getId().getEntityType().name(), latestEntityDataMsg.getEntityType());
        Assert.assertTrue(latestEntityDataMsg.hasPostTelemetryMsg());

        TransportProtos.PostTelemetryMsg timeseriesUpdatedMsg = latestEntityDataMsg.getPostTelemetryMsg();
        Assert.assertEquals(1, timeseriesUpdatedMsg.getTsKvListList().size());
        TransportProtos.TsKvListProto tsKvListProto = timeseriesUpdatedMsg.getTsKvListList().get(0);
        Assert.assertEquals(2, tsKvListProto.getKvList().size());
        for (TransportProtos.KeyValueProto keyValueProto : tsKvListProto.getKvList()) {
            if ("temperature".equals(keyValueProto.getKey())) {
                Assert.assertEquals(TransportProtos.KeyValueType.LONG_V, keyValueProto.getType());
                Assert.assertEquals(25, keyValueProto.getLongV());
            } else if ("isEnabled".equals(keyValueProto.getKey())) {
                Assert.assertEquals(TransportProtos.KeyValueType.BOOLEAN_V, keyValueProto.getType());
                Assert.assertTrue(keyValueProto.getBoolV());
            } else {
                Assert.fail("Unexpected key: " + keyValueProto.getKey());
            }
        }
    }

    @Test
    public void testVerifyProcessCorrectEdgeUpdateToDeviceActorOnUnassignFromDifferentEdge() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // assign device to another edge
        Edge tmpEdge = doPost("/api/edge", constructEdge("Test Tmp Edge", "test"), Edge.class);
        doPost("/api/edge/" + tmpEdge.getUuidId()
                + "/device/" + device.getUuidId(), Device.class);
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, device.getId());
        Assert.assertEquals(2, relatedEdgeIds.size());

        // unassign device from edge
        doDelete("/api/edge/" + edge.getUuidId()
                + "/device/" + device.getUuidId(), Device.class);
        relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, device.getId());
        Assert.assertEquals(1, relatedEdgeIds.size());
        Assert.assertEquals(tmpEdge.getId(), relatedEdgeIds.get(0));

        // edge is disconnected: perform rpc call - no edge event saved
        doPostAsync(
                "/api/rpc/oneway/" + device.getId().getId().toString(),
                JacksonUtil.toString(createDefaultRpc()),
                String.class,
                status().isOk());
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(tbClusterService, never()).onEdgeHighPriorityMsg(any()));

        // edge is connected: perform rpc call to verify edgeId in DeviceActorMessageProcessor updated properly
        simulateEdgeActivation(tmpEdge);
        doPostAsync(
                "/api/rpc/oneway/" + device.getId().getId().toString(),
                JacksonUtil.toString(createDefaultRpc()),
                String.class,
                status().isOk());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(tbClusterService, times(1)).onEdgeHighPriorityMsg(any()));

        // clean up tmp edge
        doDelete("/api/edge/" + tmpEdge.getId().getId().toString()).andExpect(status().isOk());
    }

    private Device buildDeviceForUplinkMsg(String name, String type) {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setTenantId(tenantId);
        device.setType(type);
        device.setName(name);
        return device;
    }

    private DeviceCredentials buildDeviceCredentialsForUplinkMsg(DeviceId deviceId) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsId(String.valueOf(UUID.randomUUID()));
        deviceCredentials.setCredentialsValue("NEW_TOKEN");
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        return deviceCredentials;
    }

    private void simulateEdgeActivation(Edge edge) throws Exception {
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);
        verifyEdgeConnected();
    }

}
