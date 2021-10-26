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

import org.junit.Before;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredentials;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.COAP_CONFIG;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURITY;
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
public abstract class AbstractRpcLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected String RPC_TRANSPORT_CONFIGURATION;

    protected final Set testListExpected = ConcurrentHashMap.newKeySet();
    protected Set testListActual;

    protected static final String ENDPOINT_RPC = "deviceEndpointRpc";
    protected ScheduledExecutorService executor;
    protected TbTestWebSocketClient wsClient;
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

    public AbstractRpcLwM2MIntegrationTest(){
        setResources(resources);
        setEndpoint(ENDPOINT_RPC);
    }

    @Before
    public void beforeTest() throws Exception {
        testListActual = ConcurrentHashMap.newKeySet();
        init();
        createNewClient (SECURITY, COAP_CONFIG, true);

        Thread.sleep(1000);

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

        RPC_TRANSPORT_CONFIGURATION = "{\n" +
                "  \"type\": \"LWM2M\",\n" +
                "  \"observeAttr\": {\n" +
                "    \"keyName\": {\n" +
                "      \""  + objectIdVer_3 + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\": \"" + resourceIdName_3_9 + "\",\n" +
                "      \"" + objectIdVer_3 + "/" + objectInstanceId_0 + "/" + resourceId_14 + "\": \"" + resourceIdName_3_14 + "\",\n" +
                "      \""  + objectIdVer_19 + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\": \"" + resourceIdName_19_0_0 + "\",\n" +
                "      \"" + objectIdVer_19 + "/" + objectInstanceId_1 + "/" + resourceId_0 + "\": \"" + resourceIdName_19_1_0 + "\"\n" +
                "    },\n" +
                "    \"observe\": [\n" +
                "      \"" + objectIdVer_3 + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\",\n" +
                "      \""  + objectIdVer_19 + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\"\n" +
                "    ],\n" +
                "    \"attribute\": [\n" +
                "    ],\n" +
                "    \"telemetry\": [\n" +
                "      \""  + objectIdVer_3 + "/" + objectInstanceId_0 + "/" + resourceId_9 + "\",\n" +
                "      \""  + objectIdVer_3 + "/" + objectInstanceId_0 + "/" + resourceId_14 + "\",\n" +
                "      \""  + objectIdVer_19 + "/" + objectInstanceId_0 + "/" + resourceId_0 + "\",\n" +
                "      \""  + objectIdVer_19 + "/" + objectInstanceId_1 + "/" + resourceId_0 + "\"\n" +
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
        createDeviceProfile(RPC_TRANSPORT_CONFIGURATION);

        Thread.sleep(1000);

        NoSecClientCredentials credentials =  createNoSecClientCredentials(ENDPOINT_RPC);
        final Device device = createDevice(credentials);
        deviceId = device.getId().getId().toString();

        Thread.sleep(1000);

        client.start();
     }

    protected String pathIdVerToObjectId(String pathIdVer) {
        if (pathIdVer.contains("_")){
            String [] objVer = pathIdVer.split("/");
            objVer[1] =  objVer[1].split("_")[0];
            return String.join("/",  objVer);
        }
        return pathIdVer;
    }

}
