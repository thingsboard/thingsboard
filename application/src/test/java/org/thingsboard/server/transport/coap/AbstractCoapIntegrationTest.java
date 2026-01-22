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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.utils.PortFinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "coap.server.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
@Slf4j
public abstract class AbstractCoapIntegrationTest extends AbstractTransportIntegrationTest {

    public static final String COAP_HOST = "localhost";
    public static final int COAP_PORT = PortFinder.findAvailableUdpPort();
    public static final String COAP_BASE_URL = "coap://" + COAP_HOST + ":" + COAP_PORT + "/api/v1/";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.info("coap.bind_port = {}", COAP_PORT);
        registry.add("coap.bind_port", () -> COAP_PORT);
    }

    protected final byte[] EMPTY_PAYLOAD = new byte[0];
    protected CoapTestClient client;
    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";
    protected static final String PAYLOAD_VALUES_STR_01 = "{\"key2\":\"value2\", \"key3\":false, \"key4\": 4.0, \"key5\": 5," +
            " \"key6\": {\"someNumber_02\": 52, \"someArray_02\": [1,2,3,4], \"someNestedObject_02\": {\"key_02\": \"value_02\"}}}";

    protected void processBeforeTest() throws Exception {
        loginTenantAdmin();
    }

    protected void processAfterTest() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    protected void processBeforeTest(CoapTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        deviceProfile = createCoapDeviceProfile(config);
        assertNotNull(deviceProfile);
        savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName());
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        assertNotNull(deviceCredentials);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
    }

    protected DeviceProfile createCoapDeviceProfile(CoapTestConfigProperties config) throws Exception {
        CoapDeviceType coapDeviceType = config.getCoapDeviceType();
        if (coapDeviceType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            TransportPayloadType transportPayloadType = config.getTransportPayloadType();
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            deviceProfile.setTransportType(DeviceTransportType.COAP);
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = new CoapDeviceProfileTransportConfiguration();
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration;
            if (CoapDeviceType.DEFAULT.equals(coapDeviceType)) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
                if (TransportPayloadType.PROTOBUF.equals(transportPayloadType)) {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
                    String telemetryProtoSchema = config.getTelemetryProtoSchema();
                    String attributesProtoSchema = config.getAttributesProtoSchema();
                    String rpcResponseProtoSchema = config.getRpcResponseProtoSchema();
                    String rpcRequestProtoSchema = config.getRpcRequestProtoSchema();
                    protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(
                            telemetryProtoSchema != null ? telemetryProtoSchema : DEVICE_TELEMETRY_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(
                            attributesProtoSchema != null ? attributesProtoSchema : DEVICE_ATTRIBUTES_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(
                            rpcResponseProtoSchema != null ? rpcResponseProtoSchema : DEVICE_RPC_RESPONSE_PROTO_SCHEMA
                    );
                    protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(
                            rpcRequestProtoSchema != null ? rpcRequestProtoSchema : DEVICE_RPC_REQUEST_PROTO_SCHEMA
                    );
                    transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
                } else {
                    transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
                }
                defaultCoapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
                coapDeviceTypeConfiguration = defaultCoapDeviceTypeConfiguration;
            } else {
                coapDeviceTypeConfiguration = new EfentoCoapDeviceTypeConfiguration();
            }
            coapDeviceProfileTransportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
            deviceProfileData.setTransportConfiguration(coapDeviceProfileTransportConfiguration);
            DeviceProfileProvisionConfiguration provisionConfiguration;
            switch (provisionType) {
                case ALLOW_CREATE_NEW_DEVICES:
                    provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case CHECK_PRE_PROVISIONED_DEVICES:
                    provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case DISABLED:
                default:
                    provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
            }
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfileData.setConfiguration(configuration);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);
            return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        }
    }

    protected Device createDevice(String name, String type) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }
}
