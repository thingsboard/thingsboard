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
import org.thingsboard.client.model.ApiKey;
import org.thingsboard.client.model.ApiKeyInfo;
import org.thingsboard.client.model.PageDataApiKeyInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class ApiKeyApiClientTest extends AbstractApiClientTest {

    @Test
    public void testApiKeyLifecycle() throws Exception {
        String userId = clientTenantAdmin.getId().getId().toString();

        ApiKeyInfo request = new ApiKeyInfo();
        request.setDescription("Test API key");
        request.setUserId(clientTenantAdmin.getId());
        request.setEnabled(true);
        ApiKey created = client.saveApiKey(request);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getValue());
        assertFalse(created.getValue().isBlank());
        assertEquals("Test API key", created.getDescription());

        UUID keyId = created.getId().getId();

        PageDataApiKeyInfo keysPage = client.getUserApiKeys(userId, 100, 0, null, null, null);
        assertNotNull(keysPage);
        assertNotNull(keysPage.getData());
        assertTrue("Newly created API key should appear in user's key list",
                keysPage.getData().stream()
                        .anyMatch(k -> k.getId().getId().equals(keyId)));

        client.deleteApiKey(keyId);

        PageDataApiKeyInfo keysAfterDelete = client.getUserApiKeys(userId, 100, 0, null, null, null);
        assertTrue("Deleted API key should not appear in user's key list",
                keysAfterDelete.getData().stream()
                        .noneMatch(k -> k.getId().getId().equals(keyId)));
    }

    @Test
    public void testEnableDisableApiKey() throws Exception {
        ApiKeyInfo request = new ApiKeyInfo();
        request.setDescription("Enable/disable test key");
        request.setUserId(clientTenantAdmin.getId());
        request.setEnabled(true);
        ApiKey created = client.saveApiKey(request);
        assertNotNull(created);

        UUID keyId = created.getId().getId();

        ApiKeyInfo disabled = client.enableApiKey(keyId, false);
        assertNotNull(disabled);
        assertEquals(Boolean.FALSE, disabled.getEnabled());

        ApiKeyInfo enabled = client.enableApiKey(keyId, true);
        assertNotNull(enabled);
        assertEquals(Boolean.TRUE, enabled.getEnabled());

        client.deleteApiKey(keyId);
    }

    @Test
    public void testGetUserApiKeys() throws Exception {
        String userId = clientTenantAdmin.getId().getId().toString();

        int initialCount = client.getUserApiKeys(userId, 100, 0, null, null, null)
                .getData().size();

        UUID[] createdIds = new UUID[3];
        for (int i = 0; i < 3; i++) {
            ApiKeyInfo request = new ApiKeyInfo();
            request.setDescription("Paging test key " + i);
            request.setUserId(clientTenantAdmin.getId());
            createdIds[i] = client.saveApiKey(request).getId().getId();
        }

        PageDataApiKeyInfo afterCreate = client.getUserApiKeys(userId, 100, 0, null, null, null);
        assertEquals(initialCount + 3, afterCreate.getData().size());
        assertEquals(Long.valueOf(initialCount + 3), afterCreate.getTotalElements());

        PageDataApiKeyInfo page1 = client.getUserApiKeys(userId, 2, 0, null, null, null);
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        for (UUID id : createdIds) {
            client.deleteApiKey(id);
        }
    }

}
