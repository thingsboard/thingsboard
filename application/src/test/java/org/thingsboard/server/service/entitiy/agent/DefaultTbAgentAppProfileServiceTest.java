// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTbAgentAppProfileServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentAppProfileId PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final EdgeProfileId EDGE_PROFILE_ID = new EdgeProfileId(UUID.randomUUID());
    private static final User USER = new User();

    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentAppConfigMergeOrchestrator configMergeOrchestrator;
    @Mock
    private TbLogEntityActionService logEntityActionService;

    private DefaultTbAgentAppProfileService service;

    @BeforeEach
    void setUp() {
        service = new DefaultTbAgentAppProfileService(profileService, configMergeOrchestrator);
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
    }

    @Test
    void save_rejectedEdgeProfileLink_doesNotLogTheProfileAsAdded() {
        AgentAppProfile profile = newProfile();
        AgentAppProfile saved = newProfile();
        saved.setId(PROFILE_ID);
        when(profileService.saveProfile(profile)).thenReturn(saved);
        when(profileService.setEdgeProfile(TENANT_ID, PROFILE_ID, EDGE_PROFILE_ID)).thenThrow(new DataValidationException("rejected"));

        assertThatThrownBy(() -> service.save(profile, EDGE_PROFILE_ID, USER)).isInstanceOf(DataValidationException.class);

        verify(logEntityActionService, never()).logEntityAction(TENANT_ID, PROFILE_ID, saved, ActionType.ADDED, USER);
        verify(logEntityActionService).logEntityAction(eq(TENANT_ID), any(AgentAppProfileId.class), eq(profile), eq(ActionType.ADDED), eq(USER), any(DataValidationException.class));
    }

    @Test
    void save_clearedEdgeProfileLink_logsRelationDeletedNamingThePreviousProfileBeforeTheUpdate() throws Exception {
        AgentAppProfile profile = newProfile();
        profile.setId(PROFILE_ID);
        when(profileService.saveProfile(profile)).thenReturn(profile);
        when(profileService.findEdgeProfileId(TENANT_ID, PROFILE_ID)).thenReturn(EDGE_PROFILE_ID);
        when(profileService.setEdgeProfile(TENANT_ID, PROFILE_ID, null)).thenReturn(true);

        service.save(profile, null, USER);

        InOrder inOrder = inOrder(logEntityActionService);
        inOrder.verify(logEntityActionService).logEntityAction(TENANT_ID, PROFILE_ID, profile, ActionType.RELATION_DELETED, USER, EDGE_PROFILE_ID.toString());
        inOrder.verify(logEntityActionService).logEntityAction(TENANT_ID, PROFILE_ID, profile, ActionType.UPDATED, USER);
    }

    @Test
    void save_unchangedEdgeProfileLink_logsOnlyTheUpdate() throws Exception {
        AgentAppProfile profile = newProfile();
        profile.setId(PROFILE_ID);
        when(profileService.saveProfile(profile)).thenReturn(profile);
        when(profileService.setEdgeProfile(TENANT_ID, PROFILE_ID, EDGE_PROFILE_ID)).thenReturn(false);

        service.save(profile, EDGE_PROFILE_ID, USER);

        verify(logEntityActionService).logEntityAction(TENANT_ID, PROFILE_ID, profile, ActionType.UPDATED, USER);
        verifyNoMoreInteractions(logEntityActionService);
    }

    private static AgentAppProfile newProfile() {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(TENANT_ID);
        profile.setName("Edge app profile");
        return profile;
    }

}
