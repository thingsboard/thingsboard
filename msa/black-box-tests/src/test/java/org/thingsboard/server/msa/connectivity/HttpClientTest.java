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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class HttpClientTest extends AbstractContainerTest {
    private Device device;
    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    @Test
    public void telemetryUpload() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.readTree(createPayload().toString()));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    public void getAttributes() throws Exception {
        String accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        assertThat(accessToken).isNotNull();

        JsonNode sharedAttribute = mapper.readTree(createPayload().toString());
        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, sharedAttribute);

        JsonNode clientAttribute = mapper.readTree(createPayload().toString());
        testRestClient.postAttribute(accessToken, clientAttribute);

        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        JsonNode attributes = testRestClient.getAttributes(accessToken, null, null);
        assertThat(attributes.get("shared")).isEqualTo(sharedAttribute);
        assertThat(attributes.get("client")).isEqualTo(clientAttribute);

        JsonNode attributes2 = testRestClient.getAttributes(accessToken, null, "stringKey");
        assertThat(attributes2.get("shared").get("stringKey")).isEqualTo(sharedAttribute.get("stringKey"));
        assertThat(attributes2.has("client")).isFalse();

        JsonNode attributes3 =  testRestClient.getAttributes(accessToken, "longKey,stringKey", null);

        assertThat(attributes3.has("shared")).isFalse();
        assertThat(attributes3.get("client").get("longKey")).isEqualTo(clientAttribute.get("longKey"));
        assertThat(attributes3.get("client").get("stringKey")).isEqualTo(clientAttribute.get("stringKey"));
    }

    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", device.getName());

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String credentialsType = provisionResponse.get("credentialsType");
        String credentialsValue = provisionResponse.get("credentialsValue");
        String status = provisionResponse.get("status");

        assertThat(credentialsType).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(credentialsValue).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(status).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", testDeviceName);

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String credentialsType = provisionResponse.get("credentialsType");
        String credentialsValue = provisionResponse.get("credentialsValue");
        String status = provisionResponse.get("status");

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(credentialsType).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(credentialsValue).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(status).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForGatewayDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", testDeviceName);
        provisionRequest.addProperty("gateway", true);

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String credentialsType = provisionResponse.get("credentialsType");
        String credentialsValue = provisionResponse.get("credentialsValue");
        String status = provisionResponse.get("status");

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        JsonNode additionalInfo = device.getAdditionalInfo();

        assertThat(additionalInfo).isNotNull();
        assertThat(additionalInfo.has(DataConstants.GATEWAY_PARAMETER)
                && additionalInfo.get(DataConstants.GATEWAY_PARAMETER).isBoolean()).isTrue();
        assertThat(additionalInfo.get(DataConstants.GATEWAY_PARAMETER).asBoolean()).isTrue();

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(credentialsType).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(credentialsValue).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(status).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String status = provisionResponse.get("status");

        assertThat(status).isEqualTo("NOT_FOUND");
    }

}
