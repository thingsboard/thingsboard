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
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.ApiKeyInfo;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AttributeData;
import org.thingsboard.client.model.BooleanFilterPredicate;
import org.thingsboard.client.model.BooleanOperation;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.EntityCountQuery;
import org.thingsboard.client.model.EntityKey;
import org.thingsboard.client.model.EntityKeyType;
import org.thingsboard.client.model.EntityKeyValueType;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.EntityTypeFilter;
import org.thingsboard.client.model.FilterPredicateValueBoolean;
import org.thingsboard.client.model.KeyFilter;
import org.thingsboard.client.model.PageDataDevice;
import org.thingsboard.client.model.TsData;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Mirrors every code snippet from the Java client documentation page
 * ({@code /docs/reference/java-client/}, CE edition). Each snippet appears
 * character-for-character with two allowances:
 * <ul>
 *   <li>placeholder values ({@code "{BASE_URL}"}, {@code "YOUR_API_KEY_VALUE"},
 *       {@code "YOUR_DEVICE_ID"}, {@code "YOUR_ASSET_ID"},
 *       {@code "nonexistent-id"}, {@code "tenant@thingsboard.org"},
 *       {@code "tenant"}) are swapped for real test values;</li>
 *   <li>{@code System.out.println} / {@code System.out.printf} calls inside the
 *       snippet are replaced with equivalent JUnit assertions, so the test
 *       actually verifies behavior instead of only compilation.</li>
 * </ul>
 * Setup code that pre-creates entities required by a snippet and post-snippet
 * verifications stay outside the snippet block.
 */
@DaoSqlTest
public class ClientDocsExampleTest extends AbstractApiClientTest {

    // /docs/reference/java-client/#quickstart
    @Test
    public void testQuickstart() throws Exception {
        // setup: real API key for the snippet's "YOUR_API_KEY_VALUE" placeholder
        ApiKeyInfo keyRequest = new ApiKeyInfo();
        keyRequest.setDescription("ClientDocsExampleTest");
        keyRequest.setUserId(clientTenantAdmin.getId());
        keyRequest.setEnabled(true);
        String apiKeyValue = this.client.saveApiKey(keyRequest).getValue();

        // === doc snippet ===
        ThingsboardClient client = ThingsboardClient.builder()
                .url(getBaseUrl())
                .apiKey(apiKeyValue)
                .build();

        Device newDevice = new Device();
        newDevice.setName("Quickstart Device");
        newDevice.setType("default");
        Device savedDevice = client.saveDevice(newDevice, null, null, null, null);

        String deviceId = savedDevice.getId().getId().toString();
        client.saveEntityTelemetry("DEVICE", deviceId, "ANY", """
                {"temperature": 22.4}
                """);
        assertEquals("Quickstart Device", savedDevice.getName());

        client.deleteDevice(deviceId);

        // post-snippet verification: the device is gone after deletion
        assertReturns404(() -> client.getDeviceById(deviceId));
    }

    // /docs/reference/java-client/#api-key-recommended
    @Test
    public void testAuthenticationViaApiKey() throws Exception {
        // setup: real API key for the snippet's "YOUR_API_KEY_VALUE" placeholder
        ApiKeyInfo keyRequest = new ApiKeyInfo();
        keyRequest.setDescription("ClientDocsExampleTest");
        keyRequest.setUserId(clientTenantAdmin.getId());
        keyRequest.setEnabled(true);
        String apiKeyValue = this.client.saveApiKey(keyRequest).getValue();

        // === doc snippet ===
        String url = getBaseUrl();
        String apiKey = apiKeyValue;
        ThingsboardClient client = ThingsboardClient.builder()
                .url(url)
                .apiKey(apiKey)
                .build();

        assertEquals(TENANT_ADMIN_USERNAME, client.getUser().getEmail());
    }

    // /docs/reference/java-client/#username-and-password-jwt
    @Test
    public void testAuthenticationViaCredentials() throws Exception {
        // === doc snippet ===
        String url = getBaseUrl();
        ThingsboardClient client = ThingsboardClient.builder()
                .url(url)
                .credentials(TENANT_ADMIN_USERNAME, TEST_PASSWORD)
                .build();

        assertEquals(TENANT_ADMIN_USERNAME, client.getUser().getEmail());
    }

    // /docs/reference/java-client/#rate-limit-handling
    @Test
    public void testRateLimitHandlingBuilderOptions() throws Exception {
        // setup: real url + api key that the snippet references as locals
        String url = getBaseUrl();
        ApiKeyInfo keyRequest = new ApiKeyInfo();
        keyRequest.setDescription("ClientDocsExampleTest");
        keyRequest.setUserId(clientTenantAdmin.getId());
        keyRequest.setEnabled(true);
        String apiKey = this.client.saveApiKey(keyRequest).getValue();

        // === doc snippet ===
        ThingsboardClient client = ThingsboardClient.builder()
                .url(url)
                .apiKey(apiKey)
                .maxRetries(3)              // default 3
                .initialRetryDelayMs(1000)  // default 1 s
                .maxRetryDelayMs(30_000)    // default 30 s
                .build();

        // post-snippet verification: the tuned client is actually usable
        assertEquals(TENANT_ADMIN_USERNAME, client.getUser().getEmail());
    }

    // /docs/reference/java-client/#working-with-entities
    @Test
    public void testWorkingWithEntities() throws Exception {
        // === doc snippet ===
        Device newDevice = new Device();
        newDevice.setName("Test Device");
        newDevice.setType("default");
        Device savedDevice = client.saveDevice(newDevice, null, null, null, null);

        String deviceId = savedDevice.getId().getId().toString();
        Device fetched = client.getDeviceById(deviceId);
        assertEquals("Test Device", fetched.getName());

        client.deleteDevice(deviceId);

        // post-snippet verification: the device is gone after deletion
        assertReturns404(() -> client.getDeviceById(deviceId));
    }

    // /docs/reference/java-client/#push-telemetry
    @Test
    public void testPushTelemetry() throws Exception {
        // setup: create a real device whose id replaces "YOUR_DEVICE_ID"
        Device setup = new Device();
        setup.setName("Telemetry Setup Device");
        setup.setType("default");
        String realDeviceId = client.saveDevice(setup, null, null, null, null)
                .getId().getId().toString();

        // === doc snippet ===
        String deviceId = realDeviceId;
        String body = """
                {"temperature": 26.5, "humidity": 87}
                """;
        client.saveEntityTelemetry("DEVICE", deviceId, "ANY", body);

        // post-snippet verification: telemetry was actually persisted
        Map<String, List<TsData>> latest =
                client.getLatestTimeseries("DEVICE", deviceId, "temperature,humidity", false, null);
        assertEquals("26.5", latest.get("temperature").get(0).getValue().toString());
        assertEquals("87", latest.get("humidity").get(0).getValue().toString());
    }

    // /docs/reference/java-client/#read-and-write-attributes-read-modify-write
    @Test
    public void testReadModifyWriteAttributes() throws Exception {
        // setup: create a real asset whose id replaces "YOUR_ASSET_ID"
        Asset setupAsset = new Asset();
        setupAsset.setName("Counter Setup Asset");
        setupAsset.setType("building");
        String realAssetId = client.saveAsset(setupAsset, null, null, null)
                .getId().getId().toString();

        // === doc snippet ===
        String assetId = realAssetId;

        List<AttributeData> attrs = client.getAttributesByScope(
                "ASSET", assetId, "SERVER_SCOPE", "deviceCount", null);

        // getValue() returns Object — JSON numbers come back as Number subclasses
        long current = attrs.isEmpty() ? 0L : ((Number) attrs.get(0).getValue()).longValue();
        long updated = current + 1;

        client.saveEntityAttributesV2("ASSET", assetId, "SERVER_SCOPE",
                "{\"deviceCount\": %d}".formatted(updated));

        // post-snippet verification: the increment was actually persisted
        List<AttributeData> after = client.getAttributesByScope(
                "ASSET", assetId, "SERVER_SCOPE", "deviceCount", null);
        assertEquals(1, after.size());
        assertEquals(updated, ((Number) after.get(0).getValue()).longValue());
    }

    // /docs/reference/java-client/#paginated-tenant-list
    @Test
    public void testPaginatedTenantList() throws Exception {
        // setup: populate the tenant with a few devices so the iteration has something to walk
        int expectedDeviceCount = 5;
        for (int i = 0; i < expectedDeviceCount; i++) {
            Device d = new Device();
            d.setName("Page Setup Device " + i);
            d.setType("default");
            client.saveDevice(d, null, null, null, null);
        }

        // === doc snippet ===
        int page = 0;  // pages are zero-indexed
        PageDataDevice devices;
        do {
            devices = client.getTenantDevices(100, page, null, null, null, null);
            devices.getData().forEach(d -> assertEquals("default", d.getType()));
            page++;
        } while (devices.getHasNext());

        // post-snippet verification: pagination terminated and reached every device
        assertEquals((long) expectedDeviceCount, devices.getTotalElements().longValue());
    }

    // /docs/reference/java-client/#filtered-query-with-entity-data-query-api
    @Test
    public void testEntityDataQueryCountFiltered() throws Exception {
        // setup: create a mix of active and inactive devices for the count query
        Device active1 = client.saveDevice(
                new Device().name("Active_1").type("default"),
                null, null, null, null);
        Device active2 = client.saveDevice(
                new Device().name("Active_2").type("default"),
                null, null, null, null);
        client.saveDevice(
                new Device().name("Inactive_1").type("default"),
                null, null, null, null);
        client.saveEntityAttributesV2("DEVICE", active1.getId().getId().toString(),
                "SERVER_SCOPE", "{\"active\": true}");
        client.saveEntityAttributesV2("DEVICE", active2.getId().getId().toString(),
                "SERVER_SCOPE", "{\"active\": true}");

        // === doc snippet ===
        EntityTypeFilter typeFilter = new EntityTypeFilter();
        typeFilter.setEntityType(EntityType.DEVICE);

        EntityCountQuery totalQuery = new EntityCountQuery();
        totalQuery.setEntityFilter(typeFilter);
        assertEquals(3L, client.countEntitiesByQuery(totalQuery).longValue());

        KeyFilter activeFilter = new KeyFilter();
        activeFilter.setKey(new EntityKey().type(EntityKeyType.ATTRIBUTE).key("active"));
        activeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanOperation.EQUAL);
        predicate.setValue(new FilterPredicateValueBoolean().defaultValue(true));
        activeFilter.setPredicate(predicate);

        EntityCountQuery activeQuery = new EntityCountQuery();
        activeQuery.setEntityFilter(typeFilter);
        activeQuery.setKeyFilters(List.of(activeFilter));
        assertEquals(2L, client.countEntitiesByQuery(activeQuery).longValue());
    }

    // /docs/reference/java-client/#error-handling
    @Test
    public void testErrorHandling404() {
        // setup: a real (random) UUID that doesn't resolve, replacing "nonexistent-id";
        // the flag captures whether the 404 branch ran so we can assert the snippet
        // actually entered error handling (instead of silently completing).
        String missingDeviceId = UUID.randomUUID().toString();
        boolean[] caught404 = {false};

        // === doc snippet ===
        try {
            Device device = client.getDeviceById(missingDeviceId);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                caught404[0] = true;
            } else {
                fail("API error " + e.getCode() + ": " + e.getResponseBody());
            }
        }

        // post-snippet verification: the snippet actually exercised the 404 branch
        assertTrue("Expected ApiException with code 404", caught404[0]);
    }
}
