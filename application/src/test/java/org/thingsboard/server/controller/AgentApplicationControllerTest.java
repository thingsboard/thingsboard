// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.EntitiesLimitExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Mechanism-A permission model: agent applications and app events carry no
 * Resource of their own and are gated through the parent AGENT (checkAgentAppId / checkAgentAppEventId
 * delegate to checkAgentId).
 */
@DaoSqlTest
public class AgentApplicationControllerTest extends AbstractControllerTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    AgentAppProfileService agentAppProfileService;
    @Autowired
    AppTemplateRegistry appTemplateRegistry;

    private AgentApplication application;
    private AgentAppEvent event;

    @Before
    public void setUpAgentApp() throws Exception {
        loginTenantAdmin();

        Agent newAgent = new Agent();
        newAgent.setName("Perm Test Agent");
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        Agent agent = doPost("/api/agent", newAgent, Agent.class);

        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion(AgentApplicationType.GENERIC.getDefaultVersion());
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        template = registerTemplate(template);

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName("Perm Test App");
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(genericCompose());
        app.setConfig(config);
        application = agentApplicationService.save(tenantId, app);

        AgentAppEvent appEvent = new AgentAppEvent();
        appEvent.setTenantId(tenantId);
        appEvent.setApplicationId(application.getId());
        appEvent.setAgentId(agent.getId());
        appEvent.setApplicationName(application.getName());
        appEvent.setActionType(AgentAppEventActionType.INSTALL);
        appEvent.setStartStatus(ProcessingStartStatus.PENDING);
        appEvent.setProcessingStatus(AgentProcessingStatus.PENDING);
        appEvent.setUpdatedTime(System.currentTimeMillis());
        event = agentAppEventService.save(tenantId, appEvent);
    }

    @Test
    public void testGetAgentApplicationById_allowedWithAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT, List.of(Operation.READ)));
        doGet("/api/agent/app/" + application.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testGetAgentApplicationById_deniedWithoutAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/app/" + application.getId().getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testGetAgentAppEventById_allowedWithAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT, List.of(Operation.READ)));
        doGet("/api/agent/app/event/" + event.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testGetAgentAppEventById_deniedWithoutAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/app/event/" + event.getId().getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testAttachToProfile_differentTemplateVersion_adoptsProfileVersion() throws Exception {
        loginTenantAdmin();

        registerTemplate(edgeTemplate("1.0.0", "2.0.0"));
        registerTemplate(edgeTemplate("2.0.0", null));

        AgentApplication edgeApp = new AgentApplication();
        edgeApp.setTenantId(tenantId);
        edgeApp.setAgentId(application.getAgentId());
        edgeApp.setAppType(AgentApplicationType.EDGE);
        edgeApp.setName("Attach Test App");
        edgeApp.setTemplateVersion("1.0.0");
        edgeApp.setOrigin(AgentApplicationOrigin.INSTALLED);
        edgeApp.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig appConfig = new DockerComposeConfig();
        appConfig.setCompose(edgeCompose("attach-app-rk"));
        edgeApp.setConfig(appConfig);
        edgeApp = agentApplicationService.save(tenantId, edgeApp);

        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("Attach Profile v2");
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion("2.0.0");
        DockerComposeConfig profileConfig = new DockerComposeConfig();
        profileConfig.setCompose(edgeCompose("attach-profile-rk"));
        profile.setConfig(profileConfig);
        profile = agentAppProfileService.saveProfile(profile);

        AgentApplication attached = doPost(
                "/api/agent/app/" + edgeApp.getId().getId() + "/attach/" + profile.getId().getId(),
                AgentApplication.class);
        Assert.assertEquals(profile.getId(), attached.getApplicationProfileId());
        Assert.assertEquals("2.0.0", attached.getTemplateVersion());
        Assert.assertNull(attached.getDesiredTemplateVersion());

        AgentApplicationInfo info = doGet("/api/agent/app/" + edgeApp.getId().getId(), AgentApplicationInfo.class);
        Assert.assertEquals("2.0.0", info.getTemplateVersion());
        Assert.assertEquals("2.0.0", info.getProfileTemplateVersion());
    }

    @Test
    public void testAttachToProfile_pendingDesiredVersion_keepsTheApplicationVersion() throws Exception {
        loginTenantAdmin();

        registerTemplate(edgeTemplate("1.1.0", "2.1.0"));
        registerTemplate(edgeTemplate("2.1.0", null));

        AgentApplication edgeApp = saveEdgeApp("Attach Pending App", "1.1.0", "attach-pending-rk");
        edgeApp.setDesiredTemplateVersion("2.1.0");
        edgeApp = agentApplicationService.save(tenantId, edgeApp);
        Assert.assertEquals("2.1.0", edgeApp.getDesiredTemplateVersion());

        AgentAppProfile profile = saveEdgeProfile("Attach Pending Profile v2", "2.1.0", "attach-pending-profile-rk");

        AgentApplication attached = doPost(
                "/api/agent/app/" + edgeApp.getId().getId() + "/attach/" + profile.getId().getId(),
                AgentApplication.class);
        Assert.assertEquals(profile.getId(), attached.getApplicationProfileId());
        Assert.assertEquals("1.1.0", attached.getTemplateVersion());
        Assert.assertEquals("2.1.0", attached.getDesiredTemplateVersion());
    }

    @Test
    public void testDetachFromProfile() throws Exception {
        loginTenantAdmin();

        registerTemplate(edgeTemplate("1.2.0", null));
        AgentApplication edgeApp = saveEdgeApp("Detach Test App", "1.2.0", "detach-app-rk");
        AgentAppProfile profile = saveEdgeProfile("Detach Test Profile", "1.2.0", "detach-profile-rk");

        AgentApplication attached = doPost(
                "/api/agent/app/" + edgeApp.getId().getId() + "/attach/" + profile.getId().getId(),
                AgentApplication.class);
        Assert.assertEquals(profile.getId(), attached.getApplicationProfileId());

        AgentApplication detached = doPost("/api/agent/app/" + edgeApp.getId().getId() + "/detach", AgentApplication.class);
        Assert.assertNull(detached.getApplicationProfileId());
        Assert.assertEquals("1.2.0", detached.getTemplateVersion());

        // a second detach has nothing to detach from
        doPost("/api/agent/app/" + edgeApp.getId().getId() + "/detach").andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateAgentApplication() throws Exception {
        loginTenantAdmin();

        application.setName("Perm Test App renamed");
        AgentApplication updated = doPut("/api/agent/app", application, AgentApplication.class);
        Assert.assertEquals("Perm Test App renamed", updated.getName());

        AgentApplicationInfo reloaded = doGet("/api/agent/app/" + application.getId().getId(), AgentApplicationInfo.class);
        Assert.assertEquals("Perm Test App renamed", reloaded.getName());
    }

    @Test
    public void testGetAgentAppUnitsAndEvents_paging() throws Exception {
        loginTenantAdmin();

        PageData<AgentAppUnit> units = doGetTypedWithPageLink(
                "/api/agent/app/" + application.getId().getId() + "/units?", new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(0, units.getTotalElements());

        PageData<AgentAppEvent> events = doGetTypedWithPageLink(
                "/api/agent/app/" + application.getId().getId() + "/events?", new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(1, events.getTotalElements());
        Assert.assertEquals(event.getId(), events.getData().get(0).getId());
    }

    @Test
    public void testRelatedEntityAssignLookupAndUnassign() throws Exception {
        loginTenantAdmin();

        registerTemplate(gatewayTemplate("3.8.1", modernGatewayCompose("3.8.1")));
        Device device = createGatewayDevice("Related Entity Gateway Device");
        DeviceCredentials credentials = doGet("/api/device/" + device.getId().getId() + "/credentials", DeviceCredentials.class);

        AgentApplication gatewayApp = new AgentApplication();
        gatewayApp.setTenantId(tenantId);
        gatewayApp.setAgentId(application.getAgentId());
        gatewayApp.setAppType(AgentApplicationType.GATEWAY);
        gatewayApp.setName("Related Entity Gateway App");
        gatewayApp.setTemplateVersion("3.8.1");
        gatewayApp.setOrigin(AgentApplicationOrigin.INSTALLED);
        gatewayApp.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(modernGatewayCompose("3.8.1"));
        gatewayApp.setConfig(config);
        gatewayApp = agentApplicationService.save(tenantId, gatewayApp);

        AgentApplication assigned = doPost("/api/agent/app/" + gatewayApp.getId().getId()
                + "/relatedEntity/DEVICE/" + device.getId().getId(), AgentApplication.class);
        // assign re-merges the gateway credentials into the compose
        Assert.assertEquals(credentials.getCredentialsId(), gatewayEnv(assigned).get("TB_GW_ACCESS_TOKEN").asText());
        AgentApplicationInfo assignedInfo = doGet("/api/agent/app/" + gatewayApp.getId().getId(), AgentApplicationInfo.class);
        Assert.assertEquals(device.getId(), assignedInfo.getRelatedEntityId());

        AgentApplication byRelatedEntity = doGet("/api/agent/apps/DEVICE/" + device.getId().getId(), AgentApplication.class);
        Assert.assertEquals(gatewayApp.getId(), byRelatedEntity.getId());

        List<EntityId> managed = doGetTyped("/api/agent/app/managedRelatedEntities/DEVICE", new TypeReference<>() {});
        Assert.assertTrue(managed.stream().anyMatch(id -> id.getId().equals(device.getId().getId())));

        doDelete("/api/agent/app/" + gatewayApp.getId().getId() + "/relatedEntity", AgentApplication.class);
        AgentApplicationInfo unassignedInfo = doGet("/api/agent/app/" + gatewayApp.getId().getId(), AgentApplicationInfo.class);
        Assert.assertNull(unassignedInfo.getRelatedEntityId());
        doGet("/api/agent/apps/DEVICE/" + device.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testGetManagedRelatedEntityIds_unsupportedEntityType() throws Exception {
        loginTenantAdmin();
        doGet("/api/agent/app/managedRelatedEntities/NOT_AN_ENTITY_TYPE").andExpect(status().isBadRequest());
    }

    @Test
    public void testGetRelatedEntityCandidates_gatewayDevices() throws Exception {
        loginTenantAdmin();

        registerTemplate(gatewayTemplate("3.8.1", modernGatewayCompose("3.8.1")));
        Device gateway = createGatewayDevice("Candidate Gateway Alpha");
        Device managedGateway = createGatewayDevice("Candidate Gateway Beta");
        Device plainDevice = new Device();
        plainDevice.setName("Candidate Plain Device");
        plainDevice = doPost("/api/device", plainDevice, Device.class);

        AgentApplication gatewayApp = saveGatewayApp("Candidate Gateway App");
        doPost("/api/agent/app/" + gatewayApp.getId().getId()
                + "/relatedEntity/DEVICE/" + managedGateway.getId().getId(), AgentApplication.class);

        String url = "/api/agent/app/relatedEntityCandidates?entityType=DEVICE&pageSize=100&page=0";
        List<UUID> candidates = candidateIds(url);
        Assert.assertTrue(candidates.contains(gateway.getId().getId()));
        Assert.assertFalse(candidates.contains(managedGateway.getId().getId()));
        Assert.assertFalse(candidates.contains(plainDevice.getId().getId()));

        List<UUID> withCurrent = candidateIds(url + "&currentEntityId=" + managedGateway.getId().getId());
        Assert.assertTrue(withCurrent.contains(gateway.getId().getId()));
        Assert.assertTrue(withCurrent.contains(managedGateway.getId().getId()));
        Assert.assertFalse(withCurrent.contains(plainDevice.getId().getId()));

        PageData<EntityInfo> searched = doGetTyped(url + "&textSearch=alpha", new TypeReference<>() {});
        Assert.assertEquals(1, searched.getData().size());
        Assert.assertEquals(gateway.getId(), searched.getData().get(0).getId());
        Assert.assertEquals(gateway.getName(), searched.getData().get(0).getName());
    }

    @Test
    public void testGetRelatedEntityCandidates_edgesSortedByName() throws Exception {
        loginTenantAdmin();

        doPost("/api/edge", constructEdge("Candidate Edge B", "default"), Edge.class);
        doPost("/api/edge", constructEdge("Candidate Edge A", "default"), Edge.class);

        PageData<EntityInfo> page = doGetTyped(
                "/api/agent/app/relatedEntityCandidates?entityType=EDGE&pageSize=1&page=0&textSearch=candidate",
                new TypeReference<>() {});
        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertTrue(page.hasNext());
        Assert.assertEquals(1, page.getData().size());
        Assert.assertEquals("Candidate Edge A", page.getData().get(0).getName());
        Assert.assertEquals(EntityType.EDGE, page.getData().get(0).getId().getEntityType());
    }

    @Test
    public void testGetRelatedEntityCandidates_unsupportedEntityType() throws Exception {
        loginTenantAdmin();
        doGet("/api/agent/app/relatedEntityCandidates?entityType=ASSET&pageSize=10&page=0").andExpect(status().isBadRequest());
        doGet("/api/agent/app/relatedEntityCandidates?entityType=NOT_AN_ENTITY_TYPE&pageSize=10&page=0").andExpect(status().isBadRequest());
    }

    private List<UUID> candidateIds(String url) throws Exception {
        PageData<EntityInfo> page = doGetTyped(url, new TypeReference<>() {});
        return page.getData().stream().map(info -> info.getId().getId()).toList();
    }

    @Test
    public void testSaveAgentApplication_tenantProfileLimitReached_thenRejectedUntilAppDeleted() throws Exception {
        long limit = 3;
        updateDefaultTenantProfileConfig(config -> config.setMaxAgentApplications(limit));
        try {
            List<AgentApplication> apps = new ArrayList<>(List.of(application));
            for (int i = 1; i < limit; i++) {
                apps.add(saveGenericApp("Limit App " + i));
            }

            EntitiesLimitExceededException exception = Assert.assertThrows(EntitiesLimitExceededException.class,
                    () -> saveGenericApp("Limit App Out Of Limit"));
            Assert.assertEquals(EntityType.AGENT_APPLICATION, exception.getEntityType());
            Assert.assertEquals(limit, exception.getLimit());

            agentApplicationService.delete(tenantId, apps.get(1).getId());
            Assert.assertNotNull(saveGenericApp("Limit App After Delete").getId());
        } finally {
            updateDefaultTenantProfileConfig(config -> config.setMaxAgentApplications(0));
        }
    }

    private AgentApplication saveGenericApp(String name) {
        AgentApplication genericApp = new AgentApplication();
        genericApp.setTenantId(tenantId);
        genericApp.setAgentId(application.getAgentId());
        genericApp.setAppType(AgentApplicationType.GENERIC);
        genericApp.setName(name);
        genericApp.setTemplateVersion(application.getTemplateVersion());
        genericApp.setOrigin(AgentApplicationOrigin.INSTALLED);
        genericApp.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(genericCompose());
        genericApp.setConfig(config);
        return agentApplicationService.save(tenantId, genericApp);
    }

    private AgentApplication saveGatewayApp(String name) {
        AgentApplication gatewayApp = new AgentApplication();
        gatewayApp.setTenantId(tenantId);
        gatewayApp.setAgentId(application.getAgentId());
        gatewayApp.setAppType(AgentApplicationType.GATEWAY);
        gatewayApp.setName(name);
        gatewayApp.setTemplateVersion("3.8.1");
        gatewayApp.setOrigin(AgentApplicationOrigin.INSTALLED);
        gatewayApp.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(modernGatewayCompose("3.8.1"));
        gatewayApp.setConfig(config);
        return agentApplicationService.save(tenantId, gatewayApp);
    }

    // --- cross-tenant isolation of the child entities gated through the parent AGENT ---

    @Test
    public void testGetAgentApplicationById_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/app/" + application.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentAppEventById_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/app/event/" + event.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testAttachToProfile_crossTenantDenied() throws Exception {
        loginTenantAdmin();
        registerTemplate(edgeTemplate("1.3.0", null));
        AgentApplication edgeApp = saveEdgeApp("Cross Tenant Attach App", "1.3.0", "cross-attach-app-rk");
        AgentAppProfile profile = saveEdgeProfile("Cross Tenant Attach Profile", "1.3.0", "cross-attach-profile-rk");

        loginDifferentTenant();
        doPost("/api/agent/app/" + edgeApp.getId().getId() + "/attach/" + profile.getId().getId())
                .andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentAppUnits_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/app/" + application.getId().getId() + "/units?pageSize=100&page=0")
                .andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    private AgentApplication saveEdgeApp(String name, String templateVersion, String routingKey) {
        AgentApplication edgeApp = new AgentApplication();
        edgeApp.setTenantId(tenantId);
        edgeApp.setAgentId(application.getAgentId());
        edgeApp.setAppType(AgentApplicationType.EDGE);
        edgeApp.setName(name);
        edgeApp.setTemplateVersion(templateVersion);
        edgeApp.setOrigin(AgentApplicationOrigin.INSTALLED);
        edgeApp.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig appConfig = new DockerComposeConfig();
        appConfig.setCompose(edgeCompose(routingKey));
        edgeApp.setConfig(appConfig);
        return agentApplicationService.save(tenantId, edgeApp);
    }

    private AgentAppProfile saveEdgeProfile(String name, String templateVersion, String routingKey) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateVersion(templateVersion);
        DockerComposeConfig profileConfig = new DockerComposeConfig();
        profileConfig.setCompose(edgeCompose(routingKey));
        profile.setConfig(profileConfig);
        return agentAppProfileService.saveProfile(profile);
    }

    @Test
    public void testMergePreview_gatewayLegacyCompose_injectsLegacyEnvNames() throws Exception {
        loginTenantAdmin();
        registerTemplate(gatewayTemplate("3.6.3", legacyGatewayCompose("3.6.3")));
        Device device = createGatewayDevice("Legacy Gateway Device");
        DeviceCredentials credentials = doGet("/api/device/" + device.getId().getId() + "/credentials", DeviceCredentials.class);

        AgentApplication merged = doPost("/api/agent/app/merge/3.6.3/preview?appType=GATEWAY&composeType=default&setHostValues=true"
                + "&relatedEntityType=DEVICE&relatedEntityId=" + device.getId().getId(), AgentApplication.class);

        JsonNode env = gatewayEnv(merged);
        Assert.assertEquals(credentials.getCredentialsId(), env.get("accessToken").asText());
        Assert.assertEquals("host.docker.internal", env.get("host").asText());
        Assert.assertEquals("1883", env.get("port").asText());
        Assert.assertFalse(env.has("TB_GW_ACCESS_TOKEN"));
        Assert.assertFalse(env.has("TB_GW_SECURITY_TYPE"));
        Assert.assertFalse(env.has("TB_GW_HOST"));
        Assert.assertFalse(env.has("TB_GW_PORT"));
    }

    @Test
    public void testMergePreview_gatewayModernCompose_injectsTbGwEnvNames() throws Exception {
        loginTenantAdmin();
        registerTemplate(gatewayTemplate("3.8.0", modernGatewayCompose("3.8.0")));
        Device device = createGatewayDevice("Modern Gateway Device");
        DeviceCredentials credentials = doGet("/api/device/" + device.getId().getId() + "/credentials", DeviceCredentials.class);

        AgentApplication merged = doPost("/api/agent/app/merge/3.8.0/preview?appType=GATEWAY&composeType=default&setHostValues=true"
                + "&relatedEntityType=DEVICE&relatedEntityId=" + device.getId().getId(), AgentApplication.class);

        JsonNode env = gatewayEnv(merged);
        Assert.assertEquals("accessToken", env.get("TB_GW_SECURITY_TYPE").asText());
        Assert.assertEquals(credentials.getCredentialsId(), env.get("TB_GW_ACCESS_TOKEN").asText());
        Assert.assertEquals("host.docker.internal", env.get("TB_GW_HOST").asText());
        Assert.assertEquals("1883", env.get("TB_GW_PORT").asText());
        Assert.assertFalse(env.has("accessToken"));
        Assert.assertFalse(env.has("host"));
    }

    private Device createGatewayDevice(String name) {
        Device device = new Device();
        device.setName(name);
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("gateway", true));
        return doPost("/api/device", device, Device.class);
    }

    private AgentAppTemplate gatewayTemplate(String version, ObjectNode compose) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GATEWAY);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(version);
        ComposeTypeChoiceStep choiceStep = new ComposeTypeChoiceStep(UUID.randomUUID(), null, "Choose compose type");
        choiceStep.setComposeTemplates(Map.of("default", compose));
        template.setStartSteps(List.of(choiceStep));
        return template;
    }

    private ObjectNode legacyGatewayCompose(String version) {
        ObjectNode env = JacksonUtil.newObjectNode()
                .put("host", "PLACEHOLDER_HOST")
                .put("port", "PLACEHOLDER_PORT")
                .put("accessToken", "PLACEHOLDER_TOKEN");
        return gatewayCompose(version, env);
    }

    private ObjectNode modernGatewayCompose(String version) {
        ObjectNode env = JacksonUtil.newObjectNode()
                .put("TB_GW_HOST", "PLACEHOLDER_HOST")
                .put("TB_GW_PORT", "PLACEHOLDER_PORT")
                .put("TB_GW_SECURITY_TYPE", "PLACEHOLDER_TYPE")
                .put("TB_GW_ACCESS_TOKEN", "PLACEHOLDER_TOKEN");
        return gatewayCompose(version, env);
    }

    private ObjectNode gatewayCompose(String version, ObjectNode env) {
        ObjectNode service = JacksonUtil.newObjectNode().put("image", "thingsboard/tb-gateway:" + version);
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-gateway", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private JsonNode gatewayEnv(AgentApplication app) {
        return ((DockerComposeConfig) app.getConfig()).getCompose()
                .get("services").get("tb-gateway").get("environment");
    }

    private AgentAppTemplate registerTemplate(AgentAppTemplate template) {
        return org.thingsboard.server.agent.AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }

    private AgentAppTemplate edgeTemplate(String currentVersion, String nextVersion) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.EDGE);
        template.setCurrentVersion(currentVersion);
        template.setNextVersion(nextVersion);
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return template;
    }

    private ObjectNode edgeCompose(String routingKey) {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", "secret");
        env.put("CLOUD_RPC_HOST", "");
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private ObjectNode genericCompose() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

}
