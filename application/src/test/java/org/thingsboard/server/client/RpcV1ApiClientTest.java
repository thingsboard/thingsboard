// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
