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
package org.thingsboard.server.mqtt.claim;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.mqtt.AbstractMqttIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttClaimDeviceTest extends AbstractMqttIntegrationTest {

    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    protected User customerAdmin;
    protected Customer savedCustomer;

    @Before
    public void beforeTest() throws Exception {
        super.processBeforeTest("Test Claim device", "Test Claim gateway", null, null, null);
        createCustomerAndUser();
    }

    protected void createCustomerAndUser() throws Exception {
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
        user.setEmail("customer@thingsboard.org");

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD);
        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());
    }

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
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
        byte[] failurePayloadBytes;
        if (emptyPayload) {
            payloadBytes = "{}".getBytes();
            failurePayloadBytes = "{\"durationMs\":1}".getBytes();
        } else {
            payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
            failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        }
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    protected void validateClaimResponse(boolean emptyPayload, MqttAsyncClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publish(MqttTopics.DEVICE_CLAIM_TOPIC, new MqttMessage(failurePayloadBytes));

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest()),
                20,
                100
        );

        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publish(MqttTopics.DEVICE_CLAIM_TOPIC, new MqttMessage(payloadBytes));

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void validateGatewayClaimResponse(String deviceName, boolean emptyPayload, MqttAsyncClient client, byte[] failurePayloadBytes, byte[] payloadBytes) throws Exception {
        client.publish(MqttTopics.GATEWAY_CLAIM_TOPIC, new MqttMessage(failurePayloadBytes));

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100
        );

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

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );

        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        String failurePayload;
        String payload;
        if (emptyPayload) {
            failurePayload = "{\"" + deviceName + "\": " + "{\"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{}" + "}";
        } else {
            failurePayload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":1}" + "}";
            payload = "{\"" + deviceName + "\": " + "{\"secretKey\":\"value\", \"durationMs\":60000}" + "}";
        }
        payloadBytes = payload.getBytes();
        failurePayloadBytes = failurePayload.getBytes();
        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

}
