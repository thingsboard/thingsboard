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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.query.EntityKeyType.TIME_SERIES;

@DaoSqlTest
@TestPropertySource(properties = {
        "sql.attributes.value_no_xss_validation=true",
        "sql.ts.value_no_xss_validation=true"
})
public class TelemetryControllerTest extends AbstractControllerTest {

    @Test
    public void testConstraintValidator() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String correctRequestBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", correctRequestBody, String.class, status().isOk());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", correctRequestBody, String.class, status().isOk());
        String invalidRequestBody = "{\"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\": \"data\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());
    }

    @Test
    public void testDeleteAllTelemetryWithLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);
        var data = latest.get("data");
        Assert.assertNotNull(data);

        Assert.assertEquals("value", data.get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertTrue(latest.get("data").get(0).get("value").isNull());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    @Test
    public void testDeleteAllTelemetryWithoutLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true&deleteLatest=false", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    @Test
    public void testValueConstraintValidator() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String correctRequestBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", correctRequestBody, String.class, status().isOk());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", correctRequestBody, String.class, status().isOk());
        String invalidRequestBody = "{\"data\": \"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());
    }

    @Test
    public void testEmptyKeyIsProhibited() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String invalidRequestBody = "{\"\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());

        String invalidRequestBody2 = "{\" \": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody2, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody2, String.class, status().isBadRequest());
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

        return readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
    }
}
