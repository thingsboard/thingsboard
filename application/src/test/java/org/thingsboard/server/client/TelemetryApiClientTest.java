// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteDeviceAttributesArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteEntityAttributesArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteEntityTimeseriesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAttributeKeysArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAttributeKeysByScopeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAttributesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAttributesByScopeArgs;
import org.thingsboard.client.api.ThingsboardApi.GetLatestTimeseriesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTimeseriesHistoryArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTimeseriesKeysArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceAttributesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityAttributesV2Args;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityTelemetryArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityTelemetryWithTTLArgs;
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
        Device createdDevice = client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());
        assertNotNull(createdDevice);

        String entityType = "DEVICE";
        String entityId = createdDevice.getId().getId().toString();

        // save server-side attributes
        String serverAttributes = "{\"serverAttr1\": \"value1\", \"serverAttr2\": 42}";
        client.saveEntityAttributesV2(SaveEntityAttributesV2Args.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .body(serverAttributes)
                .build());

        // save shared attributes
        String sharedAttributes = "{\"sharedAttr1\": \"sharedValue1\", \"sharedAttr2\": true}";
        client.saveEntityAttributesV2(SaveEntityAttributesV2Args.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SHARED_SCOPE")
                .body(sharedAttributes)
                .build());

        // get attribute keys
        List<String> allKeys = client.getAttributeKeys(GetAttributeKeysArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .build());
        assertNotNull(allKeys);
        assertTrue(allKeys.containsAll(List.of("serverAttr1", "serverAttr2", "sharedAttr1", "sharedAttr2")));

        // get attribute keys by scope
        List<String> serverKeys = client.getAttributeKeysByScope(GetAttributeKeysByScopeArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .build());
        assertEquals(2 + 1, serverKeys.size()); //active attribute is automatically added to server scope
        assertTrue(serverKeys.containsAll(List.of("serverAttr1", "serverAttr2", "active")));

        // get attributes by scope
        List<AttributeData> serverAttrs = client.getAttributesByScope(GetAttributesByScopeArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .keys("serverAttr1,serverAttr2")
                .build());
        assertNotNull(serverAttrs);
        assertEquals(2, serverAttrs.size());

        // get all attributes
        List<AttributeData> allAttrs = client.getAttributes(GetAttributesArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .keys("serverAttr1,sharedAttr1")
                .build());
        assertEquals(2, allAttrs.size());
        assertEquals("value1", allAttrs.stream().filter(attr -> attr.getKey().equals("serverAttr1")).findFirst().orElseThrow().getValue().toString());
        assertEquals("sharedValue1", allAttrs.stream().filter(attr -> attr.getKey().equals("sharedAttr1")).findFirst().orElseThrow().getValue().toString());

        // save timeseries data
        long ts1 = timestamp - 60000;
        long ts2 = timestamp - 30000;
        long ts3 = timestamp;
        String telemetryBody = "{\"ts\":" + ts1 + ",\"values\":{\"temperature\":25.5,\"humidity\":60}}";
        client.saveEntityTelemetry(SaveEntityTelemetryArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("ANY")
                .body(telemetryBody)
                .build());

        String telemetryBody2 = "{\"ts\":" + ts2 + ",\"values\":{\"temperature\":26.0,\"humidity\":58}}";
        client.saveEntityTelemetry(SaveEntityTelemetryArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("ANY")
                .body(telemetryBody2)
                .build());

        String telemetryBody3 = "{\"ts\":" + ts3 + ",\"values\":{\"temperature\":27.1,\"humidity\":55}}";
        client.saveEntityTelemetry(SaveEntityTelemetryArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("ANY")
                .body(telemetryBody3)
                .build());

        // get timeseries keys
        List<String> tsKeys = client.getTimeseriesKeys(GetTimeseriesKeysArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .build());
        assertNotNull(tsKeys);
        assertEquals(2, tsKeys.size());
        assertTrue(tsKeys.containsAll(List.of("humidity", "temperature")));

        // get latest timeseries
        Map<String, List<TsData>> latestData = client.getLatestTimeseries(GetLatestTimeseriesArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .keys("temperature,humidity")
                .useStrictDataTypes(false)
                .build());
        assertNotNull(latestData);
        assertNotNull(latestData.get("temperature"));
        assertFalse(latestData.get("temperature").isEmpty());
        assertEquals("27.1", latestData.get("temperature").get(0).getValue().toString());

        // get timeseries history
        Map<String, List<TsData>> historyData = client.getTimeseriesHistory(GetTimeseriesHistoryArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .startTs(ts1 - 1000)
                .endTs(ts3 + 1000)
                .keys("temperature")
                .agg("NONE")
                .orderBy("ASC")
                .useStrictDataTypes(false)
                .build());
        assertNotNull(historyData);
        List<TsData> tempHistory = historyData.get("temperature");
        assertNotNull(tempHistory);
        assertEquals(3, tempHistory.size());
        assertEquals("25.5", tempHistory.get(0).getValue().toString());
        assertEquals("27.1", tempHistory.get(2).getValue().toString());

        // delete timeseries
        client.deleteEntityTimeseries(DeleteEntityTimeseriesArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .keys("humidity")
                .deleteAllDataForKeys(true)
                .deleteLatest(true)
                .rewriteLatestIfDeleted(false)
                .build());

        List<String> keysAfterDelete = client.getTimeseriesKeys(GetTimeseriesKeysArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .build());
        assertFalse(keysAfterDelete.contains("humidity"));

        // delete attributes
        client.deleteEntityAttributes(DeleteEntityAttributesArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .keys("serverAttr1")
                .build());

        List<String> serverKeysAfterDelete = client.getAttributeKeysByScope(GetAttributeKeysByScopeArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .build());
        assertFalse(serverKeysAfterDelete.contains("serverAttr1"));
        assertTrue(serverKeysAfterDelete.contains("serverAttr2"));

        // save device attributes using device-specific endpoint
        client.saveDeviceAttributes(SaveDeviceAttributesArgs.builder()
                .deviceId(entityId)
                .scope("SERVER_SCOPE")
                .body("{\"deviceSpecificAttr\": \"test\"}")
                .build());

        List<String> deviceKeys = client.getAttributeKeysByScope(GetAttributeKeysByScopeArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .build());
        assertTrue(deviceKeys.contains("deviceSpecificAttr"));

        // delete device attributes
        client.deleteDeviceAttributes(DeleteDeviceAttributesArgs.builder()
                .deviceId(entityId)
                .scope("SERVER_SCOPE")
                .keys("deviceSpecificAttr")
                .build());

        List<String> deviceKeysAfterDelete = client.getAttributeKeysByScope(GetAttributeKeysByScopeArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("SERVER_SCOPE")
                .build());
        assertFalse(deviceKeysAfterDelete.contains("deviceSpecificAttr"));

        // save telemetry with TTL
        String ttlTelemetry = "{\"ts\":" + timestamp + ",\"values\":{\"shortLived\":99}}";
        client.saveEntityTelemetryWithTTL(SaveEntityTelemetryWithTTLArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .scope("ANY")
                .ttl(86400L)
                .body(ttlTelemetry)
                .build());

        Map<String, List<TsData>> latestWithTtl = client.getLatestTimeseries(GetLatestTimeseriesArgs.builder()
                .entityType(entityType)
                .entityId(entityId)
                .keys("shortLived")
                .useStrictDataTypes(false)
                .build());
        assertNotNull(latestWithTtl.get("shortLived"));
        assertEquals("99", latestWithTtl.get("shortLived").get(0).getValue().toString());
    }

}
