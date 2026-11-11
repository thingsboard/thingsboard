// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentAppProfileServiceTest extends AbstractServiceTest {

    @Autowired
    AgentAppProfileService profileService;
    @Autowired
    AgentApplicationService agentApplicationService;

    @Test
    public void testSaveAndFind() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Test Profile", template);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        AgentAppProfile found = profileService.findProfileById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());
        Assert.assertEquals("Test Profile", found.getName());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveEdgeProfile_withValidCredentialKeys() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Valid Edge Profile", template);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveEdgeProfile_missingCredentialKeys_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("Bad Edge Profile");
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion(template.getCurrentVersion());

        // Config with edge service but missing credential env vars
        DockerComposeConfig config = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", JacksonUtil.newObjectNode());
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        config.setCompose(compose);
        profile.setConfig(config);

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_accessToken_valid() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW AccessToken", template, "accessToken",
                env -> env.put("TB_GW_ACCESS_TOKEN", "placeholder"));

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveGatewayProfile_accessToken_missingToken_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad AccessToken", template, "accessToken",
                env -> {});

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_usernamePassword_valid() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW UserPass", template, "usernamePassword",
                env -> {
                    env.put("TB_GW_CLIENT_ID", "placeholder");
                    env.put("TB_GW_USERNAME", "placeholder");
                    env.put("TB_GW_PASSWORD", "placeholder");
                });

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveGatewayProfile_usernamePassword_missingKeys_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad UserPass", template, "usernamePassword",
                env -> env.put("TB_GW_CLIENT_ID", "placeholder"));

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_unsupportedSecurityType_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad Type", template, "x509",
                env -> {});

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGenericProfile_noCredentialValidation() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("Generic Profile");
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateVersion(template.getCurrentVersion());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveProfile_nullConfig_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("No Config");
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion(template.getCurrentVersion());

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testUpdateProfile_validationStillRuns() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Update Test", template);
        AgentAppProfile saved = profileService.saveProfile(profile);

        // Now update with invalid config (missing keys)
        DockerComposeConfig badConfig = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", JacksonUtil.newObjectNode());
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        badConfig.setCompose(compose);
        saved.setConfig(badConfig);

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(saved));

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testFindProfilesByTenantIdAndAppTypeAndTemplateVersion() throws Exception {
        AgentAppTemplate template = createTemplate();

        AgentAppProfile first = createEdgeProfile("Finder Edge A", template);
        first.setTemplateVersion("2.0.0");
        first = profileService.saveProfile(first);
        Thread.sleep(5);
        AgentAppProfile second = createEdgeProfile("Finder Edge B", template);
        second.setTemplateVersion("2.0.0");
        second = profileService.saveProfile(second);
        AgentAppProfile otherVersion = createEdgeProfile("Finder Edge C", template);
        otherVersion.setTemplateVersion("3.0.0");
        otherVersion = profileService.saveProfile(otherVersion);
        AgentAppProfile otherType = createGatewayProfile("Finder GW", template, "accessToken",
                env -> env.put("TB_GW_ACCESS_TOKEN", "placeholder"));
        otherType.setTemplateVersion("2.0.0");
        otherType = profileService.saveProfile(otherType);

        List<AgentAppProfile> found = profileService.findProfilesByTenantIdAndAppTypeAndTemplateVersion(
                tenantId, AgentApplicationType.EDGE, "2.0.0");
        Assert.assertEquals(2, found.size());
        Assert.assertEquals(first.getId(), found.get(0).getId());
        Assert.assertEquals(second.getId(), found.get(1).getId());

        profileService.deleteProfile(tenantId, first.getId());
        profileService.deleteProfile(tenantId, second.getId());
        profileService.deleteProfile(tenantId, otherVersion.getId());
        profileService.deleteProfile(tenantId, otherType.getId());
    }

    @Test
    public void testFindUninstalledByAppType_returnsAssignedProfilesOrderedByCreatedTime() throws Exception {
        AgentAppTemplate edgeTemplate = registerTemplate(AgentApplicationType.EDGE, "1.0.0");
        AgentAppTemplate genericTemplate = registerTemplate(AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion());
        AgentProfile agentProfile = createAgentProfileEntity();
        Agent agent = createAgent();

        AgentAppProfile edgeProfile = profileService.saveProfile(createEdgeProfile("ByType Edge " + UUID.randomUUID(), edgeTemplate));
        Thread.sleep(5);
        AgentAppProfile genericProfile = profileService.saveProfile(createGenericProfile("ByType Generic " + UUID.randomUUID(), genericTemplate));
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), edgeProfile.getId());
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), genericProfile.getId());

        List<AgentAppProfile> result = profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(
                tenantId, agentProfile.getId(), agent.getId());

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(edgeProfile.getId(), result.get(0).getId());
        Assert.assertEquals(genericProfile.getId(), result.get(1).getId());

        cleanup(agent, agentProfile, edgeProfile, genericProfile);
    }

    @Test
    public void testFindUninstalledByAppType_excludesTypeWithExistingApp_evenPendingDeletion() {
        AgentAppTemplate edgeTemplate = registerTemplate(AgentApplicationType.EDGE, "1.0.0");
        AgentProfile agentProfile = createAgentProfileEntity();
        Agent agent = createAgent();

        AgentAppProfile edgeProfile = profileService.saveProfile(createEdgeProfile("ByType Blocked Edge " + UUID.randomUUID(), edgeTemplate));
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), edgeProfile.getId());
        // EDGE app from a different (null) profile, mid-uninstall — still occupies the EDGE slot
        saveApplication(agent, AgentApplicationType.EDGE, "1.0.0", null, true);

        List<AgentAppProfile> result = profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(
                tenantId, agentProfile.getId(), agent.getId());

        Assert.assertTrue(result.isEmpty());

        cleanup(agent, agentProfile, edgeProfile);
    }

    @Test
    public void testFindUninstalledPerProfile_templateMatchIsScopedToSameAppType() {
        // GENERIC apps must use the type's default version, so the colliding version is shared
        String sharedVersion = AgentApplicationType.GENERIC.getDefaultVersion();
        registerTemplate(AgentApplicationType.GENERIC, sharedVersion);
        AgentAppTemplate gatewayTemplate = registerTemplate(AgentApplicationType.GATEWAY, sharedVersion);
        AgentProfile agentProfile = createAgentProfileEntity();
        Agent agent = createAgent();

        AgentAppProfile gatewayProfile = profileService.saveProfile(createGatewayProfile(
                "PerProfile GW " + UUID.randomUUID(), gatewayTemplate, "accessToken",
                env -> env.put("TB_GW_ACCESS_TOKEN", "placeholder")));
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), gatewayProfile.getId());

        // GENERIC app with the same templateVersion must NOT block the GATEWAY profile
        saveApplication(agent, AgentApplicationType.GENERIC, sharedVersion, null, false);
        List<AgentAppProfile> result = profileService.findUninstalledAppProfilesForAgentProfile(
                tenantId, agentProfile.getId(), agent.getId());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(gatewayProfile.getId(), result.get(0).getId());

        // GATEWAY app with the same templateVersion (from another profile) DOES block it
        saveApplication(agent, AgentApplicationType.GATEWAY, sharedVersion, null, false);
        result = profileService.findUninstalledAppProfilesForAgentProfile(
                tenantId, agentProfile.getId(), agent.getId());
        Assert.assertTrue(result.isEmpty());

        cleanup(agent, agentProfile, gatewayProfile);
    }

    @Test
    public void testFindUninstalledByAppType_genericMatchesByProfileId() {
        String genericVersion = AgentApplicationType.GENERIC.getDefaultVersion();
        AgentAppTemplate genericTemplate = registerTemplate(AgentApplicationType.GENERIC, genericVersion);
        AgentProfile agentProfile = createAgentProfileEntity();
        Agent agent = createAgent();

        AgentAppProfile installedGeneric = profileService.saveProfile(createGenericProfile("ByType Generic Installed " + UUID.randomUUID(), genericTemplate));
        AgentAppProfile pendingGeneric = profileService.saveProfile(createGenericProfile("ByType Generic Pending " + UUID.randomUUID(), genericTemplate));
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), installedGeneric.getId());
        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), pendingGeneric.getId());
        saveApplication(agent, AgentApplicationType.GENERIC, genericVersion, installedGeneric.getId(), false);

        List<AgentAppProfile> result = profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(
                tenantId, agentProfile.getId(), agent.getId());

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(pendingGeneric.getId(), result.get(0).getId());

        cleanup(agent, agentProfile, installedGeneric, pendingGeneric);
    }

    @Test
    public void testSaveProfile_duplicateNameInSameTenant_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile saved = profileService.saveProfile(createEdgeProfile("Duplicate Name", template));

        AgentAppProfile duplicate = createEdgeProfile("Duplicate Name", template);
        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> profileService.saveProfile(duplicate));
        Assert.assertEquals("Agent application profile with such name already exists!", exception.getMessage());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testDeleteProfile_referencedByApplication_throws() {
        AgentAppTemplate template = registerTemplate(AgentApplicationType.EDGE, "1.0.0");
        Agent agent = createAgent();
        AgentAppProfile appProfile = profileService.saveProfile(createEdgeProfile("Referenced " + UUID.randomUUID(), template));
        saveApplication(agent, AgentApplicationType.EDGE, "1.0.0", appProfile.getId(), false);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> profileService.deleteProfile(tenantId, appProfile.getId()));
        Assert.assertEquals("The application profile referenced by agent applications cannot be deleted!", exception.getMessage());

        agentApplicationService.deleteByAgentId(tenantId, agent.getId());
        agentService.deleteAgent(tenantId, agent.getId());
        profileService.deleteProfile(tenantId, appProfile.getId());
    }

    @Test
    public void testDeleteByTenantId_removesEveryProfileOfTheTenant() {
        AgentAppTemplate template = createTemplate();
        profileService.saveProfile(createEdgeProfile("Tenant Wide A", template));
        profileService.saveProfile(createEdgeProfile("Tenant Wide B", template));
        Assert.assertEquals(2, profileService.findProfilesByTenantId(tenantId, new PageLink(100)).getTotalElements());

        profileService.deleteByTenantId(tenantId);

        Assert.assertTrue(profileService.findProfilesByTenantId(tenantId, new PageLink(100)).getData().isEmpty());
    }

    // ==================== Helpers ====================

    private AgentAppTemplate registerTemplate(AgentApplicationType appType, String version) {
        return registerAppTemplate(createAppTemplate(appType, version));
    }

    private AgentProfile createAgentProfileEntity() {
        return createAgentProfile(tenantId, "byType-agent-profile-" + UUID.randomUUID());
    }

    private Agent createAgent() {
        return createAgent(tenantId, "byType-agent-" + UUID.randomUUID());
    }

    private AgentApplication saveApplication(Agent agent, AgentApplicationType appType, String templateVersion, AgentAppProfileId appProfileId, boolean pendingDeletion) {
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(appType);
        app.setName("byType-app-" + UUID.randomUUID());
        app.setTemplateVersion(templateVersion);
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setApplicationProfileId(appProfileId);
        app.setPendingDeletion(pendingDeletion);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        app.setConfig(config);
        return agentApplicationService.save(tenantId, app);
    }

    private AgentAppProfile createGenericProfile(String name, AgentAppTemplate template) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateVersion(template.getCurrentVersion());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);
        return profile;
    }

    private void cleanup(Agent agent, AgentProfile agentProfile, AgentAppProfile... appProfiles) {
        agentApplicationService.deleteByAgentId(tenantId, agent.getId());
        agentService.deleteAgent(tenantId, agent.getId());
        for (AgentAppProfile appProfile : appProfiles) {
            agentProfileService.unassignAppProfileFromAgentProfile(tenantId, agentProfile.getId(), appProfile.getId());
            profileService.deleteProfile(tenantId, appProfile.getId());
        }
        agentProfileService.deleteProfile(tenantId, agentProfile.getId());
    }

    private AgentAppTemplate createTemplate() {
        return registerAppTemplate(createAppTemplate(AgentApplicationType.GENERIC, "1.0.0"));
    }

    private AgentAppProfile createEdgeProfile(String name, AgentAppTemplate template) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion(template.getCurrentVersion());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeCompose());
        profile.setConfig(config);
        return profile;
    }

    private ObjectNode createEdgeCompose() {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("CLOUD_ROUTING_KEY", "placeholder");
        env.put("CLOUD_ROUTING_SECRET", "placeholder");
        env.put("CLOUD_RPC_HOST", "localhost");

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private AgentAppProfile createGatewayProfile(String name, AgentAppTemplate template,
                                                   String securityType, java.util.function.Consumer<ObjectNode> envCustomizer) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GATEWAY);
        profile.setTemplateVersion(template.getCurrentVersion());

        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", securityType);
        envCustomizer.accept(env);

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.8.0");
        service.set("environment", env);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-gateway", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        profile.setConfig(config);
        return profile;
    }
}
