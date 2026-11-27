// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.secret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.sql.HasSecretsEntityDao;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The usage search for one entity type. A new secret can heal only extensions, so the caller asks for
 * that type alone; the search must then run one query and leave the other entity types untouched.
 */
class SecretServiceImplUsageSearchTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    private HasSecretsEntityDao extensionDao;
    private HasSecretsEntityDao ruleChainDao;
    private SecretServiceImpl service;

    @BeforeEach
    void setUp() {
        extensionDao = mock(HasSecretsEntityDao.class);
        when(extensionDao.getEntityType()).thenReturn(EntityType.EXTENSION);
        ruleChainDao = mock(HasSecretsEntityDao.class);
        when(ruleChainDao.getEntityType()).thenReturn(EntityType.RULE_CHAIN);

        service = new SecretServiceImpl(null, null, null, null);
        ReflectionTestUtils.invokeMethod(service, "setHasSecretsEntityDaos", List.of(extensionDao, ruleChainDao));
    }

    @Test
    void searchesOnlyTheRequestedEntityType() {
        EntityInfo weather = new EntityInfo(UUID.randomUUID(), "EXTENSION", "weather");
        when(extensionDao.findByTenantIdAndSecretPlaceholder(tenantId, "${secret:weather-key;type:TEXT}"))
                .thenReturn(List.of(weather));

        List<EntityInfo> found = service.findEntitiesBySecret(tenantId, secret("weather-key"), EntityType.EXTENSION);

        assertThat(found).containsExactly(weather);
        verify(ruleChainDao, never()).findByTenantIdAndSecretPlaceholder(any(), any());
    }

    // '%' and '_' are wildcards in the ILIKE pattern the DAO builds, so they are escaped here exactly
    // as in the search of all entity types.
    @Test
    void escapesLikeWildcardsInTheSecretName() {
        service.findEntitiesBySecret(tenantId, secret("api_key%"), EntityType.EXTENSION);

        verify(extensionDao).findByTenantIdAndSecretPlaceholder(tenantId, "${secret:api\\_key\\%;type:TEXT}");
    }

    @Test
    void returnsNothingForAnEntityTypeThatHoldsNoSecrets() {
        assertThat(service.findEntitiesBySecret(tenantId, secret("weather-key"), EntityType.DEVICE)).isEmpty();

        verify(extensionDao, never()).findByTenantIdAndSecretPlaceholder(any(), any());
        verify(ruleChainDao, never()).findByTenantIdAndSecretPlaceholder(any(), any());
    }

    private SecretInfo secret(String name) {
        SecretInfo secretInfo = new SecretInfo();
        secretInfo.setTenantId(tenantId);
        secretInfo.setName(name);
        secretInfo.setType(SecretType.TEXT);
        return secretInfo;
    }

}
