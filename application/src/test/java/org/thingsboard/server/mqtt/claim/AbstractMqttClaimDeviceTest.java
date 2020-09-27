/**
 * Copyright © 2016-2020 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttClaimDeviceTest extends AbstractControllerTest {

    protected static final String MQTT_URL = "tcp://localhost:1883";
    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    protected User tenantAdmin;
    protected User customerAdmin;

    protected Tenant savedTenant;
    private Customer savedCustomer;

    protected Device savedDevice;
    protected String accessToken;

    protected Device savedGateway;
    protected String gatewayAccessToken;

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);


    @Before
    public void beforeTest() throws Exception {
        processBeforeTest(null);
    }

    protected void processBeforeTest(TransportPayloadType transportPayloadType) throws Exception {
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

        Device device = new Device();
        device.setName("Test Claim device");
        device.setType("default");


        Device gateway = new Device();
        gateway.setName("Test Claim gateway");
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);

        if (transportPayloadType != null) {
            DeviceProfile mqttDeviceProfile = createMqttDeviceProfile(transportPayloadType);
            DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", mqttDeviceProfile, DeviceProfile.class);
            device.setDeviceProfileId(savedDeviceProfile.getId());
            gateway.setDeviceProfileId(savedDeviceProfile.getId());
        }

        savedDevice = doPost("/api/device", device, Device.class);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

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
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Test
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device", false);
    }

    @Test
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload", true);
    }


    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(accessToken);
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = "{}".getBytes();
        } else {
            payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
        }
        byte[] failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    protected void validateClaimResponse(boolean emptyPayload, MqttAsyncClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publish(MqttTopics.DEVICE_CLAIM_TOPIC, new MqttMessage(failurePayloadBytes));

        Thread.sleep(1000);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publish(MqttTopics.DEVICE_CLAIM_TOPIC, new MqttMessage(payloadBytes));

        Thread.sleep(2000);

        ClaimResult claimResult = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk());
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        client.publish(MqttTopics.DEVICE_CLAIM_TOPIC, new MqttMessage(payloadBytes));

        Thread.sleep(1000);

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void validateGatewayClaimResponse(String deviceName, boolean emptyPayload, MqttAsyncClient client, byte[] failurePayloadBytes, byte[] payloadBytes) throws Exception {
        client.publish(MqttTopics.GATEWAY_CLAIM_TOPIC, new MqttMessage(failurePayloadBytes));

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

        client.publish(MqttTopics.GATEWAY_CLAIM_TOPIC, new MqttMessage(payloadBytes));

        Thread.sleep(2000);

        ClaimResult claimResult = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk());
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        client.publish(MqttTopics.GATEWAY_CLAIM_TOPIC, new MqttMessage(payloadBytes));
        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        String failurePayload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":1}" + "}";
        String payload;
        if (emptyPayload) {
            payload = "{\"" + deviceName + "\": " + "{}" + "}";
        } else {
            payload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":60000}" + "}";
        }
        payloadBytes = payload.getBytes();
        failurePayloadBytes = failurePayload.getBytes();
        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    protected MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }

    protected Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    protected DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    protected DeviceProfile createMqttDeviceProfile(TransportPayloadType transportPayloadType) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(transportPayloadType.name());
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setDescription(transportPayloadType.name() + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        MqttDeviceProfileTransportConfiguration transportConfiguration = new MqttDeviceProfileTransportConfiguration();
        transportConfiguration.setTransportPayloadType(transportPayloadType);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setConfiguration(configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }


}
