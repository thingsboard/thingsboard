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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.Rpc;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class RpcV2ApiClientTest extends AbstractApiClientTest {

    private static final String PERSISTENT_BODY =
            "{\"method\":\"setGpio\",\"params\":{\"pin\":7,\"value\":1},\"persistent\":true}";

    @Test
    public void testHandleOneWayDeviceRPCRequest() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = createNewDevice(TEST_PREFIX + ts);
        String deviceId = device.getId().getId().toString();

        try {
            client.handleOneWayDeviceRPCRequestV2(deviceId, PERSISTENT_BODY);
        } catch (ApiException e) {
            assertEquals("handleOneWayDeviceRPCRequest1 got an unexpected HTTP error: " + e.getCode(),
                    0, e.getCode());
        }

        client.deleteDevice(deviceId);
    }

    @Test
    public void testHandleTwoWayDeviceRPCRequest() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = createNewDevice(TEST_PREFIX + ts);
        String deviceId = device.getId().getId().toString();

        try {
            client.handleTwoWayDeviceRPCRequestV2(deviceId, PERSISTENT_BODY);
        } catch (ApiException e) {
            assertEquals("handleTwoWayDeviceRPCRequest1 got an unexpected HTTP error: " + e.getCode(),
                    0, e.getCode());
        }

        client.deleteDevice(deviceId);
    }

    @Test
    public void testGetPersistedRpcAndDeleteRpc() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = createNewDevice(TEST_PREFIX + ts);
        String deviceId = device.getId().getId().toString();

        String rpcId = postPersistentRpcAndGetId(deviceId);
        assertNotNull(rpcId);

        Rpc rpc = client.getPersistedRpc(rpcId);
        assertNotNull(rpc);
        assertNotNull(rpc.getId());

        client.deleteRpc(rpcId);

        assertReturns404(() -> client.getPersistedRpc(rpcId));

        client.deleteDevice(deviceId);
    }

    @Test
    public void testGetPersistedRpcNotFound() {
        assertReturns404(() -> client.getPersistedRpc(UUID.randomUUID().toString()));
    }

    @Test
    public void testGetPersistedRpcByDevice() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = createNewDevice(TEST_PREFIX + ts);
        String deviceId = device.getId().getId().toString();

        postPersistentRpcAndGetId(deviceId);

        try {
            client.getPersistedRpcByDevice(deviceId, 100, 0, null, null, null, null);
        } catch (ApiException e) {
            assertEquals("getPersistedRpcByDevice got an unexpected HTTP error: " + e.getCode(),
                    0, e.getCode());
        }

        client.deleteDevice(deviceId);
    }

    private Device createNewDevice(String name) throws ApiException {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        return client.saveDevice(device, null, null, null, null);
    }

    private String postPersistentRpcAndGetId(String deviceId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/plugins/rpc/oneway/" + deviceId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + client.getToken())
                .POST(HttpRequest.BodyPublishers.ofString(PERSISTENT_BODY))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return OBJECT_MAPPER.readTree(response.body()).get("rpcId").asText();
    }

}
