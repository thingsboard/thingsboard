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
