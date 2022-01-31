/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.object.Security;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.util.SocketUtils;
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
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.controller.AbstractWebsocketTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.telemetry.cmd.TelemetryPluginCmdsWrapper;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public abstract class AbstractLwM2MIntegrationTest extends AbstractWebsocketTest {

    protected final String TRANSPORT_CONFIGURATION = "{\n" +
            "  \"type\": \"LWM2M\",\n" +
            "  \"observeAttr\": {\n" +
            "    \"keyName\": {\n" +
            "      \"/3_1.0/0/9\": \"batteryLevel\"\n" +
            "    },\n" +
            "    \"observe\": [],\n" +
            "    \"attribute\": [\n" +
            "    ],\n" +
            "    \"telemetry\": [\n" +
            "      \"/3_1.0/0/9\"\n" +
            "    ],\n" +
            "    \"attributeLwm2m\": {}\n" +
            "  },\n" +
            "  \"bootstrapServerUpdateEnable\": true,\n" +
            "  \"bootstrap\": [\n" +
            "    {\n" +
            "       \"host\": \"0.0.0.0\",\n" +
            "       \"port\": 5687,\n" +
            "       \"binding\": \"U\",\n" +
            "       \"lifetime\": 300,\n" +
            "       \"securityMode\": \"NO_SEC\",\n" +
            "       \"shortServerId\": 111,\n" +
            "       \"notifIfDisabled\": true,\n" +
            "       \"serverPublicKey\": \"\",\n" +
            "       \"defaultMinPeriod\": 1,\n" +
            "       \"bootstrapServerIs\": true,\n" +
            "       \"clientHoldOffTime\": 1,\n" +
            "       \"bootstrapServerAccountTimeout\": 0\n" +
            "    },\n" +
            "    {\n" +
            "       \"host\": \"0.0.0.0\",\n" +
            "       \"port\": 5685,\n" +
            "       \"binding\": \"U\",\n" +
            "       \"lifetime\": 300,\n" +
            "       \"securityMode\": \"NO_SEC\",\n" +
            "       \"shortServerId\": 123,\n" +
            "       \"notifIfDisabled\": true,\n" +
            "       \"serverPublicKey\": \"\",\n" +
            "       \"defaultMinPeriod\": 1,\n" +
            "       \"bootstrapServerIs\": false,\n" +
            "       \"clientHoldOffTime\": 1,\n" +
            "       \"bootstrapServerAccountTimeout\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"clientLwM2mSettings\": {\n" +
            "    \"edrxCycle\": null,\n" +
            "    \"powerMode\": \"DRX\",\n" +
            "    \"fwUpdateResource\": null,\n" +
            "    \"fwUpdateStrategy\": 1,\n" +
            "    \"psmActivityTimer\": null,\n" +
            "    \"swUpdateResource\": null,\n" +
            "    \"swUpdateStrategy\": 1,\n" +
            "    \"pagingTransmissionWindow\": null,\n" +
            "    \"clientOnlyObserveAfterConnect\": 1\n" +
            "  }\n" +
            "}";

    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;
    protected LwM2MTestClient client;
    private final LwM2MBootstrapClientCredentials defaultBootstrapCredentials;
    private String[] resources;

    public AbstractLwM2MIntegrationTest() {
        this.defaultBootstrapCredentials = new LwM2MBootstrapClientCredentials();
        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        this.defaultBootstrapCredentials.setBootstrapServer(serverCredentials);
        this.defaultBootstrapCredentials.setLwm2mServer(serverCredentials);
    }

    public void init () throws Exception{
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-lwm2m-scheduled"));
        loginTenantAdmin();

        for (String resourceName : this.resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(Base64.getEncoder().encodeToString(bytes));
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
        wsClient = buildAndConnectWebSocketClient();
    }

    @Before
    public void beforeTest() throws Exception {
        this.init();
    }

    @After
    public void after() {
        wsClient.close();
        clientDestroy();
        executor.shutdownNow();
    }

    public void basicTestConnectionObserveTelemetry(Security security,
                                                    LwM2MClientCredential credentials,
                                                    Configuration coapConfig,
                                                    String endpoint) throws Exception {
        createDeviceProfile(TRANSPORT_CONFIGURATION);
        Device device = createDevice(credentials);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));

        wsClient.send(JacksonUtil.toString(wrapper));
        wsClient.waitForReply();

        wsClient.registerWaitForUpdate();
        createNewClient(security, coapConfig, false, endpoint);
        String msg = wsClient.waitForUpdate();

        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertEquals(42, Long.parseLong(tsValue.getValue()));
    }

    protected void createDeviceProfile(String transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();
        deviceProfile.setName("LwM2M");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(JacksonUtil.fromString(transportConfiguration, Lwm2mDeviceProfileTransportConfiguration.class));
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    protected Device createDevice(LwM2MClientCredential clientCredentials) throws Exception {
        Device device = new Device();
        device.setName("Device A");
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);

        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        credentials.setBootstrap(defaultBootstrapCredentials);

        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    public NoSecClientCredential createNoSecClientCredentials(String endpoint) {
        NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(endpoint);
        return clientCredentials;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }

    public void createNewClient(Security security, Configuration coapConfig, boolean isRpc, String endpoint) throws Exception {
        clientDestroy();
        client = new LwM2MTestClient(this.executor, endpoint);
        int clientPort = SocketUtils.findAvailableTcpPort();
        client.init(security, coapConfig, clientPort, isRpc);
    }

    private void clientDestroy() {
        if (client != null) {
            client.destroy();
        }
    }
}
