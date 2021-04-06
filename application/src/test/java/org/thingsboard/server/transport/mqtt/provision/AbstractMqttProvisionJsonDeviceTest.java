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
package org.thingsboard.server.transport.mqtt.provision;

import com.google.gson.JsonObject;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractMqttProvisionJsonDeviceTest extends AbstractMqttIntegrationTest {

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Autowired
    DeviceService deviceService;

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
    }

    @Test
    public void testProvisioningDisabledDevice() throws Exception {
        processTestProvisioningDisabledDevice();
    }

    @Test
    public void testProvisioningCheckPreProvisionedDevice() throws Exception {
        processTestProvisioningCheckPreProvisionedDevice();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        processTestProvisioningCreateNewDeviceWithoutCredentials();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        processTestProvisioningCreateNewDeviceWithAccessToken();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithCert() throws Exception {
        processTestProvisioningCreateNewDeviceWithCert();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        processTestProvisioningCreateNewDeviceWithMqttBasic();
    }

    @Test
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    protected void processTestProvisioningDisabledDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.DISABLED, null, null);
        byte[] result = createMqttClientAndPublish().getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").getAsString());
    }


    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, "testProvisionKey", "testProvisionSecret");
        byte[] result = createMqttClientAndPublish().getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }


    protected void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, "testProvisionKey", "testProvisionSecret");
        String requestCredentials = ",\"credentialsType\": \"ACCESS_TOKEN\",\"token\": \"test_token\"";
        byte[] result = createMqttClientAndPublish(requestCredentials).getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "ACCESS_TOKEN");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }


    protected void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, "testProvisionKey", "testProvisionSecret");
        String requestCredentials = ",\"credentialsType\": \"X509_CERTIFICATE\",\"hash\": \"testHash\"";
        byte[] result = createMqttClientAndPublish(requestCredentials).getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "X509_CERTIFICATE");

        String cert = EncryptionUtil.trimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }


    protected void processTestProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        super.processBeforeTest("Test Provision device3", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES, "testProvisionKey", "testProvisionSecret");
        String requestCredentials = ",\"credentialsType\": \"MQTT_BASIC\",\"clientId\": \"test_clientId\",\"username\": \"test_username\",\"password\": \"test_password\"";
        byte[] result = createMqttClientAndPublish(requestCredentials).getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "MQTT_BASIC");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), EncryptionUtil.getSha3Hash("|", "test_clientId", "test_username"));

        BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
        mqttCredentials.setClientId("test_clientId");
        mqttCredentials.setUserName("test_username");
        mqttCredentials.setPassword("test_password");

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), JacksonUtil.toString(mqttCredentials));
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }

    protected void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES, "testProvisionKey", "testProvisionSecret");
        byte[] result = createMqttClientAndPublish().getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }

    protected void processTestProvisioningWithBadKeyDevice() throws Exception {
        super.processBeforeTest("Test Provision device", "Test Provision gateway", TransportPayloadType.JSON, null, null, null, null, null, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES, "testProvisionKeyOrig", "testProvisionSecret");
        byte[] result = createMqttClientAndPublish().getPayloadBytes();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").getAsString());
    }

    protected TestMqttCallback createMqttClientAndPublish() throws Exception {
        return createMqttClientAndPublish("");
    }

    protected TestMqttCallback createMqttClientAndPublish(String deviceCredentials) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceCredentials);
        MqttAsyncClient client = getMqttAsyncClient("provision");
        TestMqttCallback onProvisionCallback = getTestMqttCallback();
        client.setCallback(onProvisionCallback);
        client.subscribe(MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE.value());
        Thread.sleep(2000);
        client.publish(MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC, new MqttMessage(provisionRequestMsg.getBytes()));
        onProvisionCallback.getLatch().await(3, TimeUnit.SECONDS);
        return onProvisionCallback;
    }


    protected TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }


    protected static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getQoS() {
            return qoS;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    protected String createTestProvisionMessage(String deviceCredentials) {
        return "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"" + deviceCredentials + "}";
    }
}
