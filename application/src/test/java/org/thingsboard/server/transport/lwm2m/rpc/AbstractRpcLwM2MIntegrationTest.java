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
package org.thingsboard.server.transport.lwm2m.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
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
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.controller.AbstractWebsocketTest;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapServers;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MServerBootstrap;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;
import org.thingsboard.server.transport.lwm2m.security.AbstractLwM2MIntegrationTest;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.COAP_CONFIG;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.HOST;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.HOST_BS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.PORT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.PORT_BS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_PORT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURE_PORT_BS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURITY;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SHORT_SERVER_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SHORT_SERVER_ID_BS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.objectId_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.objectInstanceId_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.objectInstanceId_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceIdName_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceIdName_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceIdName_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceIdName_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceId_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceId_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resourceId_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resources;

@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationTest extends AbstractWebsocketTest {

    protected final String RPC_TRANSPORT_CONFIGURATION = "{\n" +
            "  \"type\": \"LWM2M\",\n" +
            "  \"observeAttr\": {\n" +
            "    \"keyName\": {\n" +
            "      \"" + "/" + DEVICE + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\": \"" + resourceIdName_3_9 + "\",\n" +
            "      \"" + "/" + DEVICE + "/" + objectInstanceId_0 + "/" + resourceId_14 + "\": \"" + resourceIdName_3_14 + "\",\n" +
            "      \"" + "/" + BINARY_APP_DATA_CONTAINER + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\": \"" + resourceIdName_19_0_0 + "\",\n" +
            "      \"" + "/" + BINARY_APP_DATA_CONTAINER + "/" + objectInstanceId_1 + "/" + resourceId_0 + "\": \"" + resourceIdName_19_1_0 + "\"\n" +
            "    },\n" +
            "    \"observe\": [\n" +
            "      \"" + "/" + DEVICE + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\",\n" +
            "      \"" + "/" + BINARY_APP_DATA_CONTAINER + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\"\n" +
            "    ],\n" +
            "    \"attribute\": [\n" +
            "    ],\n" +
            "    \"telemetry\": [\n" +
            "      \"" + "/" + DEVICE + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\",\n" +
            "      \"" + "/" + DEVICE + "/" + objectInstanceId_0 + "/" + resourceId_14 + "\",\n" +
            "      \"" + "/" + BINARY_APP_DATA_CONTAINER + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\",\n" +
            "      \"" + "/" + BINARY_APP_DATA_CONTAINER + "/" + objectInstanceId_1 + "/" + resourceId_0 + "\"\n" +
            "    ],\n" +
            "    \"attributeLwm2m\": {}\n" +
            "  },\n" +
            "  \"bootstrap\": {\n" +
            "    \"servers\": {\n" +
            "      \"binding\": \"U\",\n" +
            "      \"shortId\": 123,\n" +
            "      \"lifetime\": 300,\n" +
            "      \"notifIfDisabled\": true,\n" +
            "      \"defaultMinPeriod\": 1\n" +
            "    },\n" +
            "    \"lwm2mServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5686,\n" +
            "      \"serverId\": 123,\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"bootstrapServerIs\": false,\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    },\n" +
            "    \"bootstrapServer\": {\n" +
            "      \"host\": \"localhost\",\n" +
            "      \"port\": 5687,\n" +
            "      \"serverId\": 111,\n" +
            "      \"securityMode\": \"NO_SEC\",\n" +
            "      \"serverPublicKey\": \"\",\n" +
            "      \"bootstrapServerIs\": true,\n" +
            "      \"clientHoldOffTime\": 1,\n" +
            "      \"bootstrapServerAccountTimeout\": 0\n" +
            "    }\n" +
            "  },\n" +
            "  \"clientLwM2mSettings\": {\n" +
            "    \"clientOnlyObserveAfterConnect\": 1,\n" +
            "    \"fwUpdateStrategy\": 1,\n" +
            "    \"swUpdateStrategy\": 1\n" +
            "  }\n" +
            "}";

    protected static final String ENDPOINT_RPC = "deviceEndpointRpc";
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;
    protected DeviceProfile deviceProfile;

    protected NoSecClientCredentials clientCredentials;
    protected LwM2MTestClient client;
    protected String deviceId;
    public Set expectedObjects;
    public Set expectedObjectIdVers;
    public Set expectedInstances;
    public Set expectedObjectIdVerInstances;

    protected String objectInstanceIdVer_1;
    protected String objectIdVer_0;
    protected String objectIdVer_2;
    private static final Predicate predicate_3 = path -> (!((String) path).contains("/" + TEMPERATURE_SENSOR) && ((String) path).contains("/" + DEVICE));
    protected String objectIdVer_3;
    protected String objectInstanceIdVer_3;
    protected String objectInstanceIdVer_5;
    protected String objectInstanceIdVer_9;
    protected String objectIdVer_19;
    protected String objectIdVer_50 = "/50";
    protected String objectIdVer_3303;

    public AbstractRpcLwM2MIntegrationTest(){ }

    @Before
    public void beforeTest() throws Exception {
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-lwm2m-rpc-scheduled"));
        loginTenantAdmin();
        wsClient = buildAndConnectWebSocketClient();
        createDeviceProfile(RPC_TRANSPORT_CONFIGURATION);
        clientCredentials = new NoSecClientCredentials();
        clientCredentials.setEndpoint(ENDPOINT_RPC);
        Device device = createDevice(clientCredentials);
        deviceId = device.getId().getId().toString();
        client = new LwM2MTestClient(executor, ENDPOINT_RPC);
        int clientPort = SocketUtils.findAvailableTcpPort();
        client.init(SECURITY, COAP_CONFIG, clientPort);
        for (String resourceName : resources) {
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
        expectedObjects = ConcurrentHashMap.newKeySet();
        expectedObjectIdVers = ConcurrentHashMap.newKeySet();
        expectedInstances = ConcurrentHashMap.newKeySet();
        expectedObjectIdVerInstances = ConcurrentHashMap.newKeySet();
        client.getClient().getObjectTree().getObjectEnablers().forEach((key, val) -> {
            if (key > 0) {
                String objectVerId = "/" + key;
                if (!val.getObjectModel().version.equals("1.0")) {
                    objectVerId += ("_" + val.getObjectModel().version);
                }
                expectedObjects.add("/" + key);
                expectedObjectIdVers.add(objectVerId);
                String finalObjectVerId = objectVerId;
                val.getAvailableInstanceIds().forEach(inststanceId -> {
                    expectedInstances.add("/" + key + "/" + inststanceId);
                    expectedObjectIdVerInstances.add(finalObjectVerId + "/" + inststanceId);
                });
            }
        });
        String ver_Id_0 = client.getClient().getObjectTree().getModel().getObjectModel(objectId_0).version;
        if ("1.0".equals(ver_Id_0)) {
            objectIdVer_0 = "/" + objectId_0;
        }
        else {
            objectIdVer_0 = "/" + objectId_0 + "_" + ver_Id_0;
        }
        objectIdVer_2 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).contains("/" + ACCESS_CONTROL)).findFirst().get();
        objectIdVer_3 = (String) expectedObjects.stream().filter(predicate_3).findFirst().get();
        objectIdVer_19 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).contains("/" + BINARY_APP_DATA_CONTAINER)).findFirst().get();
        objectIdVer_3303 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).contains("/" + TEMPERATURE_SENSOR)).findFirst().get();
        objectInstanceIdVer_1 = (String) expectedObjectIdVerInstances.stream().filter(path -> (!((String) path).contains("/" + BINARY_APP_DATA_CONTAINER) && ((String) path).contains("/" + SERVER))).findFirst().get();
        objectInstanceIdVer_3 = (String) expectedObjectIdVerInstances.stream().filter(predicate_3).findFirst().get();
        objectInstanceIdVer_5 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).contains("/" + FIRMWARE)).findFirst().get();
        objectInstanceIdVer_9 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).contains("/" + SOFTWARE_MANAGEMENT)).findFirst().get();
    }

    protected void createDeviceProfile(String transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();

        deviceProfile.setName("LwM2M_RPC");
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


    protected Device createDevice(LwM2MClientCredentials clientCredentials) throws Exception {
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
        LwM2MCredentials credentials = new LwM2MCredentials();
        credentials.setClient(clientCredentials);
        credentials.setBootstrap(createBootstrapConfig());
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    protected LwM2MBootstrapConfig createBootstrapConfig() {
        LwM2MBootstrapConfig bootstrap = new LwM2MBootstrapConfig();
        LwM2MBootstrapServers servers = new LwM2MBootstrapServers();
        servers.setShortId(SHORT_SERVER_ID);
        bootstrap.setServers(servers);
        LwM2MServerBootstrap server = new LwM2MServerBootstrap();
        server.setHost(HOST);
        server.setPort(PORT);
        server.setSecurityHost(HOST);
        server.setSecurityPort(SECURE_PORT);
        server.setServerId(servers.getShortId());
        server.setBootstrapServerIs(false);
        bootstrap.setLwm2mServer(server);
        LwM2MServerBootstrap serverBS = new LwM2MServerBootstrap();
        serverBS.setHost(HOST_BS);
        serverBS.setPort(PORT_BS);
        serverBS.setSecurityHost(HOST_BS);
        serverBS.setSecurityPort(SECURE_PORT_BS);
        serverBS.setServerId(SHORT_SERVER_ID_BS);
        serverBS.setBootstrapServerIs(true);
        bootstrap.setBootstrapServer(serverBS);
        return bootstrap;
    }

    protected String pathIdVerToObjectId(String pathIdVer) {
        if (pathIdVer.contains("_")){
            String [] objVer = pathIdVer.split("/");
            objVer[1] =  objVer[1].split("_")[0];
            return String.join("/",  objVer);
        }
        return pathIdVer;
    }

    @After
    public void after() {
        if (client != null) {
            client.destroy();
        }
        executor.shutdownNow();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
