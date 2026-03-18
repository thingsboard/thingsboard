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
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class RpcV1ApiClientTest extends AbstractApiClientTest {

    private static final String ONE_WAY_BODY =
            "{\"method\":\"setGpio\",\"params\":{\"pin\":7,\"value\":1},\"persistent\":true}";
    private static final String TWO_WAY_BODY =
            "{\"method\":\"getGpio\",\"params\":{\"pin\":7},\"persistent\":true}";

    @Test
    public void testHandleOneWayDeviceRPCRequest() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = createNewDevice(TEST_PREFIX + ts);
        String deviceId = device.getId().getId().toString();

        try {
            client.handleOneWayDeviceRPCRequestV1(deviceId, ONE_WAY_BODY);
        } catch (ApiException e) {
            assertEquals("handleOneWayDeviceRPCRequest got an unexpected HTTP error: " + e.getCode(),
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
            client.handleTwoWayDeviceRPCRequestV1(deviceId, TWO_WAY_BODY);
        } catch (ApiException e) {
            assertEquals("handleTwoWayDeviceRPCRequest got an unexpected HTTP error: " + e.getCode(),
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

}
