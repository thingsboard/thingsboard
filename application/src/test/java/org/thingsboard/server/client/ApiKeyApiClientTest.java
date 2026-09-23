// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteApiKeyArgs;
import org.thingsboard.client.api.ThingsboardApi.EnableApiKeyArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserApiKeysArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveApiKeyArgs;
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
        ApiKey created = client.saveApiKey(SaveApiKeyArgs.builder()
                .apiKeyInfo(request)
                .build());

        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getValue());
        assertFalse(created.getValue().isBlank());
        assertEquals("Test API key", created.getDescription());

        UUID keyId = created.getId().getId();

        PageDataApiKeyInfo keysPage = client.getUserApiKeys(GetUserApiKeysArgs.builder()
                .userId(userId)
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(keysPage);
        assertNotNull(keysPage.getData());
        assertTrue("Newly created API key should appear in user's key list",
                keysPage.getData().stream()
                        .anyMatch(k -> k.getId().getId().equals(keyId)));

        client.deleteApiKey(DeleteApiKeyArgs.builder()
                .id(keyId)
                .build());

        PageDataApiKeyInfo keysAfterDelete = client.getUserApiKeys(GetUserApiKeysArgs.builder()
                .userId(userId)
                .pageSize(100)
                .page(0)
                .build());
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
        ApiKey created = client.saveApiKey(SaveApiKeyArgs.builder()
                .apiKeyInfo(request)
                .build());
        assertNotNull(created);

        UUID keyId = created.getId().getId();

        ApiKeyInfo disabled = client.enableApiKey(EnableApiKeyArgs.builder()
                .id(keyId)
                .enabledValue(false)
                .build());
        assertNotNull(disabled);
        assertEquals(Boolean.FALSE, disabled.getEnabled());

        ApiKeyInfo enabled = client.enableApiKey(EnableApiKeyArgs.builder()
                .id(keyId)
                .enabledValue(true)
                .build());
        assertNotNull(enabled);
        assertEquals(Boolean.TRUE, enabled.getEnabled());

        client.deleteApiKey(DeleteApiKeyArgs.builder()
                .id(keyId)
                .build());
    }

    @Test
    public void testGetUserApiKeys() throws Exception {
        String userId = clientTenantAdmin.getId().getId().toString();

        int initialCount = client.getUserApiKeys(GetUserApiKeysArgs.builder()
                .userId(userId)
                .pageSize(100)
                .page(0)
                .build())
                .getData().size();

        UUID[] createdIds = new UUID[3];
        for (int i = 0; i < 3; i++) {
            ApiKeyInfo request = new ApiKeyInfo();
            request.setDescription("Paging test key " + i);
            request.setUserId(clientTenantAdmin.getId());
            createdIds[i] = client.saveApiKey(SaveApiKeyArgs.builder()
                    .apiKeyInfo(request)
                    .build()).getId().getId();
        }

        PageDataApiKeyInfo afterCreate = client.getUserApiKeys(GetUserApiKeysArgs.builder()
                .userId(userId)
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialCount + 3, afterCreate.getData().size());
        assertEquals(Long.valueOf(initialCount + 3), afterCreate.getTotalElements());

        PageDataApiKeyInfo page1 = client.getUserApiKeys(GetUserApiKeysArgs.builder()
                .userId(userId)
                .pageSize(2)
                .page(0)
                .build());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        for (UUID id : createdIds) {
            client.deleteApiKey(DeleteApiKeyArgs.builder()
                    .id(id)
                    .build());
        }
    }

}
