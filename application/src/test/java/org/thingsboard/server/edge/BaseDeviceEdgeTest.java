/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "transport.mqtt.enabled=true"
})
abstract public class BaseDeviceEdgeTest extends AbstractEdgeTest {

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
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        // delete device - no messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // create device #2 and assign to edge
        edgeImitator.expectMessageAmount(2);
        savedDevice = saveDevice("Edge Device 3", "Default");
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());

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

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getCustomerIdLSB());

        // unassign device #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB())));

        // delete device #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/device/" + savedDevice.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
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
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), deviceCredentialsUpdateMsg.getCredentialsType());
        Assert.assertEquals(deviceCredentials.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsId());
        Assert.assertFalse(deviceCredentialsUpdateMsg.hasCredentialsValue());

        // update device credentials - X509_CERTIFICATE
        edgeImitator.expectMessageAmount(1);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        deviceCredentials.setCredentialsId(null);
        deviceCredentials.setCredentialsValue("-----BEGIN RSA PRIVATE KEY-----");
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsUpdateMsg);
        deviceCredentialsUpdateMsg = (DeviceCredentialsUpdateMsg) latestMessage;
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), deviceCredentialsUpdateMsg.getCredentialsType());
        Assert.assertFalse(deviceCredentialsUpdateMsg.getCredentialsId().isEmpty());
        Assert.assertTrue(deviceCredentialsUpdateMsg.hasCredentialsValue());
        Assert.assertEquals(deviceCredentials.getCredentialsValue(), deviceCredentialsUpdateMsg.getCredentialsValue());
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
        edgeImitator.expectMessageAmount(1);
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
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceUpdateMsg);
        DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDevice.getName(), deviceUpdateMsg.getName());
        Assert.assertEquals(savedDevice.getType(), deviceUpdateMsg.getType());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getFirmwareIdMSB());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getFirmwareIdLSB());
        Assert.assertEquals(softwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getSoftwareIdMSB());
        Assert.assertEquals(softwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getSoftwareIdLSB());
        Optional<DeviceData> deviceDataOpt =
                dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
        Assert.assertTrue(deviceDataOpt.isPresent());
        deviceData = deviceDataOpt.get();
        Assert.assertTrue(deviceData.getTransportConfiguration() instanceof MqttDeviceTransportConfiguration);
        MqttDeviceTransportConfiguration mqttDeviceTransportConfiguration =
                (MqttDeviceTransportConfiguration) deviceData.getTransportConfiguration();
        Assert.assertEquals("tb_rule_engine.thermostat", mqttDeviceTransportConfiguration.getProperties().get("topic"));
    }

    @Test
    public void testDeviceReachedMaximumAllowedOnCloud() throws Exception {
        // update tenant profile configuration
        loginSysAdmin();
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + savedTenant.getTenantProfileId().getId(), TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        profileConfiguration.setMaxDevices(1);
        tenantProfile.getProfileData().setConfiguration(profileConfiguration);
        doPost("/api/tenantProfile/", tenantProfile, TenantProfile.class);

        loginTenantAdmin();

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device");
        deviceUpdateMsgBuilder.setType("default");
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

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceCredentialsUpdateMsg.Builder deviceCredentialsUpdateMsgBuilder = DeviceCredentialsUpdateMsg.newBuilder();
        deviceCredentialsUpdateMsgBuilder.setDeviceIdMSB(device.getUuidId().getMostSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setDeviceIdLSB(device.getUuidId().getLeastSignificantBits());
        deviceCredentialsUpdateMsgBuilder.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN.name());
        deviceCredentialsUpdateMsgBuilder.setCredentialsId("NEW_TOKEN");
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
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdMSB(), device.getUuidId().getMostSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getDeviceIdLSB(), device.getUuidId().getLeastSignificantBits());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsType(), deviceCredentials.getCredentialsType().name());
        Assert.assertEquals(deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentials.getCredentialsId());
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

        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/" + DataConstants.SERVER_SCOPE, "keys","key1, inactivityTimeout");
        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/" + DataConstants.SHARED_SCOPE, "keys", "key2");
    }

    @Test
    public void testSendDeleteDeviceOnEdgeToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceDeleteMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        deviceDeleteMsgBuilder.setIdMSB(device.getId().getId().getMostSignificantBits());
        deviceDeleteMsgBuilder.setIdLSB(device.getId().getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(deviceDeleteMsgBuilder);

        upLinkMsgBuilder.addDeviceUpdateMsg(deviceDeleteMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        device = doGet("/api/device/" + device.getUuidId(), Device.class);
        Assert.assertNotNull(device);
        List<Device> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<Device>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeDevices.contains(device));
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
        List<Map<String, String>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {});

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
    public void testSendDeviceToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        String deviceOnCloudName = StringUtils.randomAlphanumeric(15);
        Device deviceOnCloud = saveDevice(deviceOnCloudName, "Default");

        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName(deviceOnCloudName);
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(2);
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg latestDeviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertNotEquals(deviceOnCloudName, latestDeviceUpdateMsg.getName());
        Assert.assertEquals(deviceOnCloudName, latestDeviceUpdateMsg.getConflictName());

        UUID newDeviceId = new UUID(latestDeviceUpdateMsg.getIdMSB(), latestDeviceUpdateMsg.getIdLSB());

        Assert.assertNotEquals(deviceOnCloud.getId().getId(), newDeviceId);

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());

        Optional<DeviceCredentialsRequestMsg> deviceCredentialsUpdateMsgOpt = edgeImitator.findMessageByType(DeviceCredentialsRequestMsg.class);
        Assert.assertTrue(deviceCredentialsUpdateMsgOpt.isPresent());
        DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = deviceCredentialsUpdateMsgOpt.get();
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertNotEquals(deviceOnCloudName, device.getName());
    }

    @Test
    public void testSendDeviceToCloud() throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName("Edge Device 2");
        deviceUpdateMsgBuilder.setType("test");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceCredentialsRequestMsg);
        DeviceCredentialsRequestMsg latestDeviceCredentialsRequestMsg = (DeviceCredentialsRequestMsg) latestMessage;
        Assert.assertEquals(uuid.getMostSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdMSB());
        Assert.assertEquals(uuid.getLeastSignificantBits(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        UUID newDeviceId = new UUID(latestDeviceCredentialsRequestMsg.getDeviceIdMSB(), latestDeviceCredentialsRequestMsg.getDeviceIdLSB());

        Device device = doGet("/api/device/" + newDeviceId, Device.class);
        Assert.assertNotNull(device);
        Assert.assertEquals("Edge Device 2", device.getName());
    }

    @Test
    public void testRpcCall() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        ObjectNode body = mapper.createObjectNode();
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
        clusterService.onEdgeEventUpdate(tenantId, edge.getId());
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
        JsonNode attributesData = mapper.readTree(attributesDataStr);

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
                    case STRING_V:
                        Assert.assertEquals(expectedValue, keyValueProto.getStringV());
                        break;
                    case LONG_V:
                        Assert.assertEquals(Long.parseLong(expectedValue), keyValueProto.getLongV());
                        break;
                    default:
                        Assert.fail("Unexpected data type: " + keyValueProto.getType());
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
                new TypeReference<>() {});
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

        Assert.assertTrue(onUpdateCallback.getSubscribeLatch().await(5, TimeUnit.SECONDS));

        Assert.assertEquals(JacksonUtil.OBJECT_MAPPER.createObjectNode().put(attrKey, attrValue),
                JacksonUtil.fromBytes(onUpdateCallback.getPayloadBytes()));

        client.disconnect();
    }

    @Test
    public void testVerifyDeliveryOfLatestTimeseriesOnAttributesRequest() throws Exception {
        Device device = findDeviceByName("Edge Device 1");

        JsonNode timeseriesData = mapper.readTree("{\"temperature\":25, \"isEnabled\": true}");

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE,
                timeseriesData);

        // Wait before device timeseries saved to database before requesting them from edge
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    String urlTemplate = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/keys/timeseries";
                    List<String> actualKeys = doGetAsyncTyped(urlTemplate, new TypeReference<>() {});
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
}
