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
import org.thingsboard.server.agent.AgentTemplateTestSupport;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentAppProfileControllerTest extends AbstractControllerTest {

    @Autowired
    AppTemplateRegistry appTemplateRegistry;
    @Autowired
    AgentApplicationService agentApplicationService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteAppProfile() throws Exception {
        AgentAppProfile saved = createAppProfile("Controller App Profile");
        Assert.assertNotNull(saved.getId());

        AgentAppProfile found = doGet("/api/agent/app/profile/" + saved.getId().getId(), AgentAppProfile.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testSaveAppProfile_customerForbidden() throws Exception {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Customer Denied App Profile");
        profile.setAppType(AgentApplicationType.GENERIC);

        loginCustomerUser();
        doPost("/api/agent/app/profile", profile).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testMergeForPreview() throws Exception {
        AgentAppTemplate template = createTemplate();

        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Merge Preview Profile");
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateVersion(template.getCurrentVersion());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);

        AgentAppProfile merged = doPost("/api/agent/app/profiles/merge/" + template.getCurrentVersion() + "/preview",
                profile, AgentAppProfile.class);
        Assert.assertNotNull(merged);
        Assert.assertEquals(AgentApplicationType.GENERIC, merged.getAppType());
    }

    @Test
    public void testGetAgentAppProfilesByAppType() throws Exception {
        AgentAppProfile saved = createAppProfile("By Type Profile");

        List<AgentAppProfileInfo> infos = doGetTyped(
                "/api/agent/app/profiles/" + AgentApplicationType.GENERIC.name(), new TypeReference<>() {});
        Assert.assertTrue(infos.stream().anyMatch(p -> p.getId().equals(saved.getId())));
    }

    @Test
    public void testGetAgentAppProfilesByIds() throws Exception {
        AgentAppProfile first = createAppProfile("By Ids Profile A");
        AgentAppProfile second = createAppProfile("By Ids Profile B");
        createAppProfile("By Ids Profile C");

        List<AgentAppProfile> profiles = doGetTyped("/api/agent/app/profiles?agentAppProfileIds="
                + first.getId().getId() + "," + second.getId().getId() + "," + UUID.randomUUID(), new TypeReference<>() {});
        Assert.assertEquals(2, profiles.size());
        Assert.assertEquals(first.getId(), profiles.get(0).getId());
        Assert.assertEquals(first.getName(), profiles.get(0).getName());
        Assert.assertEquals(second.getId(), profiles.get(1).getId());
    }

    @Test
    public void testGetAgentAppProfilesByIds_crossTenantReturnsNothing() throws Exception {
        AgentAppProfile saved = createAppProfile("By Ids Cross Tenant Profile");

        loginDifferentTenant();
        List<AgentAppProfile> profiles = doGetTyped("/api/agent/app/profiles?agentAppProfileIds=" + saved.getId().getId(),
                new TypeReference<>() {});
        Assert.assertTrue(profiles.isEmpty());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgentAppProfilesByIds_withoutReadPermissionReturnsNothing() throws Exception {
        AgentAppProfile saved = createAppProfile("By Ids Restricted Profile");

        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        List<AgentAppProfile> profiles = doGetTyped("/api/agent/app/profiles?agentAppProfileIds=" + saved.getId().getId(),
                new TypeReference<>() {});
        Assert.assertTrue(profiles.isEmpty());

        loginTenantAdmin();
    }

    @Test
    public void testGetTenantAgentAppProfiles_paging() throws Exception {
        createAppProfile("Paging App Profile A");
        createAppProfile("Paging App Profile B");

        PageData<AgentAppProfile> page = doGetTypedWithPageLink("/api/tenant/agent/app/profiles?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testGetAppProfile_crossTenantDenied() throws Exception {
        AgentAppProfile saved = createAppProfile("Cross Tenant App Profile");

        loginDifferentTenant();
        doGet("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().isNotFound());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgentAppProfileInfoById() throws Exception {
        AgentAppProfile saved = createAppProfile("Info App Profile");

        AgentAppProfileInfo info = doGet("/api/agent/app/profile/info/" + saved.getId().getId(), AgentAppProfileInfo.class);
        Assert.assertEquals(saved.getId(), info.getId());
        Assert.assertEquals(saved.getName(), info.getName());
    }

    @Test
    public void testGetAgentAppProfileInfo_crossTenantDenied() throws Exception {
        AgentAppProfile saved = createAppProfile("Info Cross Tenant App Profile");

        loginDifferentTenant();
        doGet("/api/agent/app/profile/info/" + saved.getId().getId()).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testDeleteAppProfile_referencedByApplication_isRejected() throws Exception {
        // a GENERIC application must carry the app type's default version, so the profile has to use it too
        String genericVersion = AgentApplicationType.GENERIC.getDefaultVersion();
        AgentAppTemplate template = AgentTemplateTestSupport.registerGenericTemplate(appTemplateRegistry, genericVersion);
        AgentAppProfile profile = doPost("/api/agent/app/profile",
                AgentTemplateTestSupport.appProfileFor(template, "Referenced App Profile"), AgentAppProfile.class);

        Agent newAgent = new Agent();
        newAgent.setName("App Profile Ref Agent");
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        Agent agent = doPost("/api/agent", newAgent, Agent.class);

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName("App Referencing Profile");
        app.setTemplateVersion(genericVersion);
        app.setApplicationProfileId(profile.getId());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        app.setConfig(config);
        agentApplicationService.save(tenantId, app);

        doDelete("/api/agent/app/profile/" + profile.getId().getId())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("referenced by agent applications")));

        doGet("/api/agent/app/profile/" + profile.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testMaterializeAppProfile_createsProfile() throws Exception {
        createEdgeTemplate("9.9.9EDGEPE");

        AgentAppProfile created = doPost("/api/agent/app/profile/materialize/EDGE/9.9.9EDGEPE", AgentAppProfile.class);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("Edge 9.9.9EDGEPE", created.getName());
        Assert.assertEquals(AgentApplicationType.EDGE, created.getAppType());
        Assert.assertEquals("9.9.9EDGEPE", created.getTemplateVersion());
        DockerComposeConfig config = (DockerComposeConfig) created.getConfig();
        Assert.assertNotNull(config.getCompose());
        Assert.assertEquals("in_memory", config.getComposeType());
    }

    @Test
    public void testMaterializeAppProfile_respectsComposeTypeParam() throws Exception {
        createEdgeTemplate("4.4.4EDGEPE", "in_memory", "kafka", "hybrid");

        AgentAppProfile created = doPost("/api/agent/app/profile/materialize/EDGE/4.4.4EDGEPE?composeType=kafka", AgentAppProfile.class);
        DockerComposeConfig config = (DockerComposeConfig) created.getConfig();
        Assert.assertEquals("kafka", config.getComposeType());
    }

    @Test
    public void testMaterializeAppProfile_reusesExistingForSameVersion() throws Exception {
        createEdgeTemplate("8.8.8EDGEPE");

        AgentAppProfile first = doPost("/api/agent/app/profile/materialize/EDGE/8.8.8EDGEPE", AgentAppProfile.class);
        AgentAppProfile second = doPost("/api/agent/app/profile/materialize/EDGE/8.8.8EDGEPE", AgentAppProfile.class);
        Assert.assertEquals(first.getId(), second.getId());
    }

    @Test
    public void testMaterializeAppProfile_reusesRenamedProfile() throws Exception {
        createEdgeTemplate("7.7.7EDGEPE");

        AgentAppProfile created = doPost("/api/agent/app/profile/materialize/EDGE/7.7.7EDGEPE", AgentAppProfile.class);
        created.setName("Edge PROD renamed");
        AgentAppProfile renamed = doPost("/api/agent/app/profile", created, AgentAppProfile.class);

        AgentAppProfile materialized = doPost("/api/agent/app/profile/materialize/EDGE/7.7.7EDGEPE", AgentAppProfile.class);
        Assert.assertEquals(renamed.getId(), materialized.getId());
        Assert.assertEquals("Edge PROD renamed", materialized.getName());
    }

    @Test
    public void testMaterializeAppProfile_returnsOldestWhenMultiple() throws Exception {
        AgentAppTemplate template = createEdgeTemplate("6.6.6EDGEPE");

        AgentAppProfile oldest = doPost("/api/agent/app/profile/materialize/EDGE/6.6.6EDGEPE", AgentAppProfile.class);

        AgentAppProfile another = new AgentAppProfile();
        another.setName("Edge second for 6.6.6EDGEPE");
        another.setAppType(AgentApplicationType.EDGE);
        another.setTemplateVersion(template.getCurrentVersion());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(edgeCompose("6.6.6EDGEPE"));
        another.setConfig(config);
        doPost("/api/agent/app/profile", another, AgentAppProfile.class);

        AgentAppProfile materialized = doPost("/api/agent/app/profile/materialize/EDGE/6.6.6EDGEPE", AgentAppProfile.class);
        Assert.assertEquals(oldest.getId(), materialized.getId());
    }

    @Test
    public void testMaterializeAppProfile_unknownVersion() throws Exception {
        doPost("/api/agent/app/profile/materialize/EDGE/0.0.0UNKNOWN").andExpect(status().isNotFound());
    }

    @Test
    public void testMaterializeAppProfile_customerForbidden() throws Exception {
        createEdgeTemplate("5.5.5EDGEPE");

        loginCustomerUser();
        doPost("/api/agent/app/profile/materialize/EDGE/5.5.5EDGEPE").andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    private AgentAppTemplate createEdgeTemplate(String version, String... composeTypes) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.EDGE);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(version);
        ComposeTypeChoiceStep choiceStep = new ComposeTypeChoiceStep(UUID.randomUUID(), null, "Choose compose type");
        String[] types = composeTypes.length > 0 ? composeTypes : new String[]{"in_memory"};
        Map<String, JsonNode> composeTemplates = new HashMap<>();
        for (String composeType : types) {
            composeTemplates.put(composeType, edgeCompose(version));
        }
        choiceStep.setComposeTemplates(composeTemplates);
        template.setStartSteps(List.of(choiceStep));
        return registerTemplate(template);
    }

    private ObjectNode edgeCompose(String version) {
        ObjectNode env = JacksonUtil.newObjectNode()
                .put("CLOUD_ROUTING_KEY", "")
                .put("CLOUD_ROUTING_SECRET", "")
                .put("CLOUD_RPC_HOST", "");
        ObjectNode service = JacksonUtil.newObjectNode().put("image", "thingsboard/tb-edge-pe:" + version);
        service.set("environment", env);
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private AgentAppTemplate createTemplate() {
        return AgentTemplateTestSupport.registerGenericTemplate(appTemplateRegistry);
    }

    private AgentAppProfile createAppProfile(String name) throws Exception {
        return doPost("/api/agent/app/profile", AgentTemplateTestSupport.appProfileFor(createTemplate(), name), AgentAppProfile.class);
    }

    private AgentAppTemplate registerTemplate(AgentAppTemplate template) {
        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }
}
