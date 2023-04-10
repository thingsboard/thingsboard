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
package org.thingsboard.server.controller;

import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseTelemetryControllerTest extends AbstractControllerTest {

    @Test
    public void testConstraintValidatorCalledAtLeastOnce() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String requestBody = "{\"data\": \"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\"}";
        doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", requestBody, String.class, status().isOk());
        requestBody = "{\"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\": \"data\"}";
        doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", requestBody, String.class, status().isBadRequest());
    }

    private Device createDevice() throws Exception {
        String testToken = "TEST_TOKEN";

        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(testToken);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);

        Device savedDevice = readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
        return savedDevice;
    }
}
