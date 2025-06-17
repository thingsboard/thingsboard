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
import org.eclipse.californium.core.coap.CoAP;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractCoapClientTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;


@DisableUIListeners
public class CoapClientTest extends AbstractCoapClientTest {

    private static final String QUERY_PARAM_SEPARATOR = "?";
    private static final String PARAM_DELIMITER = "&";

    private Device device;
    private String accessToken;
    private CoapClient coapClient;
    private static final long COAP_RESPONSE_TIMEOUT_MS = 5000;

    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
        accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
        if (coapClient != null) {
            coapClient.shutdown();
        }
        disconnect();
    }

    private void initializeScopedCoapClient(String token, AttributeScope scope, String keys) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope must not be null for scoped requests.");
        }
        String uri = constructUri(token, "/attributes" + getScopePath(scope), keys, null);
        this.coapClient = new CoapClient(uri);
    }

    private void initializeUnscopedCoapClient(String token, String clientKeys, String sharedKeys) {
        String uri = constructUri(token, "/attributes", clientKeys, sharedKeys);
        this.coapClient = new CoapClient(uri);
    }

    private String constructUri(String token, String path, String clientKeys, String sharedKeys) {
        StringBuilder uri = new StringBuilder(COAP_BASE_URL).append(token).append(path);
        List<String> queryParams = new ArrayList<>();
        if (StringUtils.isNotEmpty(clientKeys)) {
            queryParams.add("clientKeys=" + clientKeys);
        }
        if (StringUtils.isNotEmpty(sharedKeys)) {
            queryParams.add("sharedKeys=" + sharedKeys);
        }
        if (!queryParams.isEmpty()) {
            uri.append(QUERY_PARAM_SEPARATOR).append(String.join(PARAM_DELIMITER, queryParams));
        }
        return uri.toString();
    }

    private String getScopePath(AttributeScope scope) {
        return switch (scope) {
            case CLIENT_SCOPE -> "/client";
            case SHARED_SCOPE -> "/shared";
            default -> throw new IllegalArgumentException("Invalid scope: " + scope);
        };
    }

    private JsonNode getAttributes(AttributeScope scope, String keys) throws Exception {
        initializeScopedCoapClient(accessToken, scope, keys);
        return executeCoapGet();
    }

    private JsonNode getAttributes(String clientKeys, String sharedKeys) throws Exception {
        initializeUnscopedCoapClient(accessToken, clientKeys, sharedKeys);
        return executeCoapGet();
    }

    private JsonNode executeCoapGet() throws Exception {
        coapClient.setTimeout(COAP_RESPONSE_TIMEOUT_MS);
        CoapResponse response = coapClient.get();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);

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

    private JsonNode prepareAndPostAttributes(String payloadString, boolean postToSharedScope) throws Exception {
        JsonNode payload = mapper.readTree(payloadString);
        testRestClient.postAttribute(accessToken, payload);
        testRestClient.postTelemetry(accessToken, payload);
        if (postToSharedScope) {
            testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, payload);
        }
        Thread.sleep(1000);
        return payload;
    }

    @Test
    public void testAllAttributes() throws Exception {
        JsonNode payload = prepareAndPostAttributes(createPayload().toString(), true);
        JsonNode attributesResponse = getAttributes("", "");

        assertThat(attributesResponse.has("client")).isTrue();
        assertThat(attributesResponse.has("shared")).isTrue();
        assertThat(attributesResponse.get("client")).isEqualTo(payload);
        assertThat(attributesResponse.get("shared")).isEqualTo(payload);
    }

    @Test
    public void testOnlyClientAttributes() throws Exception {
        JsonNode payload = prepareAndPostAttributes(createPayload().toString(), false);
        JsonNode attributesResponse = getAttributes("booleanKey,stringKey", "");

        assertThat(attributesResponse.get("client").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(attributesResponse.get("client").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(attributesResponse.has("shared")).isFalse();
    }

    @Test
    public void testOnlySharedAttributes() throws Exception {
        JsonNode payload = prepareAndPostAttributes(createPayload().toString(), true);
        JsonNode attributesResponse = getAttributes("", "booleanKey,stringKey");

        assertThat(attributesResponse.get("shared").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(attributesResponse.get("shared").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(attributesResponse.has("client")).isFalse();
    }

    @Test
    public void testClientAttributesUsingSeparatedEndpoints() throws Exception {
        JsonNode payload = prepareAndPostAttributes(createPayload().toString(), false);
        JsonNode attributesResponse = getAttributes(AttributeScope.CLIENT_SCOPE, "booleanKey,stringKey");

        assertThat(attributesResponse.get("client").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(attributesResponse.get("client").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(attributesResponse.has("shared")).isFalse();

        JsonNode allAttributesResponse = getAttributes(AttributeScope.CLIENT_SCOPE, null);
        assertThat(allAttributesResponse.get("client").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(allAttributesResponse.get("client").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(allAttributesResponse.has("shared")).isFalse();
    }

    @Test
    public void testSharedAttributesUsingSeparatedEndpoints() throws Exception {
        JsonNode payload = prepareAndPostAttributes(createPayload().toString(), true);
        JsonNode attributesResponse = getAttributes(AttributeScope.SHARED_SCOPE, "booleanKey,stringKey");

        assertThat(attributesResponse.get("shared").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(attributesResponse.get("shared").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(attributesResponse.has("client")).isFalse();

        JsonNode allAttributesResponse = getAttributes(AttributeScope.SHARED_SCOPE, null);
        assertThat(allAttributesResponse.get("shared").get("booleanKey")).isEqualTo(payload.get("booleanKey"));
        assertThat(allAttributesResponse.get("shared").get("stringKey")).isEqualTo(payload.get("stringKey"));
        assertThat(allAttributesResponse.has("client")).isFalse();
    }
}
