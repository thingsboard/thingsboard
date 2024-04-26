/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        long now = System.currentTimeMillis();

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, now, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.readTree(createPayload().toString()));

        String updateString = getWsClient().waitForUpdate(3000, true);
        EntityDataUpdate update = JacksonUtil.fromString(updateString, EntityDataUpdate.class);
        assertThat(update).isNotNull();
        assertThat(update.getUpdate()).isNotNull();
        assertThat(update.getUpdate().size()).isEqualTo(1);
        Map<String, TsValue[]> actualLatestTelemetry = update.getUpdate().get(0).getTimeseries();

        assertThat(actualLatestTelemetry.keySet()).containsOnlyOnceElementsOf(expectedKeys);
        assertThat(actualLatestTelemetry.get("booleanKey")[0].getValue()).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.get("stringKey")[0].getValue()).isEqualTo("value1");
        assertThat(actualLatestTelemetry.get("doubleKey")[0].getValue()).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.get("longKey")[0].getValue()).isEqualTo(Long.toString(73));
    }

    @Test
    public void getAttributes() throws Exception {
        String accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        assertThat(accessToken).isNotNull();

        JsonNode sharedAttribute = mapper.readTree(createPayload().toString());
        testRestClient.postTelemetryAttribute(DEVICE, device.getId(), SHARED_SCOPE, sharedAttribute);

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
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String status = provisionResponse.get("status");

        assertThat(status).isEqualTo("NOT_FOUND");
    }

}
