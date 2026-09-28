// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.agent.AgentTemplateTestSupport;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.agent.AppTemplateRegistry;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentAppTemplateControllerTest extends AbstractControllerTest {

    @Autowired
    AppTemplateRegistry appTemplateRegistry;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testGetLatestByAppTypeAndConfigType() throws Exception {
        registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");
        AgentAppTemplate latest = registerTemplate(AgentApplicationType.EDGE, "4.4.0EDGEPE");

        AgentAppTemplate found = doGet("/api/agent/app/template/EDGE/DOCKER_COMPOSE/latest", AgentAppTemplate.class);

        Assert.assertEquals(latest.getCurrentVersion(), found.getCurrentVersion());
        Assert.assertEquals(AgentApplicationType.EDGE, found.getAppType());
    }

    @Test
    public void testGetByCurrentVersion() throws Exception {
        registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");
        registerTemplate(AgentApplicationType.EDGE, "4.4.0EDGEPE");

        AgentAppTemplate found = doGet(
                "/api/agent/app/template/EDGE/DOCKER_COMPOSE/version/4.3.0EDGEPE", AgentAppTemplate.class);

        Assert.assertEquals("4.3.0EDGEPE", found.getCurrentVersion());
    }

    @Test
    public void testGetByUnknownCurrentVersion() throws Exception {
        registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");

        doGet("/api/agent/app/template/EDGE/DOCKER_COMPOSE/version/9.9.9EDGEPE")
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetGatewayLatestTagIsNotShadowedByTheHeadLookup() throws Exception {
        AgentAppTemplate floating = registerTemplate(AgentApplicationType.GATEWAY, "latest");
        AgentAppTemplate head = registerTemplate(AgentApplicationType.GATEWAY, "3.8-stable");

        AgentAppTemplate byVersion = doGet(
                "/api/agent/app/template/GATEWAY/DOCKER_COMPOSE/version/latest", AgentAppTemplate.class);
        AgentAppTemplate latest = doGet(
                "/api/agent/app/template/GATEWAY/DOCKER_COMPOSE/latest", AgentAppTemplate.class);

        Assert.assertEquals(floating.getCurrentVersion(), byVersion.getCurrentVersion());
        Assert.assertEquals(head.getCurrentVersion(), latest.getCurrentVersion());
    }

    @Test
    public void testGetTemplatesByAppType() throws Exception {
        registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");
        registerTemplate(AgentApplicationType.EDGE, "4.4.0EDGEPE");

        List<AgentAppTemplate> templates = doGetTyped(
                "/api/agent/app/templates/EDGE/DOCKER_COMPOSE", new TypeReference<>() {});

        // the registry is a JVM-wide singleton, so assert on containment rather than on the total
        Assertions.assertThat(templates).extracting(AgentAppTemplate::getCurrentVersion)
                .contains("4.3.0EDGEPE", "4.4.0EDGEPE");
    }

    @Test
    public void testGetAllTemplates() throws Exception {
        AgentAppTemplate edge = registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");
        AgentAppTemplate gateway = registerTemplate(AgentApplicationType.GATEWAY, "3.7.0");

        List<AgentAppTemplate> templates = doGetTyped(
                "/api/agent/app/templates", new TypeReference<>() {});

        Assert.assertTrue(templates.stream()
                .anyMatch(t -> edge.getCurrentVersion().equals(t.getCurrentVersion())));
        Assert.assertTrue(templates.stream()
                .anyMatch(t -> gateway.getCurrentVersion().equals(t.getCurrentVersion())));
    }

    @Test
    public void testCustomerUserIsNotAllowed() throws Exception {
        registerTemplate(AgentApplicationType.EDGE, "4.3.0EDGEPE");

        loginCustomerUser();
        doGet("/api/agent/app/template/EDGE/DOCKER_COMPOSE/latest").andExpect(status().isForbidden());
        doGet("/api/agent/app/template/EDGE/DOCKER_COMPOSE/version/4.3.0EDGEPE").andExpect(status().isForbidden());
        doGet("/api/agent/app/templates/EDGE/DOCKER_COMPOSE").andExpect(status().isForbidden());
        doGet("/api/agent/app/templates").andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    private AgentAppTemplate registerTemplate(AgentApplicationType appType, String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(version);
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return AgentTemplateTestSupport.register(appTemplateRegistry, template);
    }
}
