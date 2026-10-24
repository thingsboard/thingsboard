// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAutoInstallServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentProfileId AGENT_PROFILE_ID = new AgentProfileId(UUID.randomUUID());
    private static final AgentAppProfileId APPLICATION_PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final String TEMPLATE_VERSION = "1.0.0";
    private static final String EDGE_TEMPLATE_VERSION = "4.3.1EDGEPE";

    @Mock
    private AgentService agentService;
    @Mock
    private AgentProfileService agentProfileService;
    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentAppProvisioner appProvisioner;
    @Mock
    private AgentEventRateLimiter agentEventRateLimiter;
    @Mock
    private TbEdgeService tbEdgeService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private SystemSecurityService systemSecurityService;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private AgentAutoInstallService autoInstallService;

    private Agent agentWithProfile;

    @BeforeEach
    void setUp() {
        agentWithProfile = new Agent();
        agentWithProfile.setId(AGENT_ID);
        agentWithProfile.setTenantId(TENANT_ID);
        agentWithProfile.setAgentProfileId(AGENT_PROFILE_ID);
        agentWithProfile.setName("test-agent");
    }

    @Test
    void whenAgentNotFound_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(null);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenAgentHasNoProfile_shouldSkip() {
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setAgentProfileId(null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agent);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenNoPendingProfiles_shouldNotCreateAnything() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(Collections.emptyList());

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenGenericProfile_shouldCreateAppAndEvent_noRelatedEntity() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GENERIC, null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), appCaptor.capture(), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        AgentApplication saved = appCaptor.getValue();
        assertThat(saved.getAgentId()).isEqualTo(AGENT_ID);
        assertThat(saved.getApplicationProfileId()).isEqualTo(APPLICATION_PROFILE_ID);
        assertThat(saved.getTemplateVersion()).isEqualTo(TEMPLATE_VERSION);
        assertThat(saved.getAppType()).isEqualTo(AgentApplicationType.GENERIC);
        assertThat(saved.getOrigin()).isEqualTo(AgentApplicationOrigin.AUTO_PROVISIONED);
        assertThat(relatedCaptor.getValue()).isNull();

        verifyNoInteractions(tbEdgeService, deviceService);
    }

    @Test
    void whenEdgeProfile_shouldCreateEdgeWithRandomCredentials() throws Exception {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(systemSecurityService.getBaseUrl(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn("http://thingsboard.local");

        RuleChain edgeTemplateRootRuleChain = new RuleChain();
        edgeTemplateRootRuleChain.setId(new RuleChainId(UUID.randomUUID()));
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(edgeTemplateRootRuleChain);

        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        Edge createdEdge = new Edge();
        createdEdge.setId(edgeId);
        when(tbEdgeService.save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.<EntityGroup>emptyList()), isNull()))
                .thenReturn(createdEdge);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(tbEdgeService).save(edgeCaptor.capture(), eq(edgeTemplateRootRuleChain), eq(Collections.emptyList()), isNull());
        Edge edge = edgeCaptor.getValue();
        assertThat(edge.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(edge.getName()).startsWith("Edge-");
        assertThat(edge.getType()).isEqualTo("default");
        assertThat(edge.getRoutingKey()).isNotBlank().hasSize(20);
        assertThat(edge.getSecret()).isNotBlank().hasSize(20);
        assertThat(edge.getEdgeLicenseKey()).isNotBlank();
        assertThat(edge.getCloudEndpoint()).isEqualTo("http://thingsboard.local");

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(edgeId);
    }

    @Test
    void whenAddOnLicenseAndEdgeProfileBelowMinVersion_shouldNotCreateEdgeOrApp() {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        profile.setTemplateVersion("4.2.2.4EDGEPE");
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(new RuleChain());
        when(subscriptionService.getLicenseVersion()).thenReturn(2);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(tbEdgeService, appProvisioner);
    }

    @Test
    void whenLegacyLicenseAndEdgeProfileBelowMinVersion_shouldCreateEdge() throws Exception {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        profile.setTemplateVersion("3.9.0EDGEPE");
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        RuleChain edgeTemplateRootRuleChain = new RuleChain();
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(edgeTemplateRootRuleChain);
        when(subscriptionService.getLicenseVersion()).thenReturn(1);
        Edge createdEdge = new Edge();
        createdEdge.setId(new EdgeId(UUID.randomUUID()));
        when(tbEdgeService.save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.<EntityGroup>emptyList()), isNull()))
                .thenReturn(createdEdge);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(tbEdgeService).save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.emptyList()), isNull());
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), eq(createdEdge.getId()), eq(AgentAppEventActionType.INSTALL));
    }

    @Test
    void isSupportedEdgeVersion() {
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(2, "4.3.0EDGEPE")).isTrue();
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(2, "4.3.1.4EDGEPE")).isTrue();
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(2, "4.4.0EDGEPE")).isTrue();
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(2, "4.2.2.4EDGEPE")).isFalse();
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(2, "3.9.0EDGEPE")).isFalse();
        assertThat(AgentAutoInstallService.isSupportedEdgeVersion(1, "3.9.0EDGEPE")).isTrue();
    }

    @Test
    void whenGatewayProfileWithAccessToken_shouldCreateDeviceWithAutoToken() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("accessToken"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithAccessToken(any(Device.class), isNull())).thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDeviceWithAccessToken(deviceCaptor.capture(), isNull());
        assertThat(deviceCaptor.getValue().getName()).startsWith("Gateway-");
        assertThat(deviceCaptor.getValue().getAdditionalInfo().get(DataConstants.GATEWAY_PARAMETER).asBoolean()).isTrue();
        verify(deviceService, never()).saveDeviceWithCredentials(any(), any());

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(deviceId);
    }

    @Test
    void whenGatewayProfileWithUsernamePassword_shouldCreateDeviceWithMqttBasicCredentials() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("usernamePassword"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithCredentials(any(Device.class), any(DeviceCredentials.class)))
                .thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        ArgumentCaptor<DeviceCredentials> credsCaptor = ArgumentCaptor.forClass(DeviceCredentials.class);
        verify(deviceService).saveDeviceWithCredentials(deviceCaptor.capture(), credsCaptor.capture());
        assertThat(deviceCaptor.getValue().getAdditionalInfo().get(DataConstants.GATEWAY_PARAMETER).asBoolean()).isTrue();
        DeviceCredentials creds = credsCaptor.getValue();
        assertThat(creds.getCredentialsType()).isEqualTo(DeviceCredentialsType.MQTT_BASIC);
        assertThat(creds.getCredentialsValue()).contains("clientId").contains("userName").contains("password");
        verify(deviceService, never()).saveDeviceWithAccessToken(any(), any());

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(deviceId);
    }

    @Test
    void whenGatewayComposeOmitsSecurityType_shouldInferFromCredentialKeys() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfigWithEnv(
                "\"TB_GW_USERNAME\": \"user\", \"TB_GW_PASSWORD\": \"pass\""));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        Device createdDevice = new Device();
        createdDevice.setId(new DeviceId(UUID.randomUUID()));
        when(deviceService.saveDeviceWithCredentials(any(Device.class), any(DeviceCredentials.class)))
                .thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<DeviceCredentials> credsCaptor = ArgumentCaptor.forClass(DeviceCredentials.class);
        verify(deviceService).saveDeviceWithCredentials(any(Device.class), credsCaptor.capture());
        assertThat(credsCaptor.getValue().getCredentialsType()).isEqualTo(DeviceCredentialsType.MQTT_BASIC);
        verify(deviceService, never()).saveDeviceWithAccessToken(any(), any());
    }

    @Test
    void whenAppSaveFailsForEdgeProfile_shouldDeleteAutoCreatedEdge() throws Exception {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(systemSecurityService.getBaseUrl(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn("http://thingsboard.local");

        RuleChain edgeTemplateRootRuleChain = new RuleChain();
        edgeTemplateRootRuleChain.setId(new RuleChainId(UUID.randomUUID()));
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(edgeTemplateRootRuleChain);

        Edge createdEdge = new Edge();
        createdEdge.setId(new EdgeId(UUID.randomUUID()));
        when(tbEdgeService.save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.<EntityGroup>emptyList()), isNull()))
                .thenReturn(createdEdge);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(tbEdgeService).delete(createdEdge, null);
    }

    @Test
    void whenAppSaveFailsForGatewayProfile_shouldDeleteAutoCreatedDevice() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("accessToken"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithAccessToken(any(Device.class), isNull())).thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(deviceService).deleteDevice(TENANT_ID, deviceId);
    }

    @Test
    void whenRollbackFails_shouldNotPropagate() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("accessToken"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithAccessToken(any(Device.class), isNull())).thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"));
        org.mockito.Mockito.doThrow(new RuntimeException("rollback failed"))
                .when(deviceService).deleteDevice(TENANT_ID, deviceId);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(deviceService).deleteDevice(TENANT_ID, deviceId);
    }

    @Test
    void whenGenericProfileFails_shouldNotAttemptRollback() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GENERIC, null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(tbEdgeService, deviceService);
    }

    @Test
    void whenOneProfileFails_shouldContinueWithOthers() {
        AgentAppProfile failingProfile = newProfile(AgentApplicationType.GENERIC, null);
        failingProfile.setId(new AgentAppProfileId(UUID.randomUUID()));
        AgentAppProfile okProfile = newProfile(AgentApplicationType.GENERIC, null);

        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(failingProfile, okProfile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(appProvisioner, org.mockito.Mockito.times(2))
                .saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL));
    }

    @Test
    void whenAgentProfileNotFound_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(agentProfileService.findProfileById(TENANT_ID, AGENT_PROFILE_ID)).thenReturn(null);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenProvisionTypeDisabled_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.DISABLED);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenProvisionTypeNoAutoInstall_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.NO_AUTO_INSTALL);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenPerAppTypeProvisionType_shouldUsePerAppTypeQuery() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GENERIC, null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        when(profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(profileService, never()).findUninstalledAppProfilesForAgentProfile(any(), any(), any());
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL));
    }

    @Test
    void whenPerAppTypeWithDuplicateNonGenericTypes_shouldInstallFirstOnly() throws Exception {
        AgentAppProfile firstEdgeProfile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        AgentAppProfile secondEdgeProfile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        secondEdgeProfile.setId(new AgentAppProfileId(UUID.randomUUID()));

        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        when(profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(firstEdgeProfile, secondEdgeProfile));
        when(systemSecurityService.getBaseUrl(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn("http://thingsboard.local");

        RuleChain edgeTemplateRootRuleChain = new RuleChain();
        edgeTemplateRootRuleChain.setId(new RuleChainId(UUID.randomUUID()));
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(edgeTemplateRootRuleChain);

        Edge createdEdge = new Edge();
        createdEdge.setId(new EdgeId(UUID.randomUUID()));
        when(tbEdgeService.save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.<EntityGroup>emptyList()), isNull()))
                .thenReturn(createdEdge);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), appCaptor.capture(), any(), eq(AgentAppEventActionType.INSTALL));
        assertThat(appCaptor.getValue().getApplicationProfileId()).isEqualTo(firstEdgeProfile.getId());
    }

    @Test
    void whenPerAppTypeWithMultipleGenericProfiles_shouldInstallAll() {
        AgentAppProfile firstGenericProfile = newProfile(AgentApplicationType.GENERIC, null);
        AgentAppProfile secondGenericProfile = newProfile(AgentApplicationType.GENERIC, null);
        secondGenericProfile.setId(new AgentAppProfileId(UUID.randomUUID()));

        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        when(profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(firstGenericProfile, secondGenericProfile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(appProvisioner, org.mockito.Mockito.times(2))
                .saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL));
    }

    @Test
    void whenRateLimited_shouldNotCreateRelatedEntityAndShouldContinueWithNextProfile() {
        AgentAppProfile rejected = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        AgentAppProfile accepted = newProfile(AgentApplicationType.GENERIC, null);
        accepted.setId(new AgentAppProfileId(UUID.randomUUID()));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        mockAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(rejected, accepted));
        doThrow(new TbRateLimitsException(EntityType.AGENT))
                .doNothing()
                .when(agentEventRateLimiter).checkOrThrow(TENANT_ID, AGENT_ID);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(agentEventRateLimiter, times(2)).checkOrThrow(TENANT_ID, AGENT_ID);
        verifyNoInteractions(tbEdgeService, deviceService);
        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), appCaptor.capture(), any(), eq(AgentAppEventActionType.INSTALL));
        assertThat(appCaptor.getValue().getApplicationProfileId()).isEqualTo(accepted.getId());
    }

    private void mockAgentProfile(AgentProvisionType provisionType) {
        AgentProfile agentProfile = new AgentProfile();
        agentProfile.setId(AGENT_PROFILE_ID);
        agentProfile.setTenantId(TENANT_ID);
        agentProfile.setProvisionType(provisionType);
        when(agentProfileService.findProfileById(TENANT_ID, AGENT_PROFILE_ID)).thenReturn(agentProfile);
    }

    private AgentAppProfile newProfile(AgentApplicationType appType, DockerComposeConfig config) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setId(APPLICATION_PROFILE_ID);
        profile.setTenantId(TENANT_ID);
        profile.setName("profile-" + appType.name().toLowerCase());
        profile.setAppType(appType);
        profile.setTemplateVersion(appType == AgentApplicationType.EDGE ? EDGE_TEMPLATE_VERSION : TEMPLATE_VERSION);
        profile.setConfig(config);
        return profile;
    }

    private DockerComposeConfig emptyComposeConfig() {
        DockerComposeConfig cfg = new DockerComposeConfig();
        cfg.setCompose(JacksonUtil.newObjectNode());
        return cfg;
    }

    private DockerComposeConfig gatewayComposeConfig(String securityType) {
        return gatewayComposeConfigWithEnv("\"TB_GW_SECURITY_TYPE\": \"" + securityType + "\"");
    }

    private DockerComposeConfig gatewayComposeConfigWithEnv(String envEntries) {
        DockerComposeConfig cfg = new DockerComposeConfig();
        String json = "{\n" +
                "  \"services\": {\n" +
                "    \"tb-gateway\": {\n" +
                "      \"image\": \"thingsboard/tb-gateway:latest\",\n" +
                "      \"environment\": {\n" +
                "        " + envEntries + "\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonNode node = JacksonUtil.toJsonNode(json);
        cfg.setCompose(node);
        return cfg;
    }
}
