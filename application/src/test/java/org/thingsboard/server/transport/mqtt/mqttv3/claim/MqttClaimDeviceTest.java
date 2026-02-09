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
package org.thingsboard.server.transport.mqtt.mqttv3.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_CLAIM_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CLAIM_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttClaimDeviceTest extends AbstractMqttIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .build();
        processBeforeTest(configProperties);
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
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
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

    protected void validateClaimResponse(boolean emptyPayload, MqttTestClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publishAndWait(DEVICE_CLAIM_TOPIC, failurePayloadBytes);
        awaitForClaimingInfoToBeRegistered(savedDevice.getId());

        loginCustomerUser();
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

        client.publishAndWait(DEVICE_CLAIM_TOPIC, payloadBytes);
        client.disconnect();
        awaitForClaimingInfoToBeRegistered(savedDevice.getId());

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerId, claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void validateGatewayClaimResponse(String deviceName, boolean emptyPayload, MqttTestClient client, byte[] failurePayloadBytes, byte[] payloadBytes) throws Exception {
        client.publishAndWait(GATEWAY_CLAIM_TOPIC, failurePayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(
                () -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100
        );

        assertNotNull(savedDevice);
        awaitForClaimingInfoToBeRegistered(savedDevice.getId());

        loginCustomerUser();
        ClaimRequest claimRequest;
        if (!emptyPayload) {
            claimRequest = new ClaimRequest("value");
        } else {
            claimRequest = new ClaimRequest(null);
        }

        ClaimResponse claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(GATEWAY_CLAIM_TOPIC, payloadBytes);
        client.disconnect();
        awaitForClaimingInfoToBeRegistered(savedDevice.getId());

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );

        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerId, claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + deviceName + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
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

    protected void processProtoTestGatewayClaimDevice(String deviceName, boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        byte[] failurePayloadBytes;
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = getGatewayClaimMsg(deviceName, 0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getGatewayClaimMsg(deviceName, 60000, emptyPayload).toByteArray();
        }
        failurePayloadBytes = getGatewayClaimMsg(deviceName, 1, emptyPayload).toByteArray();

        validateGatewayClaimResponse(deviceName, emptyPayload, client, failurePayloadBytes, payloadBytes);
    }

    private TransportApiProtos.GatewayClaimMsg getGatewayClaimMsg(String deviceName, long duration, boolean emptyPayload) {
        TransportApiProtos.GatewayClaimMsg.Builder gatewayClaimMsgBuilder = TransportApiProtos.GatewayClaimMsg.newBuilder();
        TransportApiProtos.ClaimDeviceMsg.Builder claimDeviceMsgBuilder = TransportApiProtos.ClaimDeviceMsg.newBuilder();
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setDurationMs(duration);
        }
        TransportApiProtos.ClaimDevice claimDevice = claimDeviceBuilder.build();
        claimDeviceMsgBuilder.setClaimRequest(claimDevice);
        claimDeviceMsgBuilder.setDeviceName(deviceName);
        TransportApiProtos.ClaimDeviceMsg claimDeviceMsg = claimDeviceMsgBuilder.build();
        gatewayClaimMsgBuilder.addMsg(claimDeviceMsg);
        return gatewayClaimMsgBuilder.build();
    }

}
