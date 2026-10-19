// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractCoapClientTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class CoapClientTest extends AbstractCoapClientTest{
    private Device device;
    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
        disconnect();
    }

    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials deviceCreds = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(deviceCreds.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(deviceCreds.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        JsonNode attributes = testRestClient.getAttributes(device.getId(), AttributeScope.SERVER_SCOPE, "provisionState");
        assertThat(attributes.get(0).get("value").asText()).isEqualTo("provisioned");

        // provision second time should fail
        JsonNode provisionResponse2 = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));
        assertThat(provisionResponse2.get("status").asText()).isEqualTo("FAILURE");

        // update provision attribute to non-valid value
        testRestClient.postTelemetryAttribute(device.getId(), AttributeScope.SERVER_SCOPE.name(), JacksonUtil.valueToTree(Map.of("provisionState", "non-valid")));

        JsonNode provisionResponse3 = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));
        assertThat(provisionResponse3.get("status").asText()).isEqualTo("FAILURE");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(testDeviceName));

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(null));

        assertThat(response.get("status").asText()).isEqualTo("NOT_FOUND");
    }
}

