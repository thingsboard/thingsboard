// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.AckStatus;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true",
        "agents.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
public class AutoInstallAgentTest extends AbstractControllerTest {

    @DynamicPropertySource
    static void agentTestProps(DynamicPropertyRegistry registry) {
        registry.add("edges.rpc.port", () -> AbstractAgentTest.AGENT_PORT);
    }

    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentApplicationService agentApplicationService;
    @Autowired
    private AgentAppProfileService agentAppProfileService;
    @Autowired
    private AppTemplateRegistry appTemplateRegistry;
    @Autowired
    private EdgeService edgeService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    private AgentImitator imitator;
    private AgentProfileId profileId;
    private AgentId agentId;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() {
        if (imitator != null) {
            try {
                imitator.disconnect();
            } catch (Exception ignored) {
            }
        }
        try {
            loginTenantAdmin();
            if (agentId != null) {
                try {
                    doDelete("/api/agent/" + agentId.getId()).andExpect(status().isOk());
                } catch (Exception ignored) {
                }
            }
            if (profileId != null) {
                try {
                    doDelete("/api/agent/profile/" + profileId.getId()).andExpect(status().isOk());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testAutoInstallCreatesAppAndDispatchesInstallCommandOnInitialSyncComplete() throws Exception {
        AgentAppTemplate template = getOrCreateGenericTemplate();
        AgentAppProfile profile = createProfile("auto-install-profile", template);
        Agent provisioned = provisionAndConnectAgent(profile);
        Assert.assertEquals(profileId, provisioned.getAgentProfileId());
        Assert.assertTrue(imitator.getHelloAck().getSuccess());

        // Sanity: no apps yet because InitialSyncComplete hasn't been sent
        Assert.assertEquals(0, agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().size());

        imitator.expectMessageAmount(1);
        imitator.sendInitialSyncComplete();

        AgentApplication created = awaitAutoInstalledApp();
        Assert.assertEquals(1, agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().size());
        Assert.assertEquals(AgentApplicationOrigin.AUTO_PROVISIONED, created.getOrigin());
        Assert.assertEquals(profile.getId(), created.getApplicationProfileId());
        Assert.assertEquals(template.getCurrentVersion(), created.getTemplateVersion());

        imitator.waitForMessages();
        AppCommand command = imitator.getLatestCommand();
        Assert.assertEquals(AppCommandAction.APP_INSTALL, command.getAction());

        AgentAppEventId eventId = new AgentAppEventId(new java.util.UUID(
                command.getCommandId().getIdMSB(), command.getCommandId().getIdLSB()));
        imitator.sendCommandAck(command.getCommandId(), AckStatus.ACCEPTED);
        imitator.sendCommandResult(command.getCommandId(), command.getStepId(), true);

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    var ev = agentAppEventService.findById(tenantId, eventId);
                    return ev != null && ev.getProcessingStatus() == AgentProcessingStatus.FINISHED;
                });
    }

    @Test
    public void testAutoInstallIsIdempotentAcrossReconnects() throws Exception {
        AgentAppTemplate template = getOrCreateGenericTemplate();
        AgentAppProfile profile = createProfile("idempotent-profile", template);
        provisionAndAwaitAutoInstalledApp(profile);
        Assert.assertEquals(1, agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().size());

        // Drain the first install command so it does not interfere with the reconnect check
        imitator.waitForMessages();

        // Assign a SECOND app profile so the reconnect's auto-install pass has new work to do.
        // The pass builds the uninstalled-profile list (which now excludes the already-installed
        // first profile) and provisions only the second profile. Awaiting the second app is a
        // positive signal that the pass ran to completion and evaluated the first profile
        // idempotently — no fixed sleep, no race-prone negative wait.
        AgentAppProfile secondProfile = createProfile("idempotent-profile-2", template);
        assignAppProfileToAgentProfile(profileId, secondProfile.getId());

        // Reconnect and re-trigger auto-install
        Agent provisioned = agentService.findAgentById(tenantId, agentId);
        imitator.disconnect();
        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT,
                provisioned.getRoutingKey(), provisioned.getSecret());
        imitator.connect();
        imitator.sendInitialSyncComplete();

        // Positive completion signal: the second profile's app has been auto-installed
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(tenantId, agentId, new PageLink(100))
                        .getData().stream()
                        .anyMatch(a -> secondProfile.getId().equals(a.getApplicationProfileId())));

        // Idempotency: the first profile still has exactly one app (it was not re-provisioned),
        // and the reconnect provisioned exactly the two assigned profiles.
        List<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(100)).getData();
        long firstProfileApps = apps.stream()
                .filter(a -> profile.getId().equals(a.getApplicationProfileId()))
                .count();
        Assert.assertEquals("First profile must not be re-provisioned on reconnect",
                1, firstProfileApps);
        Assert.assertEquals("Reconnect should provision exactly the two assigned profiles",
                2, apps.size());
    }

    @Test
    public void testAutoInstallEdgeProfile() throws Exception {
        AgentAppTemplate template = createEdgeTemplate();
        AgentAppProfile profile = createProfile("auto-install-edge", template);
        AgentApplicationInfo app = provisionAndAwaitAutoInstalledApp(profile);

        Assert.assertEquals(AgentApplicationOrigin.AUTO_PROVISIONED, app.getOrigin());
        Assert.assertNotNull("Auto-installed EDGE app should reference the created Edge", app.getRelatedEntityId());
        Assert.assertEquals(EntityType.EDGE, app.getRelatedEntityId().getEntityType());

        Edge createdEdge = edgeService.findEdgeById(tenantId, (EdgeId) app.getRelatedEntityId());
        Assert.assertNotNull("Edge entity should exist after successful auto-install", createdEdge);
        Assert.assertTrue("EdgeDataValidator requires non-blank edgeLicenseKey",
                createdEdge.getEdgeLicenseKey() != null && !createdEdge.getEdgeLicenseKey().isBlank());
        Assert.assertTrue("EdgeDataValidator requires non-blank cloudEndpoint",
                createdEdge.getCloudEndpoint() != null && !createdEdge.getCloudEndpoint().isBlank());
    }

    @Test
    public void testAutoInstallGatewayProfileWithAccessToken() throws Exception {
        AgentAppTemplate template = createGatewayTemplate("accessToken");
        AgentAppProfile profile = createProfile("auto-install-gateway-token", template);
        AgentApplicationInfo app = provisionAndAwaitAutoInstalledApp(profile);

        Assert.assertEquals(AgentApplicationOrigin.AUTO_PROVISIONED, app.getOrigin());
        Assert.assertNotNull("Auto-installed GATEWAY app should reference the created Device", app.getRelatedEntityId());
        Assert.assertEquals(EntityType.DEVICE, app.getRelatedEntityId().getEntityType());

        DeviceId deviceId = (DeviceId) app.getRelatedEntityId();
        Device createdDevice = deviceService.findDeviceById(tenantId, deviceId);
        Assert.assertNotNull("Device entity should exist after successful auto-install", createdDevice);

        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        Assert.assertNotNull("Auto-created gateway Device should have credentials", credentials);
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
        Assert.assertTrue("Access token should be non-blank",
                credentials.getCredentialsId() != null && !credentials.getCredentialsId().isBlank());
    }

    @Test
    public void testAutoInstallGatewayProfileWithUsernamePassword() throws Exception {
        AgentAppTemplate template = createGatewayTemplate("usernamePassword");
        AgentAppProfile profile = createProfile("auto-install-gateway-mqtt", template);
        AgentApplicationInfo app = provisionAndAwaitAutoInstalledApp(profile);

        Assert.assertNotNull(app.getRelatedEntityId());
        Assert.assertEquals(EntityType.DEVICE, app.getRelatedEntityId().getEntityType());

        DeviceId deviceId = (DeviceId) app.getRelatedEntityId();
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        Assert.assertNotNull(credentials);
        Assert.assertEquals(DeviceCredentialsType.MQTT_BASIC, credentials.getCredentialsType());
        Assert.assertTrue("MQTT_BASIC credentials value must contain clientId/userName/password",
                credentials.getCredentialsValue() != null
                        && credentials.getCredentialsValue().contains("clientId")
                        && credentials.getCredentialsValue().contains("userName")
                        && credentials.getCredentialsValue().contains("password"));
    }

    @Test
    public void testAutoInstallSkippedWhenAgentProfileHasNoAppProfiles() throws Exception {
        // Agent profile with no profiles assigned
        AgentProfile agentProfile = createAgentProfile();
        profileId = agentProfile.getId();

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), agentProfile.getProvisionSecret());
        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, imitator.getProvisionResponse().getRoutingKey());
        agentId = provisioned.getId();

        imitator.connect();
        imitator.sendInitialSyncComplete();

        // No apps should ever be created — assert the count stays empty over a bounded window
        // instead of blocking on a fixed sleep.
        Awaitility.await()
                .during(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(
                        tenantId, agentId, new PageLink(10)).getData().isEmpty());
    }

    @Test
    public void testNoAutoInstallStrategySkipsAssignedProfiles() throws Exception {
        AgentAppProfile profile = createProfile("no-auto-install-profile", getOrCreateGenericTemplate());
        provisionAndConnectAgent(profile, AgentProvisionType.NO_AUTO_INSTALL);
        imitator.sendInitialSyncComplete();

        // Assigned profile must be ignored — assert the count stays empty over a bounded window
        Awaitility.await()
                .during(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(
                        tenantId, agentId, new PageLink(10)).getData().isEmpty());
    }

    @Test
    public void testPerAppTypeBlocksSecondAppOfSameTypeAcrossReconnects() throws Exception {
        AgentAppProfile edgeProfile = createProfile("per-type-edge", createEdgeTemplate());
        provisionAndAwaitAutoInstalledApp(edgeProfile, AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        imitator.waitForMessages();

        // Second EDGE profile with a DIFFERENT template version: the per-profile strategy would
        // install it, the per-type strategy must not — the agent already has an EDGE app.
        AgentAppProfile secondEdgeProfile = createProfile("per-type-edge-2", createEdgeTemplate("3.8.0"));
        assignAppProfileToAgentProfile(profileId, secondEdgeProfile.getId());
        // GENERIC profile as positive completion signal for the reconnect's auto-install pass
        AgentAppProfile genericProfile = createProfile("per-type-generic-signal", getOrCreateGenericTemplate());
        assignAppProfileToAgentProfile(profileId, genericProfile.getId());

        Agent provisioned = agentService.findAgentById(tenantId, agentId);
        imitator.disconnect();
        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT,
                provisioned.getRoutingKey(), provisioned.getSecret());
        imitator.connect();
        imitator.sendInitialSyncComplete();

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(tenantId, agentId, new PageLink(100))
                        .getData().stream()
                        .anyMatch(a -> genericProfile.getId().equals(a.getApplicationProfileId())));

        List<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(100)).getData();
        Assert.assertEquals("Only the first EDGE app and the GENERIC signal app are expected", 2, apps.size());
        Assert.assertEquals("Agent must keep a single EDGE app under per-type strategy",
                1, apps.stream().filter(a -> a.getAppType() == AgentApplicationType.EDGE).count());
        Assert.assertTrue("Second EDGE profile must not be provisioned",
                apps.stream().noneMatch(a -> secondEdgeProfile.getId().equals(a.getApplicationProfileId())));
    }

    @Test
    public void testPerAppTypeInstallsSingleAppWhenMultipleProfilesOfSameTypeAssigned() throws Exception {
        AgentAppProfile firstEdgeProfile = createProfile("per-type-dup-edge-1", createEdgeTemplate());
        AgentAppProfile secondEdgeProfile = createProfile("per-type-dup-edge-2", createEdgeTemplate("3.8.0"));
        AgentAppProfile genericProfile = createProfile("per-type-dup-generic", getOrCreateGenericTemplate());

        provisionAndConnectAgent(firstEdgeProfile, AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        assignAppProfileToAgentProfile(profileId, secondEdgeProfile.getId());
        assignAppProfileToAgentProfile(profileId, genericProfile.getId());
        imitator.sendInitialSyncComplete();

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(tenantId, agentId, new PageLink(100))
                        .getData().stream()
                        .anyMatch(a -> genericProfile.getId().equals(a.getApplicationProfileId())));

        List<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(100)).getData();
        Assert.assertEquals(2, apps.size());
        AgentApplication edgeApp = apps.stream()
                .filter(a -> a.getAppType() == AgentApplicationType.EDGE)
                .findFirst().orElseThrow();
        Assert.assertEquals("The earliest-created EDGE profile must win",
                firstEdgeProfile.getId(), edgeApp.getApplicationProfileId());
    }

    @Test
    public void testPerAppTypeInstallsGenericProfilesPerProfile() throws Exception {
        AgentAppTemplate template = getOrCreateGenericTemplate();
        AgentAppProfile firstGenericProfile = createProfile("per-type-generic-1", template);
        AgentAppProfile secondGenericProfile = createProfile("per-type-generic-2", template);

        provisionAndConnectAgent(firstGenericProfile, AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        assignAppProfileToAgentProfile(profileId, secondGenericProfile.getId());
        imitator.sendInitialSyncComplete();

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> agentApplicationService.findByAgentId(tenantId, agentId, new PageLink(100))
                        .getData().size() == 2);

        // one app per GENERIC profile, not two apps from whichever profile was picked first
        List<AgentApplication> apps = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(100)).getData();
        Assertions.assertThat(apps).extracting(AgentApplication::getApplicationProfileId)
                .containsExactlyInAnyOrder(firstGenericProfile.getId(), secondGenericProfile.getId());
    }

    // --- Helpers ---

    /**
     * For GENERIC app type, BaseAgentApplicationService.resolveTemplateId() overrides the
     * templateId on new apps with a lookup by (appType, DOCKER_COMPOSE, defaultVersion).
     * To keep the test's expected templateId consistent with what the save pipeline will
     * resolve, we return whatever that same lookup returns — creating one if none exists.
     * This also makes the test resilient to ordering/pollution across test methods.
     */
    private AgentAppTemplate getOrCreateGenericTemplate() {
        AgentAppTemplate existing = appTemplateRegistry.get(
                AgentApplicationType.GENERIC, AgentAppConfigType.DOCKER_COMPOSE, AgentApplicationType.GENERIC.getDefaultVersion());
        if (existing != null) {
            return existing;
        }
        JsonNode compose = JacksonUtil.toJsonNode(
                "{\"services\":{\"my-app\":{\"image\":\"my-app:1.0\"}}}");

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion(AgentApplicationType.GENERIC.getDefaultVersion());
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);

        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy compose");
        template.setStartSteps(AgentTemplateTestSupport.withComposeTemplate(List.of(step), compose));

        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }


    private AgentAppProfile createProfile(String name, AgentAppTemplate template) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name + "-" + System.nanoTime());
        profile.setAppType(template.getAppType());
        profile.setTemplateVersion(template.getCurrentVersion());
        profile.setConfig(AgentTemplateTestSupport.resolveAppConfig(template));
        return doPost("/api/agent/app/profile", profile, AgentAppProfile.class);
    }

    private AgentAppTemplate createEdgeTemplate() {
        return createEdgeTemplate("3.7.0");
    }

    private AgentAppTemplate createEdgeTemplate(String version) {
        return saveAppTemplate(AgentApplicationType.EDGE, version,
                "{\"services\":{\"tb-edge\":{" +
                        "\"image\":\"thingsboard/tb-edge-pe:" + version + "\"," +
                        "\"environment\":{" +
                        "\"CLOUD_ROUTING_KEY\":\"placeholder\"," +
                        "\"CLOUD_ROUTING_SECRET\":\"placeholder\"," +
                        "\"CLOUD_RPC_HOST\":\"thingsboard.local\"" +
                        "}}}}");
    }

    private AgentAppTemplate createGatewayTemplate(String securityType) {
        String envBlock = "accessToken".equals(securityType)
                ? "\"TB_GW_SECURITY_TYPE\":\"accessToken\"," +
                  "\"TB_GW_ACCESS_TOKEN\":\"placeholder-token\""
                : "\"TB_GW_SECURITY_TYPE\":\"usernamePassword\"," +
                  "\"TB_GW_CLIENT_ID\":\"placeholder-client\"," +
                  "\"TB_GW_USERNAME\":\"placeholder-user\"," +
                  "\"TB_GW_PASSWORD\":\"placeholder-pass\"";
        return saveAppTemplate(AgentApplicationType.GATEWAY, "1.0.0",
                "{\"services\":{\"tb-gateway\":{" +
                        "\"image\":\"thingsboard/tb-gateway:3.7.0\"," +
                        "\"environment\":{" + envBlock + "}}}}");
    }

    private AgentAppTemplate saveAppTemplate(AgentApplicationType appType, String version, String composeJson) {
        JsonNode compose = JacksonUtil.toJsonNode(composeJson);

        AgentAppTemplate template = new AgentAppTemplate();
        template.setTenantId(tenantId);
        template.setAppType(appType);
        template.setCurrentVersion(version);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);

        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Deploy " + appType.name().toLowerCase() + " compose");
        template.setStartSteps(AgentTemplateTestSupport.withComposeTemplate(List.of(step), compose));

        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }

    private Agent provisionAndConnectAgent(AgentAppProfile appProfile) throws Exception {
        return provisionAndConnectAgent(appProfile, AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
    }

    private Agent provisionAndConnectAgent(AgentAppProfile appProfile, AgentProvisionType provisionType) throws Exception {
        AgentProfile agentProfile = createAgentProfile(provisionType);
        assignAppProfileToAgentProfile(agentProfile.getId(), appProfile.getId());
        profileId = agentProfile.getId();

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), agentProfile.getProvisionSecret());
        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertTrue("Provision failed: " + resp.getErrorMessage(), resp.getSuccess());

        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull(provisioned);
        agentId = provisioned.getId();

        imitator.connect();
        return provisioned;
    }

    private AgentApplicationInfo awaitAutoInstalledApp() {
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> !agentApplicationService.findByAgentId(
                        tenantId, agentId, new PageLink(10)).getData().isEmpty());
        AgentApplication app = agentApplicationService.findByAgentId(
                tenantId, agentId, new PageLink(10)).getData().get(0);
        return agentApplicationService.findInfoById(tenantId, app.getId());
    }

    private AgentApplicationInfo provisionAndAwaitAutoInstalledApp(AgentAppProfile appProfile) throws Exception {
        return provisionAndAwaitAutoInstalledApp(appProfile, AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
    }

    private AgentApplicationInfo provisionAndAwaitAutoInstalledApp(AgentAppProfile appProfile, AgentProvisionType provisionType) throws Exception {
        provisionAndConnectAgent(appProfile, provisionType);
        imitator.expectMessageAmount(1);
        imitator.sendInitialSyncComplete();
        return awaitAutoInstalledApp();
    }

    private AgentProfile createAgentProfile() {
        return createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE);
    }

    private AgentProfile createAgentProfile(AgentProvisionType provisionType) {
        AgentProfile g = new AgentProfile();
        g.setName("auto-install-agentProfile-" + System.nanoTime());
        g.setProvisionType(provisionType);
        try {
            return doPost("/api/agent/profile", g, AgentProfile.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assignAppProfileToAgentProfile(AgentProfileId agentProfileId, AgentAppProfileId appProfileId) throws Exception {
        doPost("/api/agent/profile/" + agentProfileId.getId() + "/appProfile/" + appProfileId.getId())
                .andExpect(status().isOk());
    }

    // Autowired services that AbstractControllerTest doesn't expose directly
    @Autowired
    private org.thingsboard.server.dao.agent.AgentAppEventService agentAppEventService;
}
