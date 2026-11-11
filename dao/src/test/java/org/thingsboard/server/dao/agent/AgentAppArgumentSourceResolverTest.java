// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.owner.OwnerService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppArgumentSourceResolverTest {

    @Mock
    private OwnerService ownerService;
    @Mock
    private AgentAppRelationService agentAppRelationService;

    @InjectMocks
    private AgentAppArgumentSourceResolver resolver;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final AgentId agentId = new AgentId(UUID.randomUUID());
    private AgentApplication application;

    @BeforeEach
    void setUp() {
        application = new AgentApplication();
        application.setTenantId(tenantId);
        application.setAgentId(agentId);
    }

    @Test
    void agentResolvesToAgentId() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.AGENT)).isEqualTo(agentId);
    }

    @Test
    void tenantResolvesToTenantId() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.TENANT)).isEqualTo(tenantId);
    }

    @Test
    void ownerResolvesViaOwnerService() {
        EntityId ownerId = new CustomerId(UUID.randomUUID());
        when(ownerService.getOwner(tenantId, agentId)).thenReturn(ownerId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.OWNER)).isEqualTo(ownerId);
    }

    @Test
    void relatedEntityUsesSavedRelationWhenPresent() {
        EntityId edgeId = new EdgeId(UUID.randomUUID());
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(edgeId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isEqualTo(edgeId);
        verify(agentAppRelationService, never()).findRelatedEntityByConfig(tenantId, application);
    }

    @Test
    void relatedEntityFallsBackToConfigWhenNoSavedRelation() {
        EntityId edgeId = new EdgeId(UUID.randomUUID());
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(null);
        when(agentAppRelationService.findRelatedEntityByConfig(tenantId, application)).thenReturn(edgeId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isEqualTo(edgeId);
    }

    @Test
    void relatedEntityIsNullWhenNeitherResolves() {
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(null);
        when(agentAppRelationService.findRelatedEntityByConfig(tenantId, application)).thenReturn(null);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isNull();
    }

    @Test
    void concreteSourceResolvesToNull() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.DEVICE)).isNull();
    }
}
