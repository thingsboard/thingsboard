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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.msa.connectivity.lwm2m.LwM2MTestClient;
import org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_ENDPOINT_NO_SEC;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_ENDPOINT_PSK;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_LWM2M_SETTINGS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_PSK_IDENTITY;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_PSK_KEY;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.OBSERVE_ATTRIBUTES_WITHOUT_PARAMS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.SECURE_URI;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.SECURITY_NO_SEC;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.resources;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.shortServerId;

@Slf4j
public class AbstractLwm2mClientTest extends AbstractContainerTest{
    protected ScheduledExecutorService executor;

    protected Security security;

    protected final PageLink pageLink = new PageLink(30);
    protected TenantId tenantId;
    protected DeviceProfile lwm2mDeviceProfile;
    protected Device lwM2MDeviceTest;
    protected LwM2MTestClient lwM2MTestClient;

    public final Set<LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));

    public void connectLwm2mClientNoSec() throws Exception {
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(CLIENT_ENDPOINT_NO_SEC));
        basicTestConnection(SECURITY_NO_SEC,
                deviceCredentials,
                CLIENT_ENDPOINT_NO_SEC,
                "TestConnection Lwm2m NoSec (msa)");
    }

    public void connectLwm2mClientPsk() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
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

        basicTestConnection(security,
                deviceCredentials,
                clientEndpoint,
                "TestConnection Lwm2m Rpc (msa)");
    }

    public void basicTestConnection(Security security,
                                    LwM2MDeviceCredentials deviceCredentials,
                                    String clientEndpoint, String alias) throws Exception {
        // create lwm2mClient and lwM2MDevice
        lwM2MDeviceTest = createDeviceWithCredentials(deviceCredentials, clientEndpoint);
        lwM2MTestClient = createNewClient(security, clientEndpoint, executor);

        LwM2MClientState finishState =  ON_REGISTRATION_SUCCESS;
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
        LwM2MTestClient lwM2MTestClient = new LwM2MTestClient(endpoint);
        try (ServerSocket socket = new ServerSocket(0)) {
            int clientPort = socket.getLocalPort();
            lwM2MTestClient.init(security, clientPort);
        }
        return lwM2MTestClient;
    }

    protected void destroyAfter(){
        clientDestroy();
        deviceDestroy();
        deviceProfileDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    protected void clientDestroy() {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }
    protected void deviceDestroy() {
        try {
            if (lwM2MDeviceTest != null) {
                testRestClient.deleteDeviceIfExists(lwM2MDeviceTest.getId());
            }
        } catch (Exception e) {
            log.error("Failed device Delete", e);
        }
    }

    protected void initTest(String deviceProfileName) throws Exception {
        if (executor != null) {
            executor.shutdown();
        }
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-scheduled-" + deviceProfileName));

        lwm2mDeviceProfile = getDeviceProfile(deviceProfileName);
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

    protected void deviceProfileDestroy(){
        try {
            if (lwm2mDeviceProfile != null) {
                testRestClient.deleteDeviceProfileIfExists(lwm2mDeviceProfile);
            }
        } catch (Exception e) {
            log.error("Failed deviceProfile Delete", e);
        }
    }

    protected Device createDeviceWithCredentials(LwM2MDeviceCredentials deviceCredentials, String clientEndpoint) throws Exception {
        Device device = createDevice(deviceCredentials, clientEndpoint);
        return device;
    }

    protected Device createDevice(LwM2MDeviceCredentials credentials, String clientEndpoint) throws Exception {
        Device device = testRestClient.getDeviceByNameIfExists(clientEndpoint);
        if (device == null) {
            device = new Device();
            device.setName(clientEndpoint);
            device.setDeviceProfileId(lwm2mDeviceProfile.getId());
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
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, TelemetryMappingConfiguration.class);
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
}
