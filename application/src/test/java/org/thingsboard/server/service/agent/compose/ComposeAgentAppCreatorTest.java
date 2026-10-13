// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppRelationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.service.agent.AgentAppProvisioner;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComposeAgentAppCreatorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentProfileId AGENT_PROFILE_ID = new AgentProfileId(UUID.randomUUID());
    private static final AgentAppProfileId APP_PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final String PROJECT_NAME = "tb-edge";
    private static final String EDGE_VERSION = "4.2.0EDGEPE";
    private static final EdgeId EDGE_ID = new EdgeId(UUID.randomUUID());

    @Mock
    private AgentService agentService;
    @Mock
    private AgentAppProfileService appProfileService;
    @Mock
    private AgentAppProvisioner appProvisioner;
    @Mock
    private AgentEventRateLimiter agentEventRateLimiter;
    @Mock
    private TbAgentAppTemplateService templateService;
    @Mock
    private AgentAppRelationService appRelationService;

    @InjectMocks
    private ComposeAgentAppCreator creator;

    private AgentAppTemplate edgeTemplate;

    @BeforeEach
    void setUp() {
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setAgentProfileId(AGENT_PROFILE_ID);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agent);

        edgeTemplate = template(AgentApplicationType.EDGE, EDGE_VERSION);
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, EDGE_VERSION)).thenReturn(edgeTemplate);
        when(appRelationService.findRelatedEntityByConfig(eq(TENANT_ID),
                argThat(app -> app != null && app.getConfig() != null))).thenReturn(EDGE_ID);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(), any(), any()))
                .thenAnswer(inv -> {
                    AgentApplication app = inv.getArgument(1);
                    app.setId(new AgentApplicationId(UUID.randomUUID()));
                    return app;
                });
    }

    @Test
    void withoutAssignedProfile_storesConfigAndEnqueuesNoEvent() {
        when(appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                TENANT_ID, AGENT_PROFILE_ID, AgentApplicationType.EDGE, EDGE_VERSION)).thenReturn(List.of());

        AgentApplication created = creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose(EDGE_VERSION));

        assertThat(created).isNotNull();
        assertThat(created.getAppType()).isEqualTo(AgentApplicationType.EDGE);
        assertThat(created.getTemplateVersion()).isEqualTo(EDGE_VERSION);
        assertThat(created.getOrigin()).isEqualTo(AgentApplicationOrigin.DISCOVERED);
        assertThat(created.getApplicationProfileId()).isNull();
        assertThat(created.getConfig()).isNotNull();

        verify(agentEventRateLimiter, never()).checkOrThrow(any(), any());
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(), eq(EDGE_ID), eq(null));
    }

    @Test
    void withAssignedProfile_setsProfileAndEnqueuesUpdateEvent() {
        when(appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                TENANT_ID, AGENT_PROFILE_ID, AgentApplicationType.EDGE, EDGE_VERSION))
                .thenReturn(List.of(autoDiscoveryRelation(true)));

        AgentApplication created = creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose(EDGE_VERSION));

        assertThat(created.getApplicationProfileId()).isEqualTo(APP_PROFILE_ID);
        assertThat(created.getConfig()).isNull();

        verify(agentEventRateLimiter).checkOrThrow(TENANT_ID, AGENT_ID);
        ArgumentCaptor<AgentAppEventActionType> actionCaptor = ArgumentCaptor.forClass(AgentAppEventActionType.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(), eq(EDGE_ID), actionCaptor.capture());
        assertThat(actionCaptor.getValue()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void profileNotRelatedOnAutoDiscovery_isIgnored() {
        when(appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                TENANT_ID, AGENT_PROFILE_ID, AgentApplicationType.EDGE, EDGE_VERSION))
                .thenReturn(List.of(autoDiscoveryRelation(false)));

        AgentApplication created = creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose(EDGE_VERSION));

        assertThat(created.getApplicationProfileId()).isNull();
        assertThat(created.getConfig()).isNotNull();
        verify(agentEventRateLimiter, never()).checkOrThrow(any(), any());
    }

    @Test
    void rateLimitOnAssignedProfile_abortsBeforeSave() {
        when(appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                TENANT_ID, AGENT_PROFILE_ID, AgentApplicationType.EDGE, EDGE_VERSION))
                .thenReturn(List.of(autoDiscoveryRelation(true)));
        doThrow(new TbRateLimitsException("Agent events rate limit reached"))
                .when(agentEventRateLimiter).checkOrThrow(TENANT_ID, AGENT_ID);

        assertThatThrownBy(() -> creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose(EDGE_VERSION)))
                .isInstanceOf(TbRateLimitsException.class);

        verify(appProvisioner, never()).saveWithLifecycleEvent(any(), any(), any(), any());
    }

    @Test
    void unknownEdgeVersion_fallsBackToGenericTemplate() {
        AgentAppTemplate generic = template(AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion());
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, "9.9.9EDGEPE")).thenReturn(null);
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                AgentApplicationType.GENERIC, AgentAppConfigType.DOCKER_COMPOSE,
                AgentApplicationType.GENERIC.getDefaultVersion())).thenReturn(generic);
        when(appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                TENANT_ID, AGENT_PROFILE_ID, AgentApplicationType.GENERIC,
                AgentApplicationType.GENERIC.getDefaultVersion())).thenReturn(List.of());

        AgentApplication created = creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose("9.9.9EDGEPE"));

        assertThat(created.getAppType()).isEqualTo(AgentApplicationType.GENERIC);
        assertThat(created.getTemplateVersion()).isEqualTo(AgentApplicationType.GENERIC.getDefaultVersion());
    }

    @Test
    void noTemplateAtAll_returnsNull() {
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(any(), any(), any())).thenReturn(null);

        assertThat(creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME, edgeCompose(EDGE_VERSION))).isNull();
        verify(appProvisioner, never()).saveWithLifecycleEvent(any(), any(), any(), any());
    }

    @Test
    void imageWithoutTag_resolvesNullVersionAndFallsBackToGeneric() {
        AgentAppTemplate generic = template(AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion());
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, null)).thenReturn(null);
        when(templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                AgentApplicationType.GENERIC, AgentAppConfigType.DOCKER_COMPOSE,
                AgentApplicationType.GENERIC.getDefaultVersion())).thenReturn(generic);

        AgentApplication created = creator.createApp(TENANT_ID, AGENT_ID, PROJECT_NAME,
                compose("{\"services\":{\"tb-edge\":{\"image\":\"thingsboard/tb-edge-pe:\"}}}"));

        assertThat(created.getAppType()).isEqualTo(AgentApplicationType.GENERIC);
    }

    private static AgentAppProfileRelationInfo autoDiscoveryRelation(boolean relatesOnAutoDiscovery) {
        AgentAppProfile profile = new AgentAppProfile(APP_PROFILE_ID);
        profile.setTenantId(TENANT_ID);
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion(EDGE_VERSION);
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        additionalInfo.put(org.thingsboard.server.dao.agent.AgentProfileService.RELATES_ON_AUTO_DISCOVERY, relatesOnAutoDiscovery);
        return new AgentAppProfileRelationInfo(
                new org.thingsboard.server.common.data.agent.AgentAppProfileInfo(profile, EDGE_VERSION),
                AGENT_PROFILE_ID, null, additionalInfo);
    }

    private static AgentAppTemplate template(AgentApplicationType appType, String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(version);
        return template;
    }

    private static ComposeState edgeCompose(String version) {
        return compose("{\"services\":{\"tb-edge\":{\"image\":\"thingsboard/tb-edge-pe:" + version + "\"}}}");
    }

    private static ComposeState compose(String json) {
        return ComposeState.newBuilder().setComposeJson(json).build();
    }
}
