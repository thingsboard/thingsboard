/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.junit.Assert;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapIntegrationTest extends AbstractControllerTest {

    protected static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);

    public static final String DEVICE_TELEMETRY_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostTelemetry {\n" +
            "  string key1 = 1;\n" +
            "  bool key2 = 2;\n" +
            "  double key3 = 3;\n" +
            "  int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static final String DEVICE_ATTRIBUTES_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string key1 = 1;\n" +
            "  bool key2 = 2;\n" +
            "  double key3 = 3;\n" +
            "  int32 key4 = 4;\n" +
            "  JsonObject key5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    protected Tenant savedTenant;
    protected User tenantAdmin;

    protected Device savedDevice;
    protected String accessToken;

    protected DeviceProfile deviceProfile;

    protected void processBeforeTest(String deviceName, CoapDeviceType coapDeviceType, TransportPayloadType payloadType) throws Exception {
        this.processBeforeTest(deviceName, coapDeviceType, payloadType, null, null, DeviceProfileProvisionType.DISABLED, null, null);
    }

    protected void processBeforeTest(String deviceName,
                                     CoapDeviceType coapDeviceType,
                                     TransportPayloadType payloadType,
                                     String telemetryProtoSchema,
                                     String attributesProtoSchema,
                                     DeviceProfileProvisionType provisionType,
                                     String provisionKey, String provisionSecret
    ) throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant" + atomicInteger.getAndIncrement() + "@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        if (coapDeviceType != null) {
            DeviceProfile coapDeviceProfile = createCoapDeviceProfile(payloadType, coapDeviceType, attributesProtoSchema, provisionType, provisionKey, provisionSecret, telemetryProtoSchema);
            deviceProfile = doPost("/api/deviceProfile", coapDeviceProfile, DeviceProfile.class);
            device.setType(deviceProfile.getName());
            device.setDeviceProfileId(deviceProfile.getId());
        }

        savedDevice = doPost("/api/device", device, Device.class);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

    }

    protected void processAfterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    protected TransportProtos.KeyValueProto getKeyValueProto(String key, String strValue, TransportProtos.KeyValueType type) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(key);
        keyValueProtoBuilder.setType(type);
        switch (type) {
            case BOOLEAN_V:
                keyValueProtoBuilder.setBoolV(Boolean.parseBoolean(strValue));
                break;
            case LONG_V:
                keyValueProtoBuilder.setLongV(Long.parseLong(strValue));
                break;
            case DOUBLE_V:
                keyValueProtoBuilder.setDoubleV(Double.parseDouble(strValue));
                break;
            case STRING_V:
                keyValueProtoBuilder.setStringV(strValue);
                break;
            case JSON_V:
                keyValueProtoBuilder.setJsonV(strValue);
                break;
        }
        return keyValueProtoBuilder.build();
    }

    protected DeviceProfile createCoapDeviceProfile(TransportPayloadType transportPayloadType, CoapDeviceType coapDeviceType,
                                                    String attributesProtoSchema, DeviceProfileProvisionType provisionType,
                                                    String provisionKey, String provisionSecret, String telemetryProtoSchema) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(transportPayloadType.name());
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setProvisionDeviceKey(provisionKey);
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
                if (StringUtils.isEmpty(telemetryProtoSchema)) {
                    telemetryProtoSchema = DEVICE_TELEMETRY_PROTO_SCHEMA;
                }
                if (StringUtils.isEmpty(attributesProtoSchema)) {
                    attributesProtoSchema = DEVICE_ATTRIBUTES_PROTO_SCHEMA;
                }
                protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(telemetryProtoSchema);
                protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(attributesProtoSchema);
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
                provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(provisionSecret);
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(provisionSecret);
                break;
            case DISABLED:
            default:
                provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(provisionSecret);
                break;
        }
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfileData.setConfiguration(configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    protected CoapClient getCoapClient(FeatureType featureType) {
        return new CoapClient(getFeatureTokenUrl(accessToken, featureType));
    }

    protected CoapClient getCoapClient(String featureTokenUrl) {
        return new CoapClient(featureTokenUrl);
    }

    protected String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    protected <T> T doExecuteWithRetriesAndInterval(SupplierWithThrowable<T> supplier, int retries, int intervalMs) throws Exception {
        int count = 0;
        T result = null;
        Throwable lastException = null;
        while (count < retries) {
            try {
                result = supplier.get();
                if (result != null) {
                    return result;
                }
            } catch (Throwable e) {
                lastException = e;
            }
            count++;
            if (count < retries) {
                Thread.sleep(intervalMs);
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        } else {
            return result;
        }
    }

    @FunctionalInterface
    public interface SupplierWithThrowable<T> {
        T get() throws Throwable;
    }
}
