/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseDeviceProfileEdgeTest extends AbstractEdgeTest {

    @Test
    public void testDeviceProfiles() throws Exception {
        // create device profile
        DeviceProfile deviceProfile = this.createDeviceProfile("ONE_MORE_DEVICE_PROFILE", null);
        extendDeviceProfileData(deviceProfile);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        // update device profile
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(deviceProfile.getId());
        edgeImitator.expectMessageAmount(1);
        Assert.assertTrue(edgeImitator.waitForMessages());

        deviceProfile.setFirmwareId(firmwareOtaPackageInfo.getId());
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getFirmwareIdMSB());
        Assert.assertEquals(firmwareOtaPackageInfo.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getFirmwareIdLSB());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
    }

    @Test
    public void testDeviceProfiles_snmp() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("SNMP", createSnmpDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(DeviceTransportType.SNMP.name(), deviceProfileUpdateMsg.getTransportType());

        Optional<DeviceProfileData> deviceProfileDataOpt =
                dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());

        Assert.assertTrue(deviceProfileDataOpt.isPresent());
        DeviceProfileData deviceProfileData = deviceProfileDataOpt.get();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof SnmpDeviceProfileTransportConfiguration);
        SnmpDeviceProfileTransportConfiguration transportConfiguration =
                (SnmpDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();
        Assert.assertEquals(Integer.valueOf(1000), transportConfiguration.getTimeoutMs());
        Assert.assertEquals(Integer.valueOf(3), transportConfiguration.getRetries());

        Assert.assertFalse(transportConfiguration.getCommunicationConfigs().isEmpty());
        SnmpCommunicationConfig communicationConfig = transportConfiguration.getCommunicationConfigs().get(0);
        Assert.assertTrue(communicationConfig instanceof TelemetryQueryingSnmpCommunicationConfig);
        TelemetryQueryingSnmpCommunicationConfig snmpCommunicationConfig =
                (TelemetryQueryingSnmpCommunicationConfig) communicationConfig;

        Assert.assertEquals(Long.valueOf(500L), snmpCommunicationConfig.getQueryingFrequencyMs());
        Assert.assertFalse(snmpCommunicationConfig.getMappings().isEmpty());

        SnmpMapping snmpMapping = snmpCommunicationConfig.getMappings().get(0);
        Assert.assertEquals("temperature", snmpMapping.getKey());
        Assert.assertEquals("1.3.3.5.6.7.8.9.1", snmpMapping.getOid());
        Assert.assertEquals(DataType.DOUBLE, snmpMapping.getDataType());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }

    @Test
    public void testDeviceProfiles_lwm2m() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("LWM2M", createLwm2mDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(DeviceTransportType.LWM2M.name(), deviceProfileUpdateMsg.getTransportType());

        Optional<DeviceProfileData> deviceProfileDataOpt =
                dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());

        Assert.assertTrue(deviceProfileDataOpt.isPresent());
        DeviceProfileData deviceProfileData = deviceProfileDataOpt.get();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof Lwm2mDeviceProfileTransportConfiguration);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration =
                (Lwm2mDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();

        OtherConfiguration clientLwM2mSettings = transportConfiguration.getClientLwM2mSettings();
        Assert.assertEquals(PowerMode.DRX, clientLwM2mSettings.getPowerMode());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getFwUpdateStrategy());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getSwUpdateStrategy());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getClientOnlyObserveAfterConnect());

        Assert.assertTrue(transportConfiguration.isBootstrapServerUpdateEnable());

        Assert.assertFalse(transportConfiguration.getBootstrap().isEmpty());
        LwM2MBootstrapServerCredential lwM2MBootstrapServerCredential = transportConfiguration.getBootstrap().get(0);
        Assert.assertTrue(lwM2MBootstrapServerCredential instanceof NoSecLwM2MBootstrapServerCredential);
        NoSecLwM2MBootstrapServerCredential noSecLwM2MBootstrapServerCredential = (NoSecLwM2MBootstrapServerCredential) lwM2MBootstrapServerCredential;

        Assert.assertEquals("PUBLIC_KEY", noSecLwM2MBootstrapServerCredential.getServerPublicKey());
        Assert.assertEquals(Integer.valueOf(123), noSecLwM2MBootstrapServerCredential.getShortServerId());
        Assert.assertTrue(noSecLwM2MBootstrapServerCredential.isBootstrapServerIs());
        Assert.assertEquals("localhost", noSecLwM2MBootstrapServerCredential.getHost());
        Assert.assertEquals(Integer.valueOf(5687), noSecLwM2MBootstrapServerCredential.getPort());

        TelemetryMappingConfiguration observeAttr = transportConfiguration.getObserveAttr();
        Assert.assertEquals("batteryLevel", observeAttr.getKeyName().get("/3_1.0/0/9"));
        Assert.assertTrue(observeAttr.getObserve().isEmpty());
        Assert.assertTrue(observeAttr.getAttribute().isEmpty());
        Assert.assertFalse(observeAttr.getTelemetry().isEmpty());
        Assert.assertTrue(observeAttr.getTelemetry().contains("/3_1.0/0/9"));
        Assert.assertTrue(observeAttr.getAttributeLwm2m().isEmpty());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }

    @Test
    public void testDeviceProfiles_coap() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("COAP", createCoapDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(DeviceTransportType.COAP.name(), deviceProfileUpdateMsg.getTransportType());

        Optional<DeviceProfileData> deviceProfileDataOpt =
                dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());

        Assert.assertTrue(deviceProfileDataOpt.isPresent());
        DeviceProfileData deviceProfileData = deviceProfileDataOpt.get();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration transportConfiguration =
                (CoapDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();

        PowerSavingConfiguration clientSettings = transportConfiguration.getClientSettings();

        Assert.assertEquals(PowerMode.DRX, clientSettings.getPowerMode());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getEdrxCycle());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getPsmActivityTimer());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getPagingTransmissionWindow());

        Assert.assertTrue(transportConfiguration.getCoapDeviceTypeConfiguration() instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration coapDeviceTypeConfiguration =
                (DefaultCoapDeviceTypeConfiguration) transportConfiguration.getCoapDeviceTypeConfiguration();

        Assert.assertTrue(coapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration);

        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                (ProtoTransportPayloadConfiguration) coapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();

        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_TELEMETRY_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceTelemetryProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_ATTRIBUTES_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_RPC_RESPONSE_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_RPC_REQUEST_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }


    private DeviceProfile createDeviceProfileAndDoBasicAssert(String deviceProfileName, DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile(deviceProfileName, deviceProfileTransportConfiguration);
        extendDeviceProfileData(deviceProfile);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return deviceProfile;
    }

    private void removeDeviceProfileAndDoBasicAssert(DeviceProfile deviceProfile) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
    }

    private SnmpDeviceProfileTransportConfiguration createSnmpDeviceProfileTransportConfiguration() {
        SnmpDeviceProfileTransportConfiguration transportConfiguration = new SnmpDeviceProfileTransportConfiguration();
        List<SnmpCommunicationConfig> communicationConfigs = new ArrayList<>();
        TelemetryQueryingSnmpCommunicationConfig communicationConfig = new TelemetryQueryingSnmpCommunicationConfig();
        communicationConfig.setQueryingFrequencyMs(500L);
        List<SnmpMapping> mappings = new ArrayList<>();
        mappings.add(new SnmpMapping("1.3.3.5.6.7.8.9.1", "temperature", DataType.DOUBLE));
        communicationConfig.setMappings(mappings);
        communicationConfigs.add(communicationConfig);
        transportConfiguration.setCommunicationConfigs(communicationConfigs);
        transportConfiguration.setTimeoutMs(1000);
        transportConfiguration.setRetries(3);
        return transportConfiguration;
    }

    private Lwm2mDeviceProfileTransportConfiguration createLwm2mDeviceProfileTransportConfiguration() {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();

        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(AbstractLwM2MIntegrationTest.CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);

        transportConfiguration.setBootstrapServerUpdateEnable(true);

        TelemetryMappingConfiguration observeAttrConfiguration =
                JacksonUtil.fromString(AbstractLwM2MIntegrationTest.OBSERVE_ATTRIBUTES_WITH_PARAMS, TelemetryMappingConfiguration.class);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);

        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("PUBLIC_KEY");
        bootstrapServerCredential.setShortServerId(123);
        bootstrapServerCredential.setBootstrapServerIs(true);
        bootstrapServerCredential.setHost("localhost");
        bootstrapServerCredential.setPort(5687);
        bootstrap.add(bootstrapServerCredential);
        transportConfiguration.setBootstrap(bootstrap);

        return transportConfiguration;
    }

    private CoapDeviceProfileTransportConfiguration createCoapDeviceProfileTransportConfiguration() {
        CoapDeviceProfileTransportConfiguration transportConfiguration = new CoapDeviceProfileTransportConfiguration();
        PowerSavingConfiguration clientSettings = new PowerSavingConfiguration();
        clientSettings.setPowerMode(PowerMode.DRX);
        clientSettings.setEdrxCycle(1L);
        clientSettings.setPsmActivityTimer(1L);
        clientSettings.setPagingTransmissionWindow(1L);
        transportConfiguration.setClientSettings(clientSettings);
        DefaultCoapDeviceTypeConfiguration coapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
        ProtoTransportPayloadConfiguration transportPayloadTypeConfiguration = new ProtoTransportPayloadConfiguration();
        transportPayloadTypeConfiguration.setDeviceTelemetryProtoSchema(AbstractTransportIntegrationTest.DEVICE_TELEMETRY_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceAttributesProtoSchema(AbstractTransportIntegrationTest.DEVICE_ATTRIBUTES_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcResponseProtoSchema(AbstractTransportIntegrationTest.DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcRequestProtoSchema(AbstractTransportIntegrationTest.DEVICE_RPC_REQUEST_PROTO_SCHEMA);
        coapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        transportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
        return transportConfiguration;
    }
}
