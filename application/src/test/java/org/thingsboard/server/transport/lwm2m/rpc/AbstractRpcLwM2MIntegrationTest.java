/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.junit.Before;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resources;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected String OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC;
    protected String deviceId;
    public Set expectedObjects;
    public Set expectedObjectIdVers;
    public Set expectedInstances;
    public Set expectedObjectIdVerInstances;

    protected String objectInstanceIdVer_1;
    protected String objectIdVer_0;
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
    protected String idVer_3_0_9;
    protected String idVer_19_0_0;

    public AbstractRpcLwM2MIntegrationTest() {
        setResources(resources);
    }

    @Before
    public void startInitRPC() throws Exception {
        initRpc();
    }

    private void initRpc () throws Exception {
        String endpoint = DEVICE_ENDPOINT_RPC_PREF + endpointSequence.incrementAndGet();
        createNewClient(SECURITY_NO_SEC, COAP_CONFIG, true, endpoint, false, null);
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
        objectIdVer_0 = "/" + OBJECT_ID_0 + "_" + ver_Id_0;
        objectIdVer_2 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + ACCESS_CONTROL)).findFirst().get();
        objectIdVer_3 = (String) expectedObjectIdVers.stream().filter(PREDICATE_3).findFirst().get();
        objectIdVer_19 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER)).findFirst().get();
        objectIdVer_3303 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + TEMPERATURE_SENSOR)).findFirst().get();
        objectInstanceIdVer_1 = (String) expectedObjectIdVerInstances.stream().filter(path -> (!((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER) && ((String) path).startsWith("/" + SERVER))).findFirst().get();
        objectInstanceIdVer_3 = (String) expectedObjectIdVerInstances.stream().filter(PREDICATE_3).findFirst().get();
        objectInstanceIdVer_5 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + FIRMWARE)).findFirst().get();
        objectInstanceIdVer_9 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + SOFTWARE_MANAGEMENT)).findFirst().get();

        idVer_3_0_9 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_9;
        idVer_19_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;

        OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC =
                "    {\n" +
                        "    \"keyName\": {\n" +
                        "      \"" + idVer_3_0_9 + "\": \"" + RESOURCE_ID_NAME_3_9 + "\",\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\": \"" + RESOURCE_ID_NAME_3_14 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\": \"" + RESOURCE_ID_NAME_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\": \"" + RESOURCE_ID_NAME_19_1_0 + "\"\n" +
                        "    },\n" +
                        "    \"observe\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attribute\": [\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\"\n" +
                        "    ],\n" +
                        "    \"telemetry\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attributeLwm2m\": {}\n" +
                        "  }";

        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);

        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(endpoint));
        final Device device = createDevice(deviceCredentials, endpoint);
        deviceId = device.getId().getId().toString();

        lwM2MTestClient.start(true);
        awaitObserveReadAll(2, false, device.getId().getId().toString());
    }

    protected String pathIdVerToObjectId(String pathIdVer) {
        if (pathIdVer.contains("_")) {
            String[] objVer = pathIdVer.split("/");
            objVer[1] = objVer[1].split("_")[0];
            return String.join("/", objVer);
        }
        return pathIdVer;
    }

}
