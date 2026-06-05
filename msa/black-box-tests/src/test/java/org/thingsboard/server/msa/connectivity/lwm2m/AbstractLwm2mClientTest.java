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
package org.thingsboard.server.msa.connectivity.lwm2m;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.util.Hex;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.PortFinder;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.connectivity.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.CLIENT_ENDPOINT_PSK;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.CLIENT_LWM2M_SETTINGS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.CLIENT_PSK_IDENTITY;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.CLIENT_PSK_KEY;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.OBSERVE_ATTRIBUTES_WITH_PARAMS;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_2;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.SECURE_URI;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.SECURITY_NO_SEC;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.resources;
import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.shortServerId;

@Slf4j
public class AbstractLwm2mClientTest extends AbstractContainerTest {
    protected ScheduledExecutorService executor;

    protected Security security;

    protected final PageLink pageLink = new PageLink(30);
    protected TenantId tenantId;

    public final Set<LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));

    public void createLwm2mDevicesForConnectNoSec(String name, Lwm2mDevicesForTest devicesForTest) throws Exception {
        String clientEndpoint = name + "-" + RandomStringUtils.randomAlphanumeric(7);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Device lwM2MDeviceTest = createDeviceWithCredentials(deviceCredentials, clientEndpoint, devicesForTest.getLwm2mDeviceProfile().getId());
        LwM2MTestClient lwM2MTestClient = createNewClient(SECURITY_NO_SEC, clientEndpoint, executor);
        devicesForTest.setLwM2MDeviceTest(lwM2MDeviceTest);
        devicesForTest.setLwM2MTestClient(lwM2MTestClient);
    }

    public void createLwm2mDevicesForConnectPsk(Lwm2mDevicesForTest devicesForTest) throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK + "-" + RandomStringUtils.randomAlphanumeric(7);
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecurePsk(clientCredentials);
        Device lwM2MDeviceTest = createDeviceWithCredentials(deviceCredentials, clientEndpoint, devicesForTest.getLwm2mDeviceProfile().getId());
        LwM2MTestClient lwM2MTestClient = createNewClient(security, clientEndpoint, executor);
        devicesForTest.setLwM2MDeviceTest(lwM2MDeviceTest);
        devicesForTest.setLwM2MTestClient(lwM2MTestClient);
    }

    /**
     * Observe {"id":"/3/0/0"}
     * Observe {"id":"/3/0/9"}
     * ObserveCancel {"id":"/3"} - Bad
     * ObserveCancel {"/3/0/0"} - Ok
     * ObserveCancelAl - Ok
     *
     * @param lwM2MTestClient
     * @param deviceId
     * @throws Exception
     */
    public void observeResource_Update_AfterUpdateRegistration_test(LwM2MTestClient lwM2MTestClient, DeviceId deviceId) throws Exception {
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 5);
        sendCancelObserveAllWithAwait(deviceId.toString());
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
        long tsBefore = Instant.now().toEpochMilli();
        String param = "/3_1.2/0/9";
        sendRpcObserveWithContainsLwM2mSingleResource(param, deviceId.toString(), 1);
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
        checkLatestTelemetryUploaded(deviceId, tsBefore, RESOURCE_ID_NAME_3_9);
        param = "/3_1.2/0/0";
        sendRpcObserveWithContainsLwM2mSingleResource(param, deviceId.toString(), 2);
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 2);
        param = "/3_1.2";
        String expected = "Could not find active Observe component with path: " + param;
        String actual = sendObserveCancel_BadRequest("ObserveCancel", param, deviceId.toString());
        assertEquals(expected, actual);
        param = "/3_1.2/0/0";
        sendObserveCancel_Ok("ObserveCancel", param, deviceId.toString());
        sendCancelObserveAllWithAwait(deviceId.toString());
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
    }

    public void observeCompositeResource_Update_AfterUpdateRegistration_test(LwM2MTestClient lwM2MTestClient, DeviceId deviceId) throws Exception {
        String id_3_0_9 = "/3/0/9";
        String id_3_0_14 = "/3/0/14";
        String id_19_0_0 = "/19/0/0";
        String id_19_1_0 = "/19/1/0";
        String id_19_0_2 = "/19/0/2";

        awaitUpdateRegistrationSuccess(lwM2MTestClient, 5);
        sendCancelObserveAllWithAwait(deviceId.toString());
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
        long tsBefore = Instant.now().toEpochMilli();
        String expectedKeys = "[\"" + RESOURCE_ID_NAME_3_9 + "\", \"" + RESOURCE_ID_NAME_3_14 + "\", \"" + RESOURCE_ID_NAME_19_0_0 + "\", \"" + RESOURCE_ID_NAME_19_0_2 + "\", \"" +RESOURCE_ID_NAME_19_1_0 + "\"]";
        String actualResult = sendRpcObserveCompositeWithResultValue(expectedKeys, deviceId.toString());
        assertTrue(actualResult.contains(id_3_0_9 + "=LwM2mSingleResource"));
        assertTrue(actualResult.contains(id_3_0_14 + "=LwM2mSingleResource"));
        assertTrue(actualResult.contains(id_19_0_0 + "=LwM2mMultipleResource"));
        assertTrue(actualResult.contains(id_19_1_0 + "=LwM2mMultipleResource"));
        assertTrue(actualResult.contains(id_19_0_2 + "=LwM2mSingleResource"));
        // ObserveComposite: - verify
        ObjectNode rpcActualResultBefore = sendRpcObserve("ObserveReadAll", null, deviceId.toString());
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultBefore.get("result").asText());
        JsonElement element = JsonParser.parseString(rpcActualResultBefore.get("value").asText());
        assertEquals(1, ((JsonArray) element).size());
        actualResult = ((JsonArray) element).asList().get(0).getAsString();
        assertTrue(actualResult.contains("CompositeObservation:"));
        checkLatestTelemetryUploaded(deviceId, tsBefore, RESOURCE_ID_NAME_3_9, RESOURCE_ID_NAME_3_14,
                RESOURCE_ID_NAME_19_0_0, RESOURCE_ID_NAME_19_0_2, RESOURCE_ID_NAME_19_1_0);

        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
        sendCancelObserveAllWithAwait(deviceId.toString());
        awaitUpdateRegistrationSuccess(lwM2MTestClient, 1);
        assertEquals(0, (Object) Optional.ofNullable(getCntObserveAll(deviceId.toString())).get());
    }


    public void basicTestConnection(LwM2MTestClient lwM2MTestClient, String alias) throws Exception {
        LwM2MClientState finishState = ON_REGISTRATION_SUCCESS;
        await(alias + " - " + ON_REGISTRATION_STARTED)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("msa basicTestConnection started -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED);
                });
        await(alias + " - " + ON_UPDATE_SUCCESS)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("msa basicTestConnection update -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        assertThat(lwM2MTestClient.getClientStates()).containsAll(expectedStatusesRegistrationLwm2mSuccess);

    }


    public LwM2MTestClient createNewClient(Security security,
                                           String endpoint, ScheduledExecutorService executor) throws Exception {
        this.executor = executor;
        LwM2MTestClient lwM2MTestClient = new LwM2MTestClient(executor, endpoint);
        int clientPort = PortFinder.findAvailableUdpPort();
        lwM2MTestClient.init(security, clientPort);
        return lwM2MTestClient;
    }

    protected void destroyAfter(Lwm2mDevicesForTest devicesForTest) {
        clientDestroy(devicesForTest.getLwM2MTestClient());
        deviceDestroy(devicesForTest.getLwM2MDeviceTest());
        deviceProfileDestroy(devicesForTest.getLwm2mDeviceProfile());
        if (executor != null) {
            executor.shutdown();
        }
    }

    protected void clientDestroy(LwM2MTestClient lwM2MTestClient) {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    protected void deviceDestroy(Device lwM2MDeviceTest) {
        try {
            if (lwM2MDeviceTest != null) {
                testRestClient.deleteDeviceIfExists(lwM2MDeviceTest.getId());
            }
        } catch (Exception e) {
            log.error("Failed device Delete", e);
        }
    }

    protected DeviceProfile initTest(String deviceProfileName) throws Exception {
        if (executor != null) {
            executor.shutdown();
        }
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-scheduled-" + deviceProfileName));

        DeviceProfile lwm2mDeviceProfile = getDeviceProfile(deviceProfileName);
        tenantId = lwm2mDeviceProfile.getTenantId();

        for (String resourceName : resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwm2mClientTest.class.getClassLoader().getResourceAsStream("lwm2m-registry/" + resourceName));
            lwModel.setData(bytes);
            testRestClient.postTbResourceIfNotExists(lwModel);
        }
        return lwm2mDeviceProfile;
    }

    protected DeviceProfile getDeviceProfile(String deviceProfileName) throws Exception {
        DeviceProfile deviceProfile = getDeviceProfileIfExists(deviceProfileName);
        if (deviceProfile == null) {
            deviceProfile = testRestClient.postDeviceProfile(createDeviceProfile(deviceProfileName));
        }
        return deviceProfile;
    }

    protected DeviceProfile getDeviceProfileIfExists(String deviceProfileName) throws Exception {
        return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                .filter(x -> x.getName().equals(deviceProfileName))
                .findFirst()
                .orElse(null);
    }


    protected DeviceProfile createDeviceProfile(String deviceProfileName) throws Exception {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(deviceProfileName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(getTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    protected void deviceProfileDestroy(DeviceProfile lwm2mDeviceProfile) {
        try {
            if (lwm2mDeviceProfile != null) {
                testRestClient.deleteDeviceProfileIfExists(lwm2mDeviceProfile);
            }
        } catch (Exception e) {
            log.error("Failed deviceProfile Delete", e);
        }
    }

    protected Device createDeviceWithCredentials(LwM2MDeviceCredentials deviceCredentials, String clientEndpoint, DeviceProfileId profileId) throws Exception {
        Device device = createDevice(deviceCredentials, clientEndpoint, profileId);
        return device;
    }

    protected Device createDevice(LwM2MDeviceCredentials credentials, String clientEndpoint, DeviceProfileId profileId) throws Exception {
        Device device = testRestClient.getDeviceByNameIfExists(clientEndpoint);
        if (device == null) {
            device = new Device();
            device.setName(clientEndpoint);
            device.setDeviceProfileId(profileId);
            device.setTenantId(tenantId);
            device = testRestClient.postDevice("", device);
        }

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        deviceCredentials = testRestClient.postDeviceCredentials(deviceCredentials);
        assertThat(deviceCredentials).isNotNull();
        return device;
    }

    protected LwM2MDeviceCredentials getDeviceCredentialsNoSec(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    public NoSecClientCredential createNoSecClientCredentials(String clientEndpoint) {
        NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        return clientCredentials;
    }

    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration() {
        List<LwM2MBootstrapServerCredential> bootstrapServerCredentials = new ArrayList<>();
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(OBSERVE_ATTRIBUTES_WITH_PARAMS, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    protected LwM2MDeviceCredentials getDeviceCredentialsSecurePsk(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials;
        bootstrapCredentials = getBootstrapClientCredentialsPsk(clientCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    private LwM2MBootstrapClientCredentials getBootstrapClientCredentialsPsk(LwM2MClientCredential clientCredentials) {
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        PSKBootstrapClientCredential serverCredentials = new PSKBootstrapClientCredential();
        if (clientCredentials != null) {
            serverCredentials.setClientSecretKey(((PSKClientCredential) clientCredentials).getKey());
            serverCredentials.setClientPublicKeyOrId(((PSKClientCredential) clientCredentials).getIdentity());
        }
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        return bootstrapCredentials;
    }

    protected String sendObserveCancel_BadRequest(String method, String params, String deviceIdStr) throws Exception {
        ObjectNode rpcActualResult = sendRpcObserve(method, params, deviceIdStr);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("error").asText();
    }

    protected void sendObserveCancel_Ok(String method, String params, String deviceIdStr) throws Exception {
        ObjectNode rpcActualResult = sendRpcObserve(method, params, deviceIdStr);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("1", rpcActualResult.get("value").asText());
    }

    protected void sendCancelObserveAllWithAwait(String deviceIdStr) throws Exception {
        ObjectNode rpcActualResultCancelAll = sendRpcObserve("ObserveCancelAll", null, deviceIdStr);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        awaitObserveReadAll(0, deviceIdStr);
    }

    protected void awaitObserveReadAll(int cntObserve, String deviceIdStr) throws Exception {
        await("ObserveReadAll after start client/test: countObserve " + cntObserve)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> cntObserve == getCntObserveAll(deviceIdStr));
    }

    protected void awaitUpdateRegistrationSuccess(LwM2MTestClient lwM2MTestClient, int cntUpdate) throws Exception {
        cntUpdate = cntUpdate + lwM2MTestClient.getCountUpdateRegistrationSuccess();
        int finalCntUpdate = cntUpdate;
        await("Update Registration client: countUpdateSuccess " + finalCntUpdate)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> finalCntUpdate <= lwM2MTestClient.getCountUpdateRegistrationSuccess());
    }

    protected void awaitObserveReadResource_3_0_9(int cntRead, String deviceIdStr) throws Exception {
        await("Read value 3/0/9 after start observe: countRead " + cntRead)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> cntRead == getCntObserveAll(deviceIdStr));
    }

    protected Integer getCntObserveAll(String deviceIdStr) throws Exception {
        ObjectNode rpcActualResultBefore = sendRpcObserve("ObserveReadAll", null, deviceIdStr);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultBefore.get("result").asText());
        JsonElement element = JsonParser.parseString(rpcActualResultBefore.get("value").asText());
        return element.isJsonArray() ? ((JsonArray) element).size() : null;
    }

    private void sendRpcObserveWithContainsLwM2mSingleResource(String params, String deviceIdStr, int cnt) throws Exception {
        String rpcActualResult = sendRpcObserveWithResultValue(params, deviceIdStr);
        assertTrue(rpcActualResult.contains("LwM2mSingleResource"));
        assertEquals(Optional.of(cnt).get(), Optional.ofNullable(getCntObserveAll(deviceIdStr)).get());
    }

    private String sendRpcObserveWithResultValue(String params, String deviceIdStr) throws Exception {
        ObjectNode rpcActualResult = sendRpcObserve("Observe", params, deviceIdStr);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("value").asText();
    }

    private String sendRpcObserveCompositeWithResultValue(String params, String deviceIdStr) throws Exception {
        ObjectNode rpcActualResult = sendRpcObserveComposite(params, deviceIdStr);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("value").asText();
    }

    protected ObjectNode sendRpcObserve(String method, String params, String deviceIdStr) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        } else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return testRestClient.postRpcLwm2mParams(deviceIdStr, sendRpcRequest);
    }

    protected ObjectNode sendRpcObserveComposite(String keys, String deviceIdStr) throws Exception {
        String method = "ObserveComposite";
        String sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"keys\":" + keys + "}}";
        return testRestClient.postRpcLwm2mParams(deviceIdStr, sendRpcRequest);
    }

    public void checkLatestTelemetryUploaded(DeviceId deviceId, long tsBefore, String... keyNames) throws Exception {
        WsClient wsClient = subscribeToWebSocket(deviceId, "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();
        Assert.assertEquals(actualLatestTelemetry.getData().size(), 1);
        String actualKeyName = actualLatestTelemetry.getLatestValues().keySet().toArray()[0].toString();
        Assert.assertTrue(Sets.newHashSet(keyNames).contains(actualKeyName));
        Assert.assertTrue((Long) actualLatestTelemetry.getDataValuesByKey(actualKeyName).get(0) > tsBefore);
    }
}
