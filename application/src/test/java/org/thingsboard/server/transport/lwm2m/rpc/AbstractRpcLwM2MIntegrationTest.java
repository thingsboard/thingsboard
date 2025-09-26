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
package org.thingsboard.server.transport.lwm2m.rpc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceUpdateResult;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_5700;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3303_12_5700;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.lwm2mClientResources;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

@Slf4j
@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected final LinkParser linkParser = new DefaultLwM2mLinkParser();
    protected String CONFIG_PROFILE_WITH_PARAMS_RPC;
    public Set expectedObjects;
    public Set expectedObjectIdVers;
    public Set expectedInstances;
    public Set expectedObjectIdVerInstances;

    protected String objectInstanceIdVer_1;
    protected String objectIdVer_0;
    protected String objectIdVer_1;
    protected String objectIdVer_2;
    private static final Predicate PREDICATE_3 = path -> (!((String) path).startsWith("/" + TEMPERATURE_SENSOR) && ((String) path).startsWith("/" + DEVICE));
    protected String objectIdVer_3;
    protected String objectInstanceIdVer_3;
    protected String objectInstanceIdVer_5;
    protected String objectInstanceIdVer_9;
    protected String objectIdVer_19;
    protected final String OBJECT_ID_VER_50 = "/50";
    protected String objectIdVer_3303;
    protected static AtomicInteger endpointSequence = new AtomicInteger();
    protected static String DEVICE_ENDPOINT_RPC_PREF = "deviceEndpointRpc";

    protected String idVer_3_0_0;
    protected String idVer_3_0_9;
    protected String id_3_0_9;

    protected String idVer_19_0_0;

    @SpyBean
    protected LwM2mTransportServerHelper lwM2mTransportServerHelperTest;

    public AbstractRpcLwM2MIntegrationTest() {
        setResources(lwm2mClientResources);
    }

    @Before
    public void startInitRPC() throws Exception {
        if (this.getClass().getSimpleName().equals("RpcLwm2mIntegrationWriteCborTest")) {
            supportFormatOnly_SenMLJSON_SenMLCBOR = true;
        }
        if (this.getClass().getSimpleName().contains("RpcLwm2mIntegrationObserve")) {
            initRpc(0);
        } else if (this.getClass().getSimpleName().equals("RpcLwm2mIntegrationReadCollectedValueTest")) {
            initRpc(3303);
        } else if (this.getClass().getSimpleName().equals("RpcLwm2mIntegrationInitReadCompositeAllTest")) {
            initRpc(2);
        }else if (this.getClass().getSimpleName().equals("RpcLwm2mIntegrationInitReadCompositeByObjectTest")) {
            initRpc(3);
        } else {
            initRpc(1);
        }
    }

    protected void initRpc(int typeConfigProfile) throws Exception {
        String endpoint = DEVICE_ENDPOINT_RPC_PREF + endpointSequence.incrementAndGet();
        createNewClient(SECURITY_NO_SEC, null, true, endpoint, null);
        expectedObjects = ConcurrentHashMap.newKeySet();
        expectedObjectIdVers = ConcurrentHashMap.newKeySet();
        expectedInstances = ConcurrentHashMap.newKeySet();
        expectedObjectIdVerInstances = ConcurrentHashMap.newKeySet();
        lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().forEach((key, val) -> {
            if (key > 0) {
                String objectVerId = "/" + key;
                objectVerId += ("_" + val.getObjectModel().version);
                expectedObjects.add("/" + key);
                expectedObjectIdVers.add(objectVerId);
                String finalObjectVerId = objectVerId;
                val.getAvailableInstanceIds().forEach(inststanceId -> {
                    expectedInstances.add("/" + key + "/" + inststanceId);
                    expectedObjectIdVerInstances.add(finalObjectVerId + "/" + inststanceId);
                });
            }
        });
        String ver_Id_0 = lwM2MTestClient.getLeshanClient().getObjectTree().getModel().getObjectModel(OBJECT_ID_0).version;
        String ver_Id_1 = lwM2MTestClient.getLeshanClient().getObjectTree().getModel().getObjectModel(OBJECT_ID_1).version;
        objectIdVer_0 = "/" + OBJECT_ID_0 + "_" + ver_Id_0;
        objectIdVer_1 = "/" + OBJECT_ID_1 + "_" + ver_Id_1;
        objectIdVer_2 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + ACCESS_CONTROL)).findFirst().get();
        objectIdVer_3 = (String) expectedObjectIdVers.stream().filter(PREDICATE_3).findFirst().get();
        objectIdVer_19 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER)).findFirst().get();
        objectIdVer_3303 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + TEMPERATURE_SENSOR)).findFirst().get();
        objectInstanceIdVer_1 = (String) expectedObjectIdVerInstances.stream().filter(path -> (!((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER) && ((String) path).startsWith("/" + SERVER))).findFirst().get();
        objectInstanceIdVer_3 = (String) expectedObjectIdVerInstances.stream().filter(PREDICATE_3).findFirst().get();
        objectInstanceIdVer_5 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + FIRMWARE)).findFirst().get();
        objectInstanceIdVer_9 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + SOFTWARE_MANAGEMENT)).findFirst().get();

        idVer_3_0_0 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;
        idVer_3_0_9 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_9;
        id_3_0_9 = fromVersionedIdToObjectId(idVer_3_0_9);
        idVer_19_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;

        String ATTRIBUTES_TELEMETRY_WITH_PARAMS_RPC_WITH_OBSERVE =
                "    {\n" +
                        "    \"keyName\": {\n" +
                        "      \"" + idVer_3_0_9 + "\": \"" + RESOURCE_ID_NAME_3_9 + "\",\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\": \"" + RESOURCE_ID_NAME_3_14 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\": \"" + RESOURCE_ID_NAME_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\": \"" + RESOURCE_ID_NAME_19_1_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2 + "\": \"" + RESOURCE_ID_NAME_19_0_2 + "\"\n" +
                        "    },\n" +
                        "    \"observe\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2 + "\"\n" +
                        "    ],\n" +
                        "    \"attribute\": [\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2 + "\"\n" +
                        "    ],\n" +
                        "    \"telemetry\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attributeLwm2m\": {}\n" +
                        "  }";

        String TELEMETRY_WITH_PARAMS_RPC_WITHOUT_OBSERVE =
                "    {\n" +
                        "    \"keyName\": {\n" +
                        "      \"" + idVer_3_0_9 + "\": \"" + RESOURCE_ID_NAME_3_9 + "\",\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\": \"" + RESOURCE_ID_NAME_3_14 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\": \"" + RESOURCE_ID_NAME_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\": \"" + RESOURCE_ID_NAME_19_1_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2 + "\": \"" + RESOURCE_ID_NAME_19_0_2 + "\"\n" +
                        "    },\n" +
                        "    \"observe\": [\n" +
                        "    ],\n" +
                        "    \"attribute\": [\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2 + "\"\n" +
                        "    ],\n" +
                        "    \"telemetry\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attributeLwm2m\": {}\n" +
                        "  }";
        String TELEMETRY_WITH_PARAMS_RPC_COLLECTED_VALUE =
                "    {\n" +
                        "    \"keyName\": {\n" +
                        "      \"" + objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12 + "/" + RESOURCE_ID_5700 + "\": \"" + RESOURCE_ID_NAME_3303_12_5700 + "\"\n" +
                        "    },\n" +
                        "    \"observe\": [\n" +
                        "    ],\n" +
                        "    \"attribute\": [\n" +
                        "    ],\n" +
                        "    \"telemetry\": [\n" +
                        "      \"" + objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12 + "/" + RESOURCE_ID_5700 + "\"\n" +
                        "    ],\n" +
                        "    \"attributeLwm2m\": {}\n" +
                        "  }";
       String INIT_READ_TELEMETRY_ATTRIBUTE_AS_OBSERVE_STRATEGY_ALL =
               "    {\n" +
                       "    \"keyName\": {\n" +
                       "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                       "      \"/3_1.2/0/20\": \"batteryStatus\",\n" +
                       "      \"/19_1.1/0/2\": \"dataCreationTime\",\n" +
                       "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                       "      \"/5_1.2/0/7\": \"pkgversion\",\n" +
                       "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\"\n" +
                       "    },\n" +
                       "    \"observe\": [\n" +
                       "      \"/3_1.2/0/20\"\n" +
                       "    ],\n" +
                       "    \"attribute\": [\n" +
                       "      \"/5_1.2/0/6\",\n" +
                       "      \"/5_1.2/0/7\"\n" +
                       "    ],\n" +
                       "    \"telemetry\": [\n" +
                       "      \"/3_1.2/0/9\",\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/5_1.2/0/9\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attributeLwm2m\": {},\n" +
                       "    \"initAttrTelAsObsStrategy\": true,\n" +
                       "    \"observeStrategy\": 1\n" +
                       "  }";
    String INIT_READ_TELEMETRY_ATTRIBUTE_AS_OBSERVE_STRATEGY_BY_OBJECT =
               "    {\n" +
                       "    \"keyName\": {\n" +
                       "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                       "      \"/3_1.2/0/20\": \"batteryStatus\",\n" +
                       "      \"/19_1.1/0/2\": \"dataCreationTime\",\n" +
                       "      \"/5_1.2/0/6\": \"pkgname\",\n" +
                       "      \"/5_1.2/0/7\": \"pkgversion\",\n" +
                       "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\"\n" +
                       "    },\n" +
                       "    \"observe\": [\n" +
                       "      \"/3_1.2/0/9\"\n" +
                       "    ],\n" +
                       "    \"attribute\": [\n" +
                       "      \"/5_1.2/0/6\",\n" +
                       "      \"/5_1.2/0/7\"\n" +
                       "    ],\n" +
                       "    \"telemetry\": [\n" +
                       "      \"/3_1.2/0/9\",\n" +
                       "      \"/3_1.2/0/20\",\n" +
                       "      \"/5_1.2/0/9\",\n" +
                       "      \"/19_1.1/0/2\"\n" +
                       "    ],\n" +
                       "    \"attributeLwm2m\": {},\n" +
                       "    \"initAttrTelAsObsStrategy\": true,\n" +
                       "    \"observeStrategy\": 2\n" +
                       "  }";

        CONFIG_PROFILE_WITH_PARAMS_RPC =
                switch (typeConfigProfile) {
                    case 0 -> ATTRIBUTES_TELEMETRY_WITH_PARAMS_RPC_WITH_OBSERVE;
                    case 1 -> TELEMETRY_WITH_PARAMS_RPC_WITHOUT_OBSERVE;
                    case 2 -> INIT_READ_TELEMETRY_ATTRIBUTE_AS_OBSERVE_STRATEGY_ALL;
                    case 3 -> INIT_READ_TELEMETRY_ATTRIBUTE_AS_OBSERVE_STRATEGY_BY_OBJECT;
                    case 3303 -> TELEMETRY_WITH_PARAMS_RPC_COLLECTED_VALUE;
                    default -> throw new IllegalStateException("Unexpected value: " + typeConfigProfile);
                };
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(CONFIG_PROFILE_WITH_PARAMS_RPC, getBootstrapServerCredentialsNoSec(NONE));
        DeviceProfile deviceProfile  = createLwm2mDeviceProfile("profileFor" + endpoint, transportConfiguration);

        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(endpoint));
        final Device device = createLwm2mDevice(deviceCredentials, endpoint, deviceProfile.getId());
        lwM2MTestClient.setDeviceIdStr(device.getId().getId().toString());
        lwM2MTestClient.start(true);
    }

    protected String pathIdVerToObjectId(String pathIdVer) {
        if (pathIdVer.contains("_")) {
            String[] objVer = pathIdVer.split("/");
            objVer[1] = objVer[1].split("_")[0];
            return String.join("/", objVer);
        }
        return pathIdVer;
    }

    protected long countUpdateAttrTelemetryAll() {
        return Mockito.mockingDetails(defaultUplinkMsgHandlerTest)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("updateAttrTelemetry"))
                .count();
    }

    protected long countUpdateAttrTelemetryResource(String idVerRez) {
        return Mockito.mockingDetails(defaultUplinkMsgHandlerTest)
                .getInvocations().stream()
                .filter(invocation ->
                        invocation.getMethod().getName().equals("updateAttrTelemetry") &&
                                invocation.getArguments().length > 1 &&
                                ((ResourceUpdateResult)invocation.getArguments()[0]).getPaths().toString().contains(idVerRez)
                )
                .count();
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

    protected long countSendParametersOnThingsboardTelemetryResource(String rezName) {
        return Mockito.mockingDetails(lwM2mTransportServerHelperTest)
                .getInvocations().stream()
                .filter(invocation ->
                        invocation.getMethod().getName().equals("sendParametersOnThingsboardTelemetry") &&
                                invocation.getArguments().length > 0 &&
                                invocation.getArguments()[0] instanceof List &&
                                ((List<?>) invocation.getArguments()[0]).stream()
                                        .filter(arg -> arg instanceof TransportProtos.KeyValueProto)
                                        .anyMatch(arg -> rezName.equals(((TransportProtos.KeyValueProto) arg).getKey()))
                )
                .count();
    }
}
