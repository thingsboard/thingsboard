/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractCoapClientTest;
import org.thingsboard.server.msa.DisableUIListeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;


@DisableUIListeners
public class CoapClientTest extends AbstractCoapClientTest {

    private Device device;
    private String accessToken;
    private CoapClient coapClient;
    private static final long COAP_RESPONSE_TIMEOUT_MS = 5000;

    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
        accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        initCoapClient(accessToken, "", "");
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
        if (coapClient != null) {
            coapClient.shutdown();
        }
        disconnect();
    }

    private void initCoapClient(String token, String clientKeys, String sharedKeys) {
        StringBuilder uri = new StringBuilder("coap://localhost:5683/api/v1/").append(token).append("/attributes");
        if (!clientKeys.isEmpty() || !sharedKeys.isEmpty()) {
            uri.append("?");
            if (!clientKeys.isEmpty()) {
                uri.append("clientKeys=").append(clientKeys);
            }
            if (!sharedKeys.isEmpty()) {
                if (!clientKeys.isEmpty()) uri.append("&");
                uri.append("sharedKeys=").append(sharedKeys);
            }
        }
        this.coapClient = new CoapClient(uri.toString());
    }

    private JsonNode getAttributes(String clientKeys, String sharedKeys) throws Exception {
        initCoapClient(accessToken, clientKeys, sharedKeys);
        coapClient.setTimeout(COAP_RESPONSE_TIMEOUT_MS);
        CoapResponse response = coapClient.get();
        assertThat(response).isNotNull();
        assertThat(response.getCode().name()).isEqualTo("CONTENT");
        return mapper.readTree(response.getPayload());
    }

    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

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

    @Test
    public void getAllAttributes() throws Exception {
        JsonNode payload = mapper.readTree(createPayload().toString());
        testRestClient.postAttribute(accessToken, payload);
        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, payload);
        testRestClient.postTelemetry(accessToken, payload);

        JsonNode response = getAttributes("", "");
        assertThat(response.has("client")).isTrue();
        assertThat(response.has("shared")).isTrue();
        assertThat(response.get("client")).isEqualTo(payload);
        assertThat(response.get("shared")).isEqualTo(payload);
    }

    @Test
    public void getOnlyClientAttributes() throws Exception {
        JsonNode payload = mapper.readTree(createPayload().toString());
        testRestClient.postAttribute(accessToken, payload);
        testRestClient.postTelemetry(accessToken, payload);

        JsonNode response = getAttributes("boolKey,stringKey", "");
        assertThat(response.get("client").get("boolKey")).isEqualTo(payload.get("boolKey"));
        assertThat(response.get("client").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(response.has("shared")).isFalse();
    }

    @Test
    public void getOnlySharedAttributes() throws Exception {
        JsonNode payload = mapper.readTree(createPayload().toString());
        testRestClient.postAttribute(accessToken, payload);
        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, payload);

        JsonNode response = getAttributes("", "boolKey,stringKey");
        assertThat(response.get("shared").get("boolKey")).isEqualTo(payload.get("boolKey"));
        assertThat(response.get("shared").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(response.has("client")).isFalse();
    }
}