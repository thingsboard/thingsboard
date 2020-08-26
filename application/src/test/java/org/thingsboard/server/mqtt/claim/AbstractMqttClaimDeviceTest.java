/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.mqtt.claim;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.MqttTopics;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttClaimDeviceTest extends AbstractControllerTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    private Tenant savedTenant;
    private User tenantAdmin;
    private User customerAdmin;
    private Customer savedCustomer;

    protected Device savedGateway;
    protected String gatewayAccessToken;

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);


    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Test Claiming Tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant" + atomicInteger.getAndIncrement() + "@thingsboard.org");

        createUserAndLogin(tenantAdmin, "testPassword1");

        Customer customer = new Customer();
        customer.setTenantId(savedTenant.getId());
        customer.setTitle("Test Claiming Customer");
        savedCustomer = doPost("/api/customer", customer, Customer.class);
        assertNotNull(savedCustomer);
        assertEquals(savedTenant.getId(), savedCustomer.getTenantId());

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(savedTenant.getId());
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("customer" + atomicInteger.getAndIncrement() + "@thingsboard.org");

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD);
        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());

        Device gateway = new Device();
        gateway.setName("Test Claim gateway");
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);
        savedGateway = doPost("/api/device", gateway, Device.class);

        DeviceCredentials gatewayCredentials =
                doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
        gatewayAccessToken = gatewayCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    public void testClaimingDeviceV1Json() throws Exception {
        processTestClaimingDevice("Test claiming device V1 Json", MqttTopics.DEVICE_CLAIM_TOPIC_V1_JSON, false);
    }

    @Test
    public void testClaimingDeviceV2Json() throws Exception {
        processTestClaimingDevice("Test claiming device V2 Json", MqttTopics.DEVICE_CLAIM_TOPIC_V2_JSON, false);
    }

    @Test
    public void testClaimingDeviceV2Proto() throws Exception {
        processTestClaimingDevice("Test claiming device V2 Proto", MqttTopics.DEVICE_CLAIM_TOPIC_V2_PROTO, false);
    }

    @Test
    public void testClaimingDeviceV1JsonWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice("Test claiming device empty payload V1 Json", MqttTopics.DEVICE_CLAIM_TOPIC_V1_JSON, true);
    }

    @Test
    public void testClaimingDeviceV2JsonWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice("Test claiming device empty payload V2 Json", MqttTopics.DEVICE_CLAIM_TOPIC_V2_JSON, true);
    }

    @Test
    public void testClaimingDeviceV2ProtoWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice("Test claiming device empty payload V2 Proto", MqttTopics.DEVICE_CLAIM_TOPIC_V2_PROTO, true);
    }

    @Test
    public void testGatewayClaimingDeviceV1Json() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device V1 Json", MqttTopics.GATEWAY_CLAIM_TOPIC_V1_JSON, false);
    }

    @Test
    public void testGatewayClaimingDeviceV2Json() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device V2 Json", MqttTopics.GATEWAY_CLAIM_TOPIC_V2_JSON, false);
    }

    @Test
    public void testGatewayClaimingDeviceV2Proto() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device V2 Proto", MqttTopics.GATEWAY_CLAIM_TOPIC_V2_PROTO, false);
    }

    @Test
    public void testGatewayClaimingDeviceV1JsonWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload V1 Json", MqttTopics.GATEWAY_CLAIM_TOPIC_V1_JSON, true);
    }

    @Test
    public void testGatewayClaimingDeviceV2JsonWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload V2 Json", MqttTopics.GATEWAY_CLAIM_TOPIC_V2_JSON, true);
    }

    @Test
    public void testGatewayClaimingDeviceV2ProtoWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload V2 Proto", MqttTopics.GATEWAY_CLAIM_TOPIC_V2_PROTO, true);

    }

    private void processTestClaimingDevice(String deviceName, String topic, boolean emptyPayload) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        assertEquals(savedDevice.getTenantId(), savedTenant.getId());
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);
        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        byte[] payloadBytes;
        byte[] failurePayloadBytes;

        if (topic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V1_JSON) || topic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2_JSON)) {
            if (emptyPayload) {
                payloadBytes = "{}".getBytes();
            } else {
                payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
            }
            failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        } else {
            if (emptyPayload) {
                payloadBytes = getClaimDevice(0).toByteArray();
            } else {
                payloadBytes = getClaimDevice(60000).toByteArray();
            }
            failurePayloadBytes = getClaimDevice(1).toByteArray();
        }
        client.publish(topic, new MqttMessage(failurePayloadBytes));

        Thread.sleep(1000);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publish(topic, new MqttMessage(payloadBytes));

        Thread.sleep(1000);

        ClaimResult claimResult = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk());
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        client.publish(topic, new MqttMessage(payloadBytes));

        Thread.sleep(1000);

        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    private void processTestGatewayClaimingDevice(String deviceName, String topic, boolean emptyPayload) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        if (topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String failurePayload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":1}" + "}";
            String payload;
            if (emptyPayload) {
                payload = "{\"" + deviceName + "\": " + "{}" + "}";
            } else {
                payload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":60000}" + "}";
            }
            payloadBytes = payload.getBytes();
            failurePayloadBytes = failurePayload.getBytes();
        } else {
            if (emptyPayload) {
                payloadBytes = getGatewayClaimMsg(deviceName, 0).toByteArray();
            } else {
                payloadBytes = getGatewayClaimMsg(deviceName, 60000).toByteArray();
            }
            failurePayloadBytes = getGatewayClaimMsg(deviceName, 1).toByteArray();
        }
        client.publish(topic, new MqttMessage(failurePayloadBytes));

        Thread.sleep(1000);

        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(savedDevice);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publish(topic, new MqttMessage(payloadBytes));

        Thread.sleep(1000);

        ClaimResult claimResult = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk());
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        client.publish(topic, new MqttMessage(payloadBytes));
        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    private TransportApiProtos.GatewayClaimMsg getGatewayClaimMsg(String deviceName, long duration) {
        TransportApiProtos.GatewayClaimMsg.Builder gatewayClaimMsgBuilder = TransportApiProtos.GatewayClaimMsg.newBuilder();
        TransportApiProtos.ClaimDeviceMsg.Builder claimDeviceMsgBuilder = TransportApiProtos.ClaimDeviceMsg.newBuilder();
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (duration > 0) {
            claimDeviceBuilder.setSecretKey("value");
            claimDeviceBuilder.setDurationMs(duration);
        }
        TransportApiProtos.ClaimDevice claimDevice = claimDeviceBuilder.build();
        claimDeviceMsgBuilder.setClaimRequest(claimDevice);
        claimDeviceMsgBuilder.setDeviceName(deviceName);
        TransportApiProtos.ClaimDeviceMsg claimDeviceMsg = claimDeviceMsgBuilder.build();
        gatewayClaimMsgBuilder.addMsg(claimDeviceMsg);
        return gatewayClaimMsgBuilder.build();
    }

    private TransportApiProtos.ClaimDevice getClaimDevice(long duration) {
        TransportApiProtos.ClaimDevice.Builder claimDeviceFailureBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (duration > 0) {
            claimDeviceFailureBuilder.setSecretKey("value");
            claimDeviceFailureBuilder.setDurationMs(duration);
        }
        return claimDeviceFailureBuilder.build();
    }

    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }


}
