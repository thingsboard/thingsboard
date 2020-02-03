/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;


import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

public class HttpClientTest extends AbstractContainerTest {

    @Test
    public void telemetryUpload() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");

        Device device = createDevice("http_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        ResponseEntity deviceTelemetryResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/v1/{credentialsId}/telemetry",
                        mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        deviceCredentials.getCredentialsId());
        Assert.assertTrue(deviceTelemetryResponse.getStatusCode().is2xxSuccessful());
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        Assert.assertEquals(Sets.newHashSet("booleanKey", "stringKey", "doubleKey", "longKey"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", Long.toString(73)));

        restClient.deleteDevice(device.getId());
    }

    @Test
    public void getAttributes() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        TB_TOKEN = restClient.getToken();

        Device device = createDevice("test");
        String accessToken = restClient.getCredentials(device.getId()).getCredentialsId();
        assertNotNull(accessToken);

        ResponseEntity deviceSharedAttributes = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/" + DEVICE + "/" + device.getId().toString() + "/attributes/" + SHARED_SCOPE, mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        accessToken);

        Assert.assertTrue(deviceSharedAttributes.getStatusCode().is2xxSuccessful());

        ResponseEntity deviceClientsAttributes = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/v1/" + accessToken + "/attributes/", mapper.readTree(createPayload().toString()),
                        ResponseEntity.class,
                        accessToken);

        Assert.assertTrue(deviceClientsAttributes.getStatusCode().is2xxSuccessful());

        TimeUnit.SECONDS.sleep(3);

        Optional<JsonNode> allOptional = restClient.getAttributes(accessToken, null, null);
        assertTrue(allOptional.isPresent());


        JsonNode all = allOptional.get();
        assertEquals(2, all.size());
        assertEquals(mapper.readTree(createPayload().toString()), all.get("shared"));
        assertEquals(mapper.readTree(createPayload().toString()), all.get("client"));

        Optional<JsonNode> sharedOptional = restClient.getAttributes(accessToken, null, "stringKey");
        assertTrue(sharedOptional.isPresent());

        JsonNode shared = sharedOptional.get();
        assertEquals(shared.get("shared").get("stringKey"), mapper.readTree(createPayload().get("stringKey").toString()));
        assertFalse(shared.has("client"));

        Optional<JsonNode> clientOptional = restClient.getAttributes(accessToken, "longKey,stringKey", null);
        assertTrue(clientOptional.isPresent());

        JsonNode client = clientOptional.get();
        assertFalse(client.has("shared"));
        assertEquals(mapper.readTree(createPayload().get("longKey").toString()), client.get("client").get("longKey"));
        assertEquals(client.get("client").get("stringKey"), mapper.readTree(createPayload().get("stringKey").toString()));

        restClient.deleteDevice(device.getId());
    }
}
