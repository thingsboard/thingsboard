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
import org.thingsboard.client.model.AttributeData;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.TsData;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class TelemetryApiClientTest extends AbstractApiClientTest {

    @Test
    public void testTelemetryLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // create a device for telemetry operations
        Device device = new Device();
        device.setName("TelemetryTestDevice_" + timestamp);
        device.setType("default");
        Device createdDevice = client.saveDevice(device, null, null, null, null);
        assertNotNull(createdDevice);

        String entityType = "DEVICE";
        String entityId = createdDevice.getId().getId().toString();

        // save server-side attributes
        String serverAttributes = "{\"serverAttr1\": \"value1\", \"serverAttr2\": 42}";
        client.saveEntityAttributesV2(entityType, entityId, "SERVER_SCOPE", serverAttributes);

        // save shared attributes
        String sharedAttributes = "{\"sharedAttr1\": \"sharedValue1\", \"sharedAttr2\": true}";
        client.saveEntityAttributesV2(entityType, entityId, "SHARED_SCOPE", sharedAttributes);

        // get attribute keys
        List<String> allKeys = client.getAttributeKeys(entityType, entityId);
        assertNotNull(allKeys);
        assertTrue(allKeys.containsAll(List.of("serverAttr1", "serverAttr2", "sharedAttr1", "sharedAttr2")));

        // get attribute keys by scope
        List<String> serverKeys = client.getAttributeKeysByScope(entityType, entityId, "SERVER_SCOPE");
        assertEquals(2 + 1, serverKeys.size()); //active attribute is automatically added to server scope
        assertTrue(serverKeys.containsAll(List.of("serverAttr1", "serverAttr2", "active")));

        // get attributes by scope
        List<AttributeData> serverAttrs = client.getAttributesByScope(entityType, entityId, "SERVER_SCOPE", "serverAttr1,serverAttr2", null);
        assertNotNull(serverAttrs);
        assertEquals(2, serverAttrs.size());

        // get all attributes
        List<AttributeData> allAttrs = client.getAttributes(entityType, entityId, "serverAttr1,sharedAttr1", null);
        assertEquals(2, allAttrs.size());
        assertEquals("value1", allAttrs.stream().filter(attr -> attr.getKey().equals("serverAttr1")).findFirst().orElseThrow().getValue().toString());
        assertEquals("sharedValue1", allAttrs.stream().filter(attr -> attr.getKey().equals("sharedAttr1")).findFirst().orElseThrow().getValue().toString());

        // save timeseries data
        long ts1 = timestamp - 60000;
        long ts2 = timestamp - 30000;
        long ts3 = timestamp;
        String telemetryBody = "{\"ts\":" + ts1 + ",\"values\":{\"temperature\":25.5,\"humidity\":60}}";
        client.saveEntityTelemetry(entityType, entityId, "ANY", telemetryBody);

        String telemetryBody2 = "{\"ts\":" + ts2 + ",\"values\":{\"temperature\":26.0,\"humidity\":58}}";
        client.saveEntityTelemetry(entityType, entityId, "ANY", telemetryBody2);

        String telemetryBody3 = "{\"ts\":" + ts3 + ",\"values\":{\"temperature\":27.1,\"humidity\":55}}";
        client.saveEntityTelemetry(entityType, entityId, "ANY", telemetryBody3);

        // get timeseries keys
        List<String> tsKeys = client.getTimeseriesKeys(entityType, entityId);
        assertNotNull(tsKeys);
        assertEquals(2, tsKeys.size());
        assertTrue(tsKeys.containsAll(List.of("humidity", "temperature")));

        // get latest timeseries
        Map<String, List<TsData>> latestData = client.getLatestTimeseries(entityType, entityId, "temperature,humidity", false, null);
        assertNotNull(latestData);
        assertNotNull(latestData.get("temperature"));
        assertFalse(latestData.get("temperature").isEmpty());
        assertEquals("27.1", latestData.get("temperature").get(0).getValue().toString());

        // get timeseries history
        Map<String, List<TsData>> historyData = client.getTimeseriesHistory(
                entityType, entityId,
                ts1 - 1000, ts3 + 1000, "temperature",
                null, null, null, null, "NONE", "ASC", false, null);
        assertNotNull(historyData);
        List<TsData> tempHistory = historyData.get("temperature");
        assertNotNull(tempHistory);
        assertEquals(3, tempHistory.size());
        assertEquals("25.5", tempHistory.get(0).getValue().toString());
        assertEquals("27.1", tempHistory.get(2).getValue().toString());

        // delete timeseries
        client.deleteEntityTimeseries(entityType, entityId, "humidity", true, null, null, true, false, null);

        List<String> keysAfterDelete = client.getTimeseriesKeys(entityType, entityId);
        assertFalse(keysAfterDelete.contains("humidity"));

        // delete attributes
        client.deleteEntityAttributes(entityType, entityId, "SERVER_SCOPE", "serverAttr1", null);

        List<String> serverKeysAfterDelete = client.getAttributeKeysByScope(entityType, entityId, "SERVER_SCOPE");
        assertFalse(serverKeysAfterDelete.contains("serverAttr1"));
        assertTrue(serverKeysAfterDelete.contains("serverAttr2"));

        // save device attributes using device-specific endpoint
        client.saveDeviceAttributes(entityId, "SERVER_SCOPE", "{\"deviceSpecificAttr\": \"test\"}");

        List<String> deviceKeys = client.getAttributeKeysByScope(entityType, entityId, "SERVER_SCOPE");
        assertTrue(deviceKeys.contains("deviceSpecificAttr"));

        // delete device attributes
        client.deleteDeviceAttributes(entityId, "SERVER_SCOPE", "deviceSpecificAttr", null);

        List<String> deviceKeysAfterDelete = client.getAttributeKeysByScope(entityType, entityId, "SERVER_SCOPE");
        assertFalse(deviceKeysAfterDelete.contains("deviceSpecificAttr"));

        // save telemetry with TTL
        String ttlTelemetry = "{\"ts\":" + timestamp + ",\"values\":{\"shortLived\":99}}";
        client.saveEntityTelemetryWithTTL(entityType, entityId, "ANY", 86400L, ttlTelemetry);

        Map<String, List<TsData>> latestWithTtl = client.getLatestTimeseries(entityType, entityId, "shortLived", false, null);
        assertNotNull(latestWithTtl.get("shortLived"));
        assertEquals("99", latestWithTtl.get("shortLived").get(0).getValue().toString());
    }

}
