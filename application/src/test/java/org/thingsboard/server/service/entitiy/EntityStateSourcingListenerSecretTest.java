// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.secret.SecretService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * What a saved secret triggers. A new secret can only heal an extension that already names it, so it
 * must cost one usage search (extensions), not one per entity type. An updated secret still reaches
 * every entity type that reloads on a secret change.
 */
class EntityStateSourcingListenerSecretTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final SecretId secretId = new SecretId(UUID.randomUUID());

    private TbClusterService clusterService;
    private SecretService secretService;
    private EntityStateSourcingListener listener;

    @BeforeEach
    void setUp() {
        clusterService = mock(TbClusterService.class);
        secretService = mock(SecretService.class);
        listener = new EntityStateSourcingListener(null, clusterService, null, null, secretService,
                Optional.empty(), Optional.empty(), Optional.empty(), null, Optional.empty());
    }

    @Test
    void aNewSecretSearchesOnlyTheExtensionsAndReappliesThem() {
        Secret secret = secret();
        EntityInfo weather = new EntityInfo(UUID.randomUUID(), "EXTENSION", "weather");
        when(secretService.findEntitiesBySecret(tenantId, secret, EntityType.EXTENSION)).thenReturn(List.of(weather));

        listener.handleEvent(event(secret, true));

        verify(secretService, never()).findEntitiesBySecret(any(), any());
        verify(clusterService).broadcastEntityStateChangeEvent(tenantId, weather.getId(), ComponentLifecycleEvent.CREATED);
        verifyNoMoreInteractions(clusterService);
    }

    @Test
    void aNewSecretThatNoExtensionNamesBroadcastsNothing() {
        Secret secret = secret();
        when(secretService.findEntitiesBySecret(tenantId, secret, EntityType.EXTENSION)).thenReturn(List.of());

        listener.handleEvent(event(secret, true));

        verify(secretService, never()).findEntitiesBySecret(any(), any());
        verifyNoMoreInteractions(clusterService);
    }

    @Test
    void anUpdatedSecretReachesExtensionsRuleChainsAndIntegrations() {
        Secret secret = secret();
        EntityInfo weather = new EntityInfo(UUID.randomUUID(), "EXTENSION", "weather");
        EntityInfo ruleChain = new EntityInfo(UUID.randomUUID(), "RULE_CHAIN", "Root");
        EntityInfo integration = new EntityInfo(UUID.randomUUID(), "INTEGRATION", "HTTP");
        EntityInfo aiModel = new EntityInfo(UUID.randomUUID(), "AI_MODEL", "Model");
        when(secretService.findEntitiesBySecret(tenantId, secret)).thenReturn(Map.of(
                EntityType.EXTENSION, List.of(weather),
                EntityType.RULE_CHAIN, List.of(ruleChain),
                EntityType.INTEGRATION, List.of(integration),
                EntityType.AI_MODEL, List.of(aiModel)));

        listener.handleEvent(event(secret, false));

        verify(secretService, never()).findEntitiesBySecret(any(), any(), any());
        verify(clusterService).broadcastEntityStateChangeEvent(tenantId, weather.getId(), ComponentLifecycleEvent.UPDATED);
        verify(clusterService).broadcastEntityStateChangeEvent(tenantId, ruleChain.getId(), ComponentLifecycleEvent.UPDATED);
        verify(clusterService).broadcastEntityStateChangeEvent(tenantId, integration.getId(), ComponentLifecycleEvent.UPDATED);
        verifyNoMoreInteractions(clusterService);
    }

    private Secret secret() {
        Secret secret = new Secret();
        secret.setId(secretId);
        secret.setTenantId(tenantId);
        secret.setName("weather-key");
        secret.setType(SecretType.TEXT);
        return secret;
    }

    private SaveEntityEvent<Secret> event(Secret secret, boolean created) {
        return SaveEntityEvent.<Secret>builder()
                .tenantId(tenantId)
                .entityId(secretId)
                .entity(secret)
                .created(created)
                .build();
    }

}
