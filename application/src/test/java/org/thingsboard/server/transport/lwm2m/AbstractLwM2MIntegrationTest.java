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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
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
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_ALL;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_BY_OBJECT;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.lwm2mClientResources;
import static org.thingsboard.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest.CLIENT_LWM2M_SETTINGS_19;

@TestPropertySource(properties = {
        "transport.lwm2m.enabled=true",
})
@Slf4j
@DaoSqlTest
public abstract class AbstractLwM2MIntegrationTest extends AbstractTransportIntegrationTest {

    @SpyBean
    protected LwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandlerTest;

    @SpyBean
    protected DefaultLwM2mUplinkMsgHandler defaultUplinkMsgHandlerTest;

    @Autowired
    private LwM2mClientContext clientContextTest;

    //  Lwm2m Server
    public static final int port = 5685;
    public static final int securityPort = 5686;
    public static final int portBs = 5687;
    public static final int securityPortBs = 5688;

    public static final String host = "localhost";
    public static final String hostBs = "localhost";
    public static final Integer shortServerId = 123;
    public static final Integer shortServerIdBs0 = 0;
    public static final int serverId = 1;
    public static final int serverIdBs = 0;

    public static final String COAP = "coap://";
    public static final String COAPS = "coaps://";
    public static final String URI = COAP + host + ":" + port;
    public static final String SECURE_URI = COAPS + host + ":" + securityPort;
    public static final String URI_BS = COAP + hostBs + ":" + portBs;
    public static final String SECURE_URI_BS = COAPS + hostBs + ":" + securityPortBs;
    public static final Security SECURITY_NO_SEC = noSec(URI, shortServerId);

    protected final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";
    public static  String TELEMETRY_WITHOUT_OBSERVE =
            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";
    public static  String TELEMETRY_WITH_ONE_OBSERVE =
            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/3_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static  String TELEMETRY_WITH_MANY_OBSERVE =
            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                    "      \"/3_1.2/0/20\": \"batteryStatus\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/3_1.2/0/20\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/3_1.2/0/20\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {},\n" +
                    "    \"observeStrategy\": 0\n" +
                    "  }";

    public static  String TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3 =
            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                    "      \"/5_1.2/0/3\": \"state\",\n" +
                    "      \"/5_1.2/0/5\": \"updateResult\",\n" +
                    "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                    "      \"/5_1.2/0/7\": \"pkgversion\",\n" +
                    "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/5_1.2/0/3\",\n" +
                    "      \"/5_1.2/0/5\",\n" +
                    "      \"/5_1.2/0/6\",\n" +
                    "      \"/5_1.2/0/7\",\n" +
                    "      \"/5_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {},\n" +
                    "    \"observeStrategy\": 0\n" +
                    "  }";

    public static  String TELEMETRY_WITH_COMPOSITE_ALL_OBSERVE_ID_3_ID_19 =
               "    {\n" +
                       "    \"keyName\": {\n" +
                       "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                       "      \"/3_1.2/0/20\": \"batteryStatus\",\n" +
                       "      \"/19_1.1/0/2\": \"dataCreationTime\"\n" +
                       "    },\n" +
                       "    \"observe\": [\n" +
                       "      \"/3_1.2/0/9\",\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attribute\": [],\n" +
                       "    \"telemetry\": [\n" +
                       "      \"/3_1.2/0/9\",\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attributeLwm2m\": {},\n" +
                       "    \"observeStrategy\": 1\n" +
                       "  }";

    public static  String TELEMETRY_WITH_COMPOSITE_BY_OBJECT_OBSERVE_ID_3_ID_5_ID_19 =
               "    {\n" +
                       "    \"keyName\": {\n" +
                       "      \"/3_1.2/0/20\": \"batteryStatus\",\n" +
                       "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                       "      \"/19_1.1/0/2\": \"dataCreationTime\"\n" +
                       "    },\n" +
                       "    \"observe\": [\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/5_1.2/0/6\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attribute\": [],\n" +
                       "    \"telemetry\": [\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/5_1.2/0/6\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attributeLwm2m\": {},\n" +
                       "    \"observeStrategy\": 2\n" +
                       "  }";

    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccessUpdate = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS, ON_UPDATE_STARTED, ON_UPDATE_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationBsSuccess = new HashSet<>(Arrays.asList(ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected ScheduledExecutorService executor;
    protected LwM2MTestClient lwM2MTestClient;
    private String[] resources = lwm2mClientResources;
    protected String deviceId;
    protected boolean supportFormatOnly_SenMLJSON_SenMLCBOR = false;

    @Before
    public void startInit() throws Exception {
        init();
    }

    @After
    public void after() throws Exception {
        this.clientDestroy(true);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void init() throws Exception {
        executor = ThingsBoardExecutors.newScheduledThreadPool(10, "test-lwm2m-scheduled");
        loginTenantAdmin();
        for (String resourceName : this.resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(bytes);
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
    }

    public void basicTestConnectionObserveSingleTelemetry(Security security,
                                                          LwM2MDeviceCredentials deviceCredentials,
                                                          String endpoint,
                                                          boolean queueMode,
                                                          boolean isUpdateProfile) throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_ONE_OBSERVE, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + endpoint, transportConfiguration);
        Device device = createLwm2mDevice(deviceCredentials, endpoint, deviceProfile.getId());

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        getWsClient().send(cmd);
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();
        this.createNewClient(security, null, false, endpoint, null, queueMode, device.getId().getId().toString(), null);
        awaitObserveReadAll(1, lwM2MTestClient.getDeviceIdStr());
        String msg = getWsClient().waitForUpdate();

        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        assertThat(Long.parseLong(tsValue.getValue()), instanceOf(Long.class));
        int expectedMax = 50;
        int expectedMin = 5;
        Assert.assertTrue(expectedMax >= Long.parseLong(tsValue.getValue()));
        Assert.assertTrue(expectedMin <= Long.parseLong(tsValue.getValue()));
        if (isUpdateProfile) {
            String actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            String expectedReadAll = "[\"SingleObservation:/3/0/9\"]";
            assertEquals(expectedReadAll, actualResultReadAll);

            updateProfile(deviceProfile, TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, SINGLE);
            awaitObserveReadAll(6, lwM2MTestClient.getDeviceIdStr());
            String expectedReadAll_3_9 = "\"SingleObservation:/3/0/9\"";
            String expectedReadAll_5_5 = "\"SingleObservation:/5/0/5\"";
            String expectedReadAll_5_6 = "\"SingleObservation:/5/0/6\"";
            String expectedReadAll_5_7 = "\"SingleObservation:/5/0/7\"";
            String expectedReadAll_5_9 = "\"SingleObservation:/5/0/9\"";
            actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_3_9));
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_5_5));
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_5_6));
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_5_7));
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_5_9));

            updateProfile(deviceProfile, TELEMETRY_WITH_MANY_OBSERVE, SINGLE);
            awaitObserveReadAll(2, lwM2MTestClient.getDeviceIdStr());
            String expectedReadAll_3_20 = "\"SingleObservation:/3/0/20\"";
            actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_3_9));
            assertTrue(expectedReadAll, actualResultReadAll.contains(expectedReadAll_3_20));
        }
    }

    public void basicTestConnectionObserveCompositeTelemetry(Security security,
                                                    LwM2MDeviceCredentials deviceCredentials,
                                                    String endpoint,
                                                    Lwm2mDeviceProfileTransportConfiguration transportConfiguration,
                                                    int cntObserve,
                                                    int varTest) throws Exception {

        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + endpoint, transportConfiguration);
        Device device = createLwm2mDevice(deviceCredentials, endpoint, deviceProfile.getId());

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));
        LatestValueCmd latestCmd = new LatestValueCmd();
        String key1 = "pkgname";
        String key2 = "pkgversion";
        String key3 = "batteryLevel";
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, key1)));
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, key2)));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(3, edq, null, latestCmd, null);
        getWsClient().send(cmd);
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();
        this.createNewClient(security, null, false, endpoint, null, true, device.getId().getId().toString(), null);
        awaitObserveReadAll(cntObserve, lwM2MTestClient.getDeviceIdStr());
        String msg = getWsClient().waitForUpdate();

        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(3, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue1 = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get(key1);
        var tsValue2 = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get(key2);
        var tsValue3 = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get(key3);
        var value = tsValue1 != null ? tsValue1.getValue() : tsValue2.getValue();
        if (tsValue3 != null) {
            assertThat(Long.parseLong(tsValue3.getValue()), instanceOf(Long.class));
            int expectedMax = 50;
            int expectedMin = 5;
            Assert.assertTrue(expectedMax >= Long.parseLong(tsValue3.getValue()));
            Assert.assertTrue(expectedMin <= Long.parseLong(tsValue3.getValue()));
        } else {
            assertNotNull(value);
        }

        String expectedReadAll;
        String actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
        if (varTest == 0) {
            expectedReadAll = "[\"CompositeObservation: [/5/0/9, /3/0/9, /5/0/5, /5/0/6, /5/0/7, /5/0/3]\"]";
            assertEquals(expectedReadAll, actualResultReadAll);
            updateProfile(deviceProfile, TELEMETRY_WITH_COMPOSITE_ALL_OBSERVE_ID_3_ID_19, COMPOSITE_ALL);
            awaitObserveReadAll(1, lwM2MTestClient.getDeviceIdStr());
            expectedReadAll = "[\"CompositeObservation: [/19/0/2, /3/0/20, /3/0/9]\"]";
            actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            assertEquals(expectedReadAll, actualResultReadAll);
        } else if (varTest == 1) {
            String expectedReadAll3 = "\"CompositeObservation: [/3/0/9]\"";
            String expectedReadAll5 = "\"CompositeObservation: [/5/0/9, /5/0/5, /5/0/6, /5/0/7, /5/0/3]\"";
            assertTrue(actualResultReadAll.contains(expectedReadAll3));
            assertTrue(actualResultReadAll.contains(expectedReadAll5));
            updateProfile(deviceProfile, TELEMETRY_WITH_COMPOSITE_BY_OBJECT_OBSERVE_ID_3_ID_5_ID_19, COMPOSITE_BY_OBJECT);
            awaitObserveReadAll(3, lwM2MTestClient.getDeviceIdStr());
            String expectedReadAll_3 = "\"CompositeObservation: [/3/0/20]\"";
            String expectedReadAll_5 = "\"CompositeObservation: [/3/0/20]\"";
            String expectedReadAll_19 = "\"CompositeObservation: [/19/0/2]\"";
            actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            assertTrue(actualResultReadAll.contains(expectedReadAll_3));
            assertTrue(actualResultReadAll.contains(expectedReadAll_5));
            assertTrue(actualResultReadAll.contains(expectedReadAll_19));
        } else if (varTest == 2) {
            String expectedReadAll3 = "\"CompositeObservation: [/3/0/9]\"";
            String expectedReadAll5 = "\"CompositeObservation: [/5/0/9, /5/0/5, /5/0/6, /5/0/7, /5/0/3]\"";
            assertTrue(actualResultReadAll.contains(expectedReadAll3));
            assertTrue(actualResultReadAll.contains(expectedReadAll5));
            updateProfile(deviceProfile, TELEMETRY_WITH_MANY_OBSERVE, SINGLE);
            awaitObserveReadAll(2, lwM2MTestClient.getDeviceIdStr());
            String expectedReadAll_3_9 = "\"SingleObservation:/3/0/9\"";
            String expectedReadAll_3_20 = "\"SingleObservation:/3/0/20\"";
            actualResultReadAll = sendRpcObserveOkWithResultValue("ObserveReadAll", null);
            assertTrue(actualResultReadAll.contains(expectedReadAll_3_9));
            assertTrue(actualResultReadAll.contains(expectedReadAll_3_20));
        }
    }

    protected DeviceProfile createLwm2mDeviceProfile(String name, Lwm2mDeviceProfileTransportConfiguration transportConfiguration) throws Exception {
        DeviceProfile lwm2mDeviceProfile = new DeviceProfile();
        lwm2mDeviceProfile.setName(name);
        lwm2mDeviceProfile.setType(DeviceProfileType.DEFAULT);
        lwm2mDeviceProfile.setTenantId(tenantId);
        lwm2mDeviceProfile.setTransportType(DeviceTransportType.LWM2M);
        lwm2mDeviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        lwm2mDeviceProfile.setDescription(name);

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        lwm2mDeviceProfile.setProfileData(deviceProfileData);

        lwm2mDeviceProfile = doPost("/api/deviceProfile", lwm2mDeviceProfile, DeviceProfile.class);
        Assert.assertNotNull(lwm2mDeviceProfile);
        return lwm2mDeviceProfile;
    }

    protected void updateProfile(DeviceProfile deviceProfile, String telemetryObserve, TelemetryObserveStrategy telemetryObserveStrategy)  throws Exception  {
        Lwm2mDeviceProfileTransportConfiguration  transportConfiguration = getTransportConfiguration(telemetryObserve, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(telemetryObserveStrategy);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/" + deviceProfile.getId().getId().toString(), DeviceProfile.class);
        foundDeviceProfile.getProfileData().setTransportConfiguration(transportConfiguration);
        doPost("/api/deviceProfile", foundDeviceProfile, DeviceProfile.class);
    }

    protected Device createLwm2mDevice(LwM2MDeviceCredentials credentials, String endpoint, DeviceProfileId deviceProfileId) throws Exception {
        Device device = new Device();
        device.setName(endpoint);
        device.setDeviceProfileId(deviceProfileId);
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
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
        if (this.resources == null || !Arrays.equals(this.resources, resources)) {
            this.resources = resources;
        }
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint, String deviceIdStr) throws Exception {
        this.createNewClient(security, securityBs, isRpc, endpoint, null, false, deviceIdStr, null);
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint, Integer clientDtlsCidLength, String deviceIdStr) throws Exception {
        this.createNewClient(security, securityBs, isRpc, endpoint, clientDtlsCidLength, false, deviceIdStr, null);
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint, Integer clientDtlsCidLength, boolean queueMode,
                                String deviceIdStr, Integer value3_0_9) throws Exception {
        this.clientDestroy(false);
        lwM2MTestClient = new LwM2MTestClient(this.executor, endpoint, resources);

        try (ServerSocket socket = new ServerSocket(0)) {
            int clientPort = socket.getLocalPort();
            lwM2MTestClient.init(security, securityBs, clientPort, isRpc,
                    this.defaultLwM2mUplinkMsgHandlerTest, this.clientContextTest,
                    clientDtlsCidLength, queueMode, supportFormatOnly_SenMLJSON_SenMLCBOR, value3_0_9);
        }
        lwM2MTestClient.setDeviceIdStr(deviceIdStr);
    }

    /**
     * Test: "/3/0/9" value = 44 (constant); count = 10; send from client to telemetry without observe
     * @param security
     * @param deviceCredentials
     * @param endpoint
     * @param queueMode
     * @throws Exception
     */
    public void testConnectionWithoutObserveWithDataReceivedSingleTelemetry(Security security,
                                                          LwM2MDeviceCredentials deviceCredentials,
                                                          String endpoint,
                                                          boolean queueMode) throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_ONE_OBSERVE, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + endpoint, transportConfiguration);
        Device device = createLwm2mDevice(deviceCredentials, endpoint, deviceProfile.getId());



        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        getWsClient().send(cmd);
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();

        this.createNewClient(security, null, false, endpoint, null, queueMode, device.getId().getId().toString(), 44);
        awaitObserveReadAll(1, lwM2MTestClient.getDeviceIdStr());

        LeshanClient leshanClient = lwM2MTestClient.getLeshanClient();
        Map<String, LwM2mServer> registeredServers = leshanClient.getRegisteredServers();
        List<String> paths = List.of("/3/0/9");
        int cntUpdate = 10;
        int cntLast = cntUpdate;
        for (final LwM2mServer server : registeredServers.values()) {
            log.info("Sending Data to {} using {}.", server, ContentFormat.SENML_CBOR);
            ResponseCallback<SendResponse> responseCallback = (response) -> {
                if (response.isSuccess())
                    log.warn("Data sent successfully to {} [{}].", server, response.getCode());
                else
                    log.warn("Send data to {} failed [{}] : {}.", server, response.getCode(),
                            response.getErrorMessage() == null ? "" : response.getErrorMessage());
            };
            ErrorCallback errorCallback = (e) -> log.warn("Unable to send data to {}.", server, e);
            while(cntLast > 0) {
                leshanClient.getSendService().sendData(server, ContentFormat.SENML_CBOR, paths,
                        2000, responseCallback, errorCallback);
                cntLast-- ;
            }
        }


        verify(defaultUplinkMsgHandlerTest, timeout(10000).atLeast(cntUpdate))
                .updateAttrTelemetry(Mockito.any(ResourceUpdateResult.class), eq(null));

        String msg = getWsClient().waitForUpdate();
        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        assertThat(Long.parseLong(tsValue.getValue()), instanceOf(Long.class));
        int expected = 44;
        assertEquals(expected, Long.parseLong(tsValue.getValue()));
    }


    private void clientDestroy(boolean isAfter) {
        try {
            if (lwM2MTestClient != null && lwM2MTestClient.getLeshanClient() != null) {
                if (isAfter) {
                    sendObserveCancelAllWithAwait(lwM2MTestClient.getDeviceIdStr());
                    awaitDeleteDevice(lwM2MTestClient.getDeviceIdStr());
                }
                lwM2MTestClient.destroy();
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration19(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS_19, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    protected List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(LwM2MProfileBootstrapConfigType bootstrapConfigType) {
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        switch (bootstrapConfigType) {
            case BOTH:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case BOOTSTRAP_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case LWM2M_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                break;
            case NONE:
        }
        return bootstrap;
    }

    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs0 : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
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


    protected void awaitObserveReadAll(int cntObserve, String deviceIdStr) throws Exception {
        try {
            await("ObserveReadAll: countObserve " + cntObserve)
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> cntObserve == getCntObserveAll(deviceIdStr));
        } catch (ConditionTimeoutException e) {
            int current = getCntObserveAll(deviceIdStr);
            log.error("Condition or device {} with alias 'ObserveReadAll: countObserve {}, but received {}", deviceIdStr, cntObserve, current);
            throw e;
        }
    }

    protected  void awaitDeleteDevice(String deviceIdStr) throws Exception {
        await("Delete device with id:  " + deviceIdStr)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    doDelete("/api/device/" + deviceIdStr)
                            .andExpect(status().isOk());
                   return HttpStatus.NOT_FOUND.value() == doGet("/api/device/" + deviceIdStr).andReturn().getResponse().getStatus();
                });
    }

    protected void updateRegAtLeastOnceAfterAction() {
        long initialInvocationCount = countUpdateReg();
        AtomicLong newInvocationCount = new AtomicLong(initialInvocationCount);
        log.trace("updateRegAtLeastOnceAfterAction: initialInvocationCount [{}]", initialInvocationCount);
        await("Update Registration at-least-once after action")
                .atMost(50, TimeUnit.SECONDS)
                .until(() -> {
                    newInvocationCount.set(countUpdateReg());
                    return newInvocationCount.get() > initialInvocationCount;
                });
        log.trace("updateRegAtLeastOnceAfterAction: newInvocationCount [{}]", newInvocationCount.get());
    }

    protected Integer getCntObserveAll(String deviceIdStr) throws Exception {
        String actualResult = sendObserveOK("ObserveReadAll", null, deviceIdStr);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        JsonElement element = JsonUtils.parse(rpcActualResult.get("value").asText());
        return element.isJsonArray() ? ((JsonArray)element).size() : null;
    }

    protected void sendObserveCancelAllWithAwait(String deviceIdStr) throws Exception {
        String actualResultCancelAll = sendObserveOK("ObserveCancelAll", null, deviceIdStr);
        ObjectNode rpcActualResultCancelAll = JacksonUtil.fromString(actualResultCancelAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        awaitObserveReadAll(0, lwM2MTestClient.getDeviceIdStr());
    }

    protected String sendRpcObserveOkWithResultValue(String method, String params) throws Exception {
        String actualResultReadAll = sendRpcObserveOk(method, params);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        return rpcActualResult.get("value").asText();
    }
    protected void sendRpcObserveWithContainsLwM2mSingleResource(String params) throws Exception {
        String rpcActualResult = sendRpcObserveOkWithResultValue("Observe", params);
        assertTrue(rpcActualResult.contains("LwM2mSingleResource") || rpcActualResult.contains("LwM2mMultipleResource"));
    }

    protected String sendRpcObserveOk(String method, String params) throws Exception {
        return sendObserveOK(method, params, lwM2MTestClient.getDeviceIdStr());
    }

    protected String sendObserveOK(String method, String params, String deviceIdStr) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        }
        else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceIdStr, sendRpcRequest, String.class, status().isOk());
    }

    protected ObjectNode sendRpcObserveWithResult(String method, String params) throws Exception {
        String actualResultReadAll = sendRpcObserveOk(method, params);
        return JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
    }

    protected long countUpdateReg() {
        return Mockito.mockingDetails(defaultUplinkMsgHandlerTest)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("updatedReg"))
                .count();
    }

    protected void awaitUpdateReg(int cntUpdate) {
        verify(defaultUplinkMsgHandlerTest, timeout(50000).atLeast(cntUpdate))
                .updatedReg(Mockito.any(Registration.class));
    }
}
