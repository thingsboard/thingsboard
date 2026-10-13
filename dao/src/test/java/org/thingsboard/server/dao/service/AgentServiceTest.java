// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class AgentServiceTest extends AbstractServiceTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    CustomerService customerService;

    private IdComparator<Agent> idComparator = new IdComparator<>();

    @Test
    public void testSaveAgent() {
        Agent agent = newAgent("My agent");
        Agent savedAgent = agentService.saveAgent(agent);

        Assert.assertNotNull(savedAgent);
        Assert.assertNotNull(savedAgent.getId());
        Assert.assertTrue(savedAgent.getCreatedTime() > 0);
        Assert.assertEquals(agent.getTenantId(), savedAgent.getTenantId());
        Assert.assertNotNull(savedAgent.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAgent.getCustomerId().getId());
        Assert.assertEquals(agent.getName(), savedAgent.getName());

        savedAgent.setName("My new agent");

        agentService.saveAgent(savedAgent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertEquals(foundAgent.getName(), savedAgent.getName());

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentAutoAssignsDefaultProfile() {
        AgentProfile defaultProfile = agentProfileService.findDefaultAgentProfile(tenantId);
        Assert.assertNotNull("Tenant should have a default agent profile", defaultProfile);

        Agent agent = newAgent("Agent without explicit profile");
        Assert.assertNull(agent.getAgentProfileId());

        Agent savedAgent = agentService.saveAgent(agent);
        Assert.assertNotNull(savedAgent.getAgentProfileId());
        Assert.assertEquals(defaultProfile.getId(), savedAgent.getAgentProfileId());

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentWithNonExistingProfileFails() {
        Agent agent = newAgent("Agent with bogus profile");
        agent.setAgentProfileId(new AgentProfileId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> agentService.saveAgent(agent));
    }

    @Test
    public void testSaveAgentWithProfileFromDifferentTenantFails() {
        Tenant otherTenant = createTenant();
        try {
            AgentProfile otherTenantDefault = agentProfileService.findDefaultAgentProfile(otherTenant.getId());
            Assert.assertNotNull(otherTenantDefault);

            Agent agent = newAgent("Agent with foreign profile");
            agent.setAgentProfileId(otherTenantDefault.getId());
            Assertions.assertThrows(DataValidationException.class, () -> agentService.saveAgent(agent));
        } finally {
            tenantService.deleteTenant(otherTenant.getId());
        }
    }

    @Test
    public void testDeleteAgentProfileReferencedByAgentsFails() {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(tenantId);
        profile.setName("Profile with agents " + UUID.randomUUID());
        profile.setProvisionType(AgentProvisionType.DISABLED);
        AgentProfile savedProfile = agentProfileService.saveProfile(profile);

        Agent agent = newAgent("Agent referencing profile");
        agent.setAgentProfileId(savedProfile.getId());
        Agent savedAgent = agentService.saveAgent(agent);
        try {
            Assertions.assertThrows(DataValidationException.class,
                    () -> agentProfileService.deleteProfile(tenantId, savedProfile.getId()));
        } finally {
            agentService.deleteAgent(tenantId, savedAgent.getId());
            agentProfileService.deleteProfile(tenantId, savedProfile.getId());
        }
    }

    @Test
    public void testSaveAgentWithDescription() {
        Agent agent = newAgent("Agent with description");
        agent.setDescription("Test description");
        Agent savedAgent = agentService.saveAgent(agent);

        Assert.assertNotNull(savedAgent);
        Assert.assertEquals("Test description", savedAgent.getDescription());

        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertEquals("Test description", foundAgent.getDescription());

        savedAgent.setDescription("Updated description");
        agentService.saveAgent(savedAgent);
        Agent updatedAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertEquals("Updated description", updatedAgent.getDescription());

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentWithNullDescription() {
        Agent agent = newAgent("Agent null description");
        Agent savedAgent = agentService.saveAgent(agent);

        Assert.assertNotNull(savedAgent);
        Assert.assertNull(savedAgent.getDescription());

        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNull(foundAgent.getDescription());

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentWithEmptyTenant() {
        Agent agent = new Agent();
        agent.setName("My agent");
        agent.setRoutingKey(UUID.randomUUID().toString());
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithInvalidTenant() {
        Agent agent = newAgent(TenantId.fromUUID(Uuids.timeBased()), "My agent");
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithEmptyName() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithInvalidName() {
        Agent agent = newAgent(RandomStringUtils.randomAlphabetic(300));
        Assertions.assertThrows(DataValidationException.class, () -> {
            agentService.saveAgent(agent);
        });
    }

    @Test
    public void testSaveAgentWithSameName() {
        Agent savedAgent = agentService.saveAgent(newAgent("My agent"));

        Agent agent2 = newAgent("My agent");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                agentService.saveAgent(agent2);
            });
        } finally {
            agentService.deleteAgent(tenantId, savedAgent.getId());
        }
    }

    @Test
    public void testFindAgentById() {
        Agent agent = newAgent("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgent);
        Assert.assertEquals(savedAgent, foundAgent);
        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testFindAgentInfoById() {
        Agent agent = newAgent("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        AgentInfo foundAgentInfo = agentService.findAgentInfoById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgentInfo);
        Assert.assertEquals(savedAgent.getId(), foundAgentInfo.getId());
        Assert.assertEquals(savedAgent.getName(), foundAgentInfo.getName());
        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testDeleteAgent() {
        Agent agent = newAgent("My agent");
        Agent savedAgent = agentService.saveAgent(agent);
        Agent foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNotNull(foundAgent);
        agentService.deleteAgent(tenantId, savedAgent.getId());
        foundAgent = agentService.findAgentById(tenantId, savedAgent.getId());
        Assert.assertNull(foundAgent);
    }

    @Test
    public void testFindAgentsByTenantId() {
        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            agents.add(agentService.saveAgent(newAgent("Agent" + i)));
        }

        List<Agent> loadedAgents = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Agent> pageData = null;
        do {
            pageData = agentService.findAgentsByTenantId(tenantId, pageLink);
            loadedAgents.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agents, idComparator);
        Collections.sort(loadedAgents, idComparator);

        Assert.assertEquals(agents, loadedAgents);

        agentService.deleteByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = agentService.findAgentsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindAgentsByTenantIdAndName() {
        String title1 = "Agent title 1";
        List<AgentInfo> agentsTitle1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Agent savedAgent = agentService.saveAgent(newAgent(name));
            agentsTitle1.add(asExpectedInfo(savedAgent));
        }
        String title2 = "Agent title 2";
        List<AgentInfo> agentsTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Agent savedAgent = agentService.saveAgent(newAgent(name));
            agentsTitle2.add(asExpectedInfo(savedAgent));
        }

        List<AgentInfo> loadedAgentsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<AgentInfo> pageData = null;
        do {
            pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
            loadedAgentsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentsTitle1, idComparator);
        Collections.sort(loadedAgentsTitle1, idComparator);

        Assert.assertEquals(agentsTitle1, loadedAgentsTitle1);

        List<AgentInfo> loadedAgentsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
            loadedAgentsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentsTitle2, idComparator);
        Collections.sort(loadedAgentsTitle2, idComparator);

        Assert.assertEquals(agentsTitle2, loadedAgentsTitle2);

        for (Agent agent : loadedAgentsTitle1) {
            agentService.deleteAgent(tenantId, agent.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Agent agent : loadedAgentsTitle2) {
            agentService.deleteAgent(tenantId, agent.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = agentService.findAgentInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<Agent> agents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Agent agent = agentService.saveAgent(newAgent(tenantId, "Agent" + i));
            agent.setCustomerId(customerId);
            agents.add(agentService.saveAgent(agent));
        }

        List<Agent> loadedAgents = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Agent> pageData = null;
        do {
            pageData = agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAgents.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agents, idComparator);
        Collections.sort(loadedAgents, idComparator);

        Assert.assertEquals(agents, loadedAgents);

        agentService.deleteAgentsByTenantIdAndCustomerId(tenantId, customerId);

        pageLink = new PageLink(4);
        pageData = agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAgentInfosByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<AgentInfo> agentInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Agent agent = agentService.saveAgent(newAgent(tenantId, "Agent" + i));
            agent.setCustomerId(customerId);
            agent = agentService.saveAgent(agent);
            agentInfos.add(new AgentInfo(agent, customer.getTitle(), false, null));
        }

        List<AgentInfo> loadedAgentInfos = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<AgentInfo> pageData = null;
        do {
            pageData = agentService.findAgentInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAgentInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(agentInfos, idComparator);
        Collections.sort(loadedAgentInfos, idComparator);

        Assert.assertEquals(agentInfos.size(), loadedAgentInfos.size());

        for (int i = 0; i < agentInfos.size(); i++) {
            Assert.assertEquals(agentInfos.get(i).getId(), loadedAgentInfos.get(i).getId());
            Assert.assertEquals(agentInfos.get(i).getName(), loadedAgentInfos.get(i).getName());
            Assert.assertEquals(agentInfos.get(i).getCustomerTitle(), loadedAgentInfos.get(i).getCustomerTitle());
        }

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAgentByRoutingKey() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Agent with routing key");
        agent.setRoutingKey("test-routing-key-" + UUID.randomUUID());
        agent.setSecret("test-secret-123");
        Agent savedAgent = agentService.saveAgent(agent);

        Agent found = agentService.findAgentByRoutingKey(tenantId, savedAgent.getRoutingKey());
        Assert.assertNotNull(found);
        Assert.assertEquals(savedAgent.getId(), found.getId());
        Assert.assertEquals(savedAgent.getRoutingKey(), found.getRoutingKey());
        Assert.assertEquals(savedAgent.getSecret(), found.getSecret());

        Agent notFound = agentService.findAgentByRoutingKey(tenantId, "non-existent-routing-key");
        Assert.assertNull(notFound);

        agentService.deleteAgent(tenantId, savedAgent.getId());
    }

    @Test
    public void testSaveAgentWithEmptyRoutingKey() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Agent no routing key");
        agent.setSecret("some-secret");
        Assertions.assertThrows(DataValidationException.class, () -> agentService.saveAgent(agent));
    }

    @Test
    public void testSaveAgentWithEmptySecret() {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName("Agent no secret");
        agent.setRoutingKey("some-routing-key");
        Assertions.assertThrows(DataValidationException.class, () -> agentService.saveAgent(agent));
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() throws Exception {
        Agent savedAgent = agentService.saveAgent(newAgent("Agent with applications"));
        AgentAppTemplate template = createTemplate();

        AgentApplication app1 = new AgentApplication();
        app1.setTenantId(tenantId);
        app1.setAgentId(savedAgent.getId());
        app1.setAppType(AgentApplicationType.GENERIC);
        app1.setTemplateVersion(template.getCurrentVersion());
        app1.setOrigin(AgentApplicationOrigin.INSTALLED);
        app1.setProjectName(AgentApplication.generateProjectName());
        app1.setConfig(genericComposeConfig());
        app1 = agentApplicationService.save(tenantId, app1);

        AgentApplication app2 = new AgentApplication();
        app2.setTenantId(tenantId);
        app2.setAgentId(savedAgent.getId());
        app2.setAppType(AgentApplicationType.GENERIC);
        app2.setTemplateVersion(template.getCurrentVersion());
        app2.setOrigin(AgentApplicationOrigin.INSTALLED);
        app2.setProjectName(AgentApplication.generateProjectName());
        app2.setConfig(genericComposeConfig());
        app2 = agentApplicationService.save(tenantId, app2);

        List<AgentApplication> applicationsBefore = agentApplicationService.findByAgentId(tenantId, savedAgent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(2, applicationsBefore.size());

        agentService.deleteAgent(tenantId, savedAgent.getId());

        List<AgentApplication> applicationsAfter = agentApplicationService.findByAgentId(tenantId, savedAgent.getId(), new PageLink(100)).getData();
        Assert.assertTrue(applicationsAfter.isEmpty());
        Assert.assertNull(agentApplicationService.findById(tenantId, app1.getId()));
        Assert.assertNull(agentApplicationService.findById(tenantId, app2.getId()));
    }

    private Agent newAgent(String name) {
        return newAgent(tenantId, name);
    }

    private Agent newAgent(TenantId tid, String name) {
        Agent agent = new Agent();
        agent.setTenantId(tid);
        agent.setName(name);
        agent.setRoutingKey(UUID.randomUUID().toString());
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return agent;
    }

    private AgentAppTemplate createTemplate() {
        return registerAppTemplate(createAppTemplate(
                AgentApplicationType.GENERIC, AgentApplicationType.GENERIC.getDefaultVersion()));
    }

    // Mirrors what findAgentInfosByTenantId returns: ownerName populated from
    // the tenant title and groups defaulted to an empty list.
    private AgentInfo asExpectedInfo(Agent agent) {
        AgentInfo info = new AgentInfo(agent, null, false, "default");
        info.setOwnerName(TEST_TENANT_NAME);
        info.setGroups(Collections.emptyList());
        return info;
    }

    private DockerComposeConfig genericComposeConfig() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }

    @Test
    public void testAssignUnassignAgentApplicationsStillRetrieved() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test customer for apps");
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        Agent agent = agentService.saveAgent(newAgent("Agent assign unassign"));
        AgentAppTemplate template = createTemplate();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setTemplateVersion(template.getCurrentVersion());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        app.setConfig(genericComposeConfig());
        app = agentApplicationService.save(tenantId, app);

        List<AgentApplication> afterCreate = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterCreate.size());

        agent.setCustomerId(customerId);
        agent = agentService.saveAgent(agent);
        List<AgentApplication> afterAssign = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterAssign.size());
        Assert.assertEquals(app.getId(), afterAssign.get(0).getId());

        agent.setCustomerId(null);
        agentService.saveAgent(agent);
        List<AgentApplication> afterUnassign = agentApplicationService.findByAgentId(tenantId, agent.getId(), new PageLink(100)).getData();
        Assert.assertEquals(1, afterUnassign.size());
        Assert.assertEquals(app.getId(), afterUnassign.get(0).getId());

        agentApplicationService.delete(tenantId, app.getId());
        agentService.deleteAgent(tenantId, agent.getId());
        customerService.deleteCustomer(tenantId, customerId);
    }
}
