// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.DeleteSecretArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSecretInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSecretInfoByNameArgs;
import org.thingsboard.client.api.ThingsboardApi.GetSecretInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveSecretArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateSecretDescriptionArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateSecretValueArgs;
import org.thingsboard.client.model.PageDataSecretInfo;
import org.thingsboard.client.model.Secret;
import org.thingsboard.client.model.SecretInfo;
import org.thingsboard.client.model.SecretType;
import org.thingsboard.client.model.TbSecretDeleteResult;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class SecretApiClientTest extends AbstractApiClientTest {

    @Test
    public void testSecretLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts;

        SecretInfo saved = createSecret(name);
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(name, saved.getName());
        assertEquals(SecretType.TEXT, saved.getType());
        UUID secretId = saved.getId().getId();

        SecretInfo byId = client.getSecretInfoById(GetSecretInfoByIdArgs.builder()
                .id(secretId)
                .build());
        assertNotNull(byId);
        assertEquals(secretId, byId.getId().getId());
        assertEquals(name, byId.getName());

        SecretInfo byName = client.getSecretInfoByName(GetSecretInfoByNameArgs.builder()
                .name(name)
                .build());
        assertNotNull(byName);
        assertEquals(secretId, byName.getId().getId());
        assertEquals(name, byName.getName());

        SecretInfo afterDescUpdate = client.updateSecretDescription(UpdateSecretDescriptionArgs.builder()
                .id(secretId)
                .body("updated description")
                .build());
        assertNotNull(afterDescUpdate);
        assertEquals("updated description", afterDescUpdate.getDescription());

        SecretInfo afterValueUpdate = client.updateSecretValue(UpdateSecretValueArgs.builder()
                .id(secretId)
                .body("new-secret-value")
                .build());
        assertNotNull(afterValueUpdate);
        assertEquals(secretId, afterValueUpdate.getId().getId());

        TbSecretDeleteResult result = client.deleteSecret(DeleteSecretArgs.builder()
                .id(secretId)
                .build());
        assertNotNull(result);

        assertReturns404(() -> client.getSecretInfoById(GetSecretInfoByIdArgs.builder()
                .id(secretId)
                .build()));
    }

    @Test
    public void testGetSecretInfos() throws Exception {
        long ts = System.currentTimeMillis();

        SecretInfo s1 = createSecret(TEST_PREFIX + ts + "_1");
        SecretInfo s2 = createSecret(TEST_PREFIX + ts + "_2");
        SecretInfo s3 = createSecret(TEST_PREFIX + ts + "_3");
        UUID id1 = s1.getId().getId();
        UUID id2 = s2.getId().getId();
        UUID id3 = s3.getId().getId();

        PageDataSecretInfo page = client.getSecretInfos(GetSecretInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 3);
        assertTrue(page.getData().stream().anyMatch(s -> s.getId().getId().equals(id1)));
        assertTrue(page.getData().stream().anyMatch(s -> s.getId().getId().equals(id2)));
        assertTrue(page.getData().stream().anyMatch(s -> s.getId().getId().equals(id3)));

        client.deleteSecret(DeleteSecretArgs.builder()
                .id(id1)
                .build());
        client.deleteSecret(DeleteSecretArgs.builder()
                .id(id2)
                .build());
        client.deleteSecret(DeleteSecretArgs.builder()
                .id(id3)
                .build());
    }

    @Test
    public void testGetSecretNames() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts + "_named";

        SecretInfo saved = createSecret(name);
        UUID secretId = saved.getId().getId();

        List<String> names = client.getSecretNames();
        assertNotNull(names);
        assertTrue(names.contains(name));

        client.deleteSecret(DeleteSecretArgs.builder()
                .id(secretId)
                .build());
    }

    @Test
    public void testGetSecretInfoByIdNotFound() {
        assertReturns404(() -> client.getSecretInfoById(GetSecretInfoByIdArgs.builder()
                .id(UUID.randomUUID())
                .build()));
    }

    private Secret buildSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setType(SecretType.TEXT);
        secret.setValue(value);
        secret.setDescription("description for " + name);
        return secret;
    }

    private SecretInfo createSecret(String name) throws ApiException {
        return client.saveSecret(SaveSecretArgs.builder()
                .secret(buildSecret(name, "initial-value"))
                .build());
    }

}
