// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.thingsboard.client.model.Device;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class DeviceConnectivityApiClientTest extends AbstractApiClientTest {

    @Test
    public void testGetDevicePublishTelemetryCommands() throws Exception {
        Device device = new Device();
        device.setName(TEST_PREFIX + System.currentTimeMillis());
        device.setType("default");

        Device savedDevice = client.saveDevice(device, null, null, null, null);
        String token = client.getDeviceCredentialsByDeviceId(savedDevice.getId().getId().toString()).getCredentialsId();

        String deviceId = savedDevice.getId().getId().toString();

        JsonNode commands = client.getDevicePublishTelemetryCommands(deviceId);
        assertEquals("curl -v -X POST http://localhost:8080/api/v1/" + token + "/telemetry --header Content-Type:application/json --data \"{temperature:25}\"", commands.get("http").get("http").asText());
        assertEquals("mosquitto_pub -d -q 1 -h localhost -p 1883 -t v1/devices/me/telemetry -u \"" + token + "\" -m \"{temperature:25}\"", commands.get("mqtt").get("mqtt").asText());
        assertEquals("coap-client -v 6 -m POST -t \"application/json\" -e \"{temperature:25}\" coap://localhost:5683/api/v1/" + token + "/telemetry", commands.get("coap").get("coap").asText());
    }

    @Test
    public void testGetDevicePublishTelemetryCommands_nonExistentDevice() {
        String nonExistentId = UUID.randomUUID().toString();
        assertReturns404(() -> client.getDevicePublishTelemetryCommands(nonExistentId));
    }

}
