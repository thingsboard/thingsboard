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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
    public void testTelemetryRequests() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        var startTs = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        var endOfWeek1Ts = 1705269600000L;  // Monday, January 15, 2024 0:00:00 GMT+02:00
        var endOfWeek2Ts = 1705874400000L;  // Monday, January 22, 2024 0:00:00 GMT+02:00
        var endTs = endOfWeek2Ts + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1); // Monday, January 23, 2024 1:00:00 GMT+02:00

        var firstIntervalTs = startTs + (endOfWeek1Ts - startTs) / 2;
        var secondIntervalTs = endOfWeek1Ts + (endOfWeek2Ts - endOfWeek1Ts) / 2;
        var thirdIntervalTs = endOfWeek2Ts + (endTs - endOfWeek2Ts) / 2;

        var middleOfTheInterval = startTs + (endTs - startTs) / 2;

        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(1704899728000L, new LongDataEntry("t", 1L))); // Wednesday, January 10 15:15:28 GMT
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(1704899729000L, new LongDataEntry("t", 3L))); // Wednesday, January 10 15:15:29 GMT
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek1Ts, new LongDataEntry("t", 2L))); // Monday, January 15, 2024 0:00:00 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek1Ts + 1000, new LongDataEntry("t", 5L))); // Monday, January 15, 2024 0:00:01 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek2Ts, new LongDataEntry("t", 9L))); // Monday, January 22, 2024 0:00:00 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek2Ts + 1000, new LongDataEntry("t", 2L))); // Monday, January 22, 2024 0:00:01 GMT+02:00

        ObjectNode result = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() +
                        "/values/timeseries?keys=t&startTs={startTs}&endTs={endTs}&agg={agg}&intervalType={intervalType}&timeZone={timeZone}",
                ObjectNode.class, startTs, endTs, "SUM", "WEEK_ISO", "Europe/Kyiv");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.get("t"));
        Assert.assertEquals(3, result.get("t").size());

        var firstIntervalResult = result.get("t").get(0);
        Assert.assertEquals(4L, firstIntervalResult.get("value").asLong());
        Assert.assertEquals(firstIntervalTs, firstIntervalResult.get("ts").asLong());

        var secondIntervalResult = result.get("t").get(1);
        Assert.assertEquals(7L, secondIntervalResult.get("value").asLong());
        Assert.assertEquals(secondIntervalTs, secondIntervalResult.get("ts").asLong());

        var thirdIntervalResult = result.get("t").get(2);
        Assert.assertEquals(11L, thirdIntervalResult.get("value").asLong());
        Assert.assertEquals(thirdIntervalTs, thirdIntervalResult.get("ts").asLong());

        result = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() +
                        "/values/timeseries?keys=t&startTs={startTs}&endTs={endTs}&agg={agg}&intervalType={intervalType}&timeZone={timeZone}",
                ObjectNode.class, startTs, endTs, "SUM", "MONTH", "Europe/Kyiv");

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.get("t"));
        Assert.assertEquals(1, result.get("t").size());

        var monthResult = result.get("t").get(0);
        Assert.assertEquals(22L, monthResult.get("value").asLong());
        Assert.assertEquals(middleOfTheInterval, monthResult.get("ts").asLong());

        // get all latest (without keys parameter)
        ObjectNode allLatest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() +
                        "/values/timeseries?startTs={startTs}&endTs={endTs}&agg={agg}&intervalType={intervalType}&timeZone={timeZone}",
                ObjectNode.class, startTs, endTs, "SUM", "WEEK_ISO", "Europe/Kyiv");
        Assert.assertNotNull(allLatest);
        Assert.assertNotNull(allLatest.get("t"));
    }

    @Test
    public void testDeleteAllTelemetryWithLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));

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
        filter.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));

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
    public void testBadRequestReturnedWhenMethodArgumentTypeMismatch() throws Exception {
        loginTenantAdmin();
        String content = "{\"key\": \"value\"}";
        doPost("/api/plugins/telemetry/DEVICE/20b559f5-849f-4361-b4f6-b6d0b76687e9/INVALID_SCOPE", content, (String) null)
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentTypeMismatchException.class));
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

    @Test
    public void testDeleteTelemetryByKeyWithComma() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        String tsKey = "key1,key2";
        String testBody = JacksonUtil.newObjectNode()
                .put(tsKey, "value")
                .toString();
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", tsKey);
        params.add("deleteAllDataForKeys", "true");

        ObjectNode tsData = readResponse(doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries", params), ObjectNode.class);
        assertThat(tsData.get("key1,key2").get(0).get("value").asText()).isEqualTo("value");

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete", params);

        ObjectNode tsDataAfterDeletion = readResponse(doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries", params), ObjectNode.class);
        Assert.assertTrue(tsDataAfterDeletion.get("key1,key2").get(0).get("value").isNull());
    }

    @Test
    public void testDeleteTelemetryByKeysWithComma() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        String keyWithComma = "key1,key2";
        String testBody = JacksonUtil.newObjectNode()
                .put(keyWithComma, "value")
                .toString();
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        String key = "key3";
        String testBody2 = JacksonUtil.newObjectNode()
                .put(key, "value")
                .toString();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", keyWithComma);
        params.add("key", key);
        params.add("deleteAllDataForKeys", "true");

        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody2, String.class, status().isOk());

        ObjectNode tsData = readResponse(doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries", params), ObjectNode.class);
        assertThat(tsData.get("key1,key2").get(0).get("value").asText()).isEqualTo("value");
        assertThat(tsData.get("key3").get(0).get("value").asText()).isEqualTo("value");

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete", params);

        ObjectNode tsDataAfterDeletion = readResponse(doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries", params), ObjectNode.class);
        Assert.assertTrue(tsDataAfterDeletion.get("key1,key2").get(0).get("value").isNull());
        Assert.assertTrue(tsDataAfterDeletion.get("key3").get(0).get("value").isNull());
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
