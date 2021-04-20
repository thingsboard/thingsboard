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
package org.thingsboard.server.transport.coap.provision;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@Slf4j
public abstract class AbstractCoapProvisionJsonDeviceTest extends AbstractCoapIntegrationTest {

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
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    private void processTestProvisioningDisabledDevice() throws Exception {
        super.processBeforeTest("Test Provision device", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, null, null, DeviceProfileProvisionType.DISABLED);
        byte[] result = createCoapClientAndPublish().getPayload();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").getAsString());
    }


    private void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        super.processBeforeTest("Test Provision device3", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);
        byte[] result = createCoapClientAndPublish().getPayload();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }


    private void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        super.processBeforeTest("Test Provision device3", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);
        String requestCredentials = ",\"credentialsType\": \"ACCESS_TOKEN\",\"token\": \"test_token\"";
        byte[] result = createCoapClientAndPublish(requestCredentials).getPayload();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(savedTenant.getTenantId(), "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "ACCESS_TOKEN");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }


    private void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        super.processBeforeTest("Test Provision device3", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);
        String requestCredentials = ",\"credentialsType\": \"X509_CERTIFICATE\",\"hash\": \"testHash\"";
        byte[] result = createCoapClientAndPublish(requestCredentials).getPayload();
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

    private void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        super.processBeforeTest("Test Provision device", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, "testProvisionKey", "testProvisionSecret", DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);
        byte[] result = createCoapClientAndPublish().getPayload();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedTenant.getTenantId(), savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").getAsString());
    }

    private void processTestProvisioningWithBadKeyDevice() throws Exception {
        super.processBeforeTest("Test Provision device", CoapDeviceType.DEFAULT, TransportPayloadType.JSON, null, null, null, null, "testProvisionKeyOrig", "testProvisionSecret", DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);
        byte[] result = createCoapClientAndPublish().getPayload();
        JsonObject response = JsonUtils.parse(new String(result)).getAsJsonObject();
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").getAsString());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").getAsString());
    }

    private CoapResponse createCoapClientAndPublish() throws Exception {
        return createCoapClientAndPublish("");
    }

    private CoapResponse createCoapClientAndPublish(String deviceCredentials) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceCredentials);
        CoapClient client = getCoapClient(FeatureType.PROVISION);
        return postProvision(client, provisionRequestMsg.getBytes());
    }


    private CoapResponse postProvision(CoapClient client, byte[] payload) throws IOException, ConnectorException {
        return client.setTimeout((long) 60000).post(payload, MediaTypeRegistry.APPLICATION_JSON);
    }

    private String createTestProvisionMessage(String deviceCredentials) {
        return "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"" + deviceCredentials + "}";
    }
}
