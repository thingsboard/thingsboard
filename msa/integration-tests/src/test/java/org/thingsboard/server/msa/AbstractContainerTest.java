/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.thingsboard.client.tools.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractContainerTest {
    protected static String httpUrl;
    protected static String wsUrl;
    protected static RestClient restClient;
    protected ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void before() {
        httpUrl = "http://localhost:" + ContainerTestSuite.composeContainer.getServicePort("tb-web-ui1", ContainerTestSuite.EXPOSED_PORT);
        wsUrl = "ws://localhost:" + ContainerTestSuite.composeContainer.getServicePort("tb-web-ui1", ContainerTestSuite.EXPOSED_PORT);
        restClient = new RestClient(httpUrl);
    }

    protected Device createDevice(String name) {
        return restClient.createDevice(name + RandomStringUtils.randomAlphanumeric(7), "DEFAULT");
    }

    protected WsClient subscribeToTelemetryWebSocket(DeviceId deviceId) throws URISyntaxException, InterruptedException {
        WsClient mWs = new WsClient(new URI(wsUrl + "/api/ws/plugins/telemetry?token=" + restClient.getToken()));
        mWs.connectBlocking(1, TimeUnit.SECONDS);

        JsonObject tsSubCmd = new JsonObject();
        tsSubCmd.addProperty("entityType", EntityType.DEVICE.name());
        tsSubCmd.addProperty("entityId", deviceId.toString());
        tsSubCmd.addProperty("scope", "LATEST_TELEMETRY");
        tsSubCmd.addProperty("cmdId", new Random().nextInt(100));
        tsSubCmd.addProperty("unsubscribe", false);
        JsonArray wsTsSubCmds = new JsonArray();
        wsTsSubCmds.add(tsSubCmd);
        JsonObject wsRequest = new JsonObject();
        wsRequest.add("tsSubCmds", wsTsSubCmds);
        wsRequest.add("historyCmds", new JsonArray());
        wsRequest.add("attrSubCmds", new JsonArray());
        mWs.send(wsRequest.toString());
        return mWs;
    }

    protected Map<String, Long> getExpectedLatestValues(long ts) {
        return ImmutableMap.<String, Long>builder()
                .put("booleanKey", ts)
                .put("stringKey", ts)
                .put("doubleKey", ts)
                .put("longKey", ts)
                .build();
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, Long expectedTs, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedTs.equals(list.get(0)) && expectedValue.equals(list.get(1));
    }

    protected boolean verify(WsTelemetryResponse wsTelemetryResponse, String key, String expectedValue) {
        List<Object> list = wsTelemetryResponse.getDataValuesByKey(key);
        return expectedValue.equals(list.get(1));
    }

    protected JsonObject createPayload(long ts) {
        JsonObject values = createPayload();
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "value1");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.0);
        values.addProperty("longKey", 73L);

        return values;
    }

}
