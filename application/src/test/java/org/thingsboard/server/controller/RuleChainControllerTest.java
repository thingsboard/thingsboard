/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbCreateAlarmNode;
import org.thingsboard.rule.engine.action.TbCreateAlarmNodeConfiguration;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode;
import org.thingsboard.rule.engine.metadata.TbGetRelatedDataNodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {RuleChainControllerTest.Config.class})
@DaoSqlTest
public class RuleChainControllerTest extends AbstractControllerTest {

    private final IdComparator<RuleChain> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private RuleChainDao ruleChainDao;

    static class Config {
        @Bean
        @Primary
        public RuleChainDao ruleChainDao(RuleChainDao ruleChainDao) {
            return Mockito.mock(RuleChainDao.class, AdditionalAnswers.delegatesTo(ruleChainDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");

        Mockito.reset(tbClusterService, auditLogService);

        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        Assert.assertNotNull(savedRuleChain);
        Assert.assertNotNull(savedRuleChain.getId());
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedRuleChain.setName("New RuleChain");
        savedRuleChain = doPost("/api/ruleChain", savedRuleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertEquals(savedRuleChain.getName(), foundRuleChain.getName());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveRuleChainMetadataWithVersionedNodes() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");

        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        Assert.assertNotNull(savedRuleChain);
        RuleChainId ruleChainId = savedRuleChain.getId();
        Assert.assertNotNull(ruleChainId);
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());

        var annotation = TbGetRelatedAttributeNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class);
        String ruleNodeType = TbGetRelatedAttributeNode.class.getName();
        int currentVersion = annotation.version();

        String oldConfig = "{\"attrMapping\":{\"serialNumber\":\"sn\"}," +
                "\"relationsQuery\":{\"direction\":\"FROM\",\"maxLevel\":1," +
                "\"filters\":[{\"relationType\":\"Contains\",\"entityTypes\":[]}]," +
                "\"fetchLastLevelOnly\":false},\"telemetry\":false}";

        TbGetRelatedDataNodeConfiguration defaultConfiguration = new TbGetRelatedDataNodeConfiguration().defaultConfiguration();
        String newConfig = JacksonUtil.toString(defaultConfiguration);

        var ruleChainMetaData = createRuleChainMetadataWithTbVersionedNodes(
                ruleChainId,
                ruleNodeType,
                currentVersion,
                oldConfig,
                newConfig
        );
        var savedRuleChainMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);

        Assert.assertEquals(ruleChainId, savedRuleChainMetaData.getRuleChainId());
        Assert.assertEquals(2, savedRuleChainMetaData.getNodes().size());

        for (RuleNode ruleNode : savedRuleChainMetaData.getNodes()) {
            Assert.assertNotNull(ruleNode.getId());
            Assert.assertEquals(currentVersion, ruleNode.getConfigurationVersion());
            Assert.assertEquals(defaultConfiguration, JacksonUtil.treeToValue(ruleNode.getConfiguration(), defaultConfiguration.getClass()));
        }
    }

    private RuleChainMetaData createRuleChainMetadataWithTbVersionedNodes(
            RuleChainId ruleChainId,
            String ruleNodeType,
            int currentVersion,
            String oldConfig,
            String newConfig
    ) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);

        var ruleNodeWithOldConfig = new RuleNode();
        ruleNodeWithOldConfig.setName("Old Rule Node");
        ruleNodeWithOldConfig.setType(ruleNodeType);
        ruleNodeWithOldConfig.setConfiguration(JacksonUtil.toJsonNode(oldConfig));

        var ruleNodeWithNewConfig = new RuleNode();
        ruleNodeWithNewConfig.setName("New Rule Node");
        ruleNodeWithNewConfig.setType(ruleNodeType);
        ruleNodeWithNewConfig.setConfigurationVersion(currentVersion);
        ruleNodeWithNewConfig.setConfiguration(JacksonUtil.toJsonNode(newConfig));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNodeWithOldConfig);
        ruleNodes.add(ruleNodeWithNewConfig);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);
        return ruleChainMetaData;
    }

    @Test
    public void testSaveRuleChainWithViolationOfLengthValidation() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(StringUtils.randomAlphabetic(300));
        String msgError = msgErrorFieldLength("name");
        doPost("/api/ruleChain", ruleChain)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        ruleChain.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(ruleChain,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindRuleChainById() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertNotNull(foundRuleChain);
        Assert.assertEquals(savedRuleChain, foundRuleChain);
    }

    @Test
    public void testDeleteRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        Mockito.reset(tbClusterService, auditLogService);

        String entityIdStr = savedRuleChain.getId().getId().toString();
        doDelete("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedRuleChain.getId().getId().toString());

        doGet("/api/ruleChain/" + entityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Rule chain", entityIdStr))));
    }

    @Test
    public void testFindEdgeRuleChainsByTenantIdAndName() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);


        PageLink pageLink = new PageLink(17);
        PageData<RuleChain> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        List<RuleChain> edgeRuleChains = new ArrayList<>(pageData.getData());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 28;
        for (int i = 0; i < cntEntity; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName("RuleChain " + i);
            ruleChain.setType(RuleChainType.EDGE);
            RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
            edgeRuleChains.add(savedRuleChain);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new RuleChain(), new RuleChain(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity * 2);
        Mockito.reset(tbClusterService, auditLogService);

        List<RuleChain> loadedEdgeRuleChains = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgeRuleChains.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgeRuleChains.sort(idComparator);
        loadedEdgeRuleChains.sort(idComparator);

        Assert.assertEquals(edgeRuleChains, loadedEdgeRuleChains);

        for (RuleChain ruleChain : loadedEdgeRuleChains) {
            if (!ruleChain.isRoot()) {
                doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                        + "/ruleChain/" + ruleChain.getId().getId().toString(), RuleChain.class);
            }
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new RuleChain(), new RuleChain(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_EDGE, ActionType.UNASSIGNED_FROM_EDGE, cntEntity, cntEntity, 3);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testDeleteRuleChainWithDeleteRelationsOk() throws Exception {
        RuleChainId ruleChainId = createRuleChain("RuleChain for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), ruleChainId, "/api/ruleChain/" + ruleChainId);
    }

    @Ignore
    @Test
    public void testDeleteRuleChainExceptionWithRelationsTransactional() throws Exception {
        RuleChainId ruleChainId = createRuleChain("RuleChain for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(ruleChainDao, savedTenant.getId(), ruleChainId, "/api/ruleChain/" + ruleChainId);
    }

    @Test
    public void givenRuleNodeWithInvalidConfiguration_thenReturnError() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain with invalid nodes");
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode createAlarmNode = new RuleNode();
        createAlarmNode.setName("Create alarm");
        createAlarmNode.setType(TbCreateAlarmNode.class.getName());
        TbCreateAlarmNodeConfiguration invalidCreateAlarmNodeConfiguration = new TbCreateAlarmNodeConfiguration();
        invalidCreateAlarmNodeConfiguration.setSeverity("<script/>");
        invalidCreateAlarmNodeConfiguration.setAlarmType("<script/>");
        createAlarmNode.setConfiguration(JacksonUtil.valueToTree(invalidCreateAlarmNodeConfiguration));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(createAlarmNode);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        String error = getErrorMessage(doPost("/api/ruleChain/metadata", ruleChainMetaData)
                .andExpect(status().isBadRequest()));
        assertThat(error).contains("severity is malformed");
        assertThat(error).contains("alarmType is malformed");
    }

    @Test
    public void testSaveRuleChainWithOutdatedVersion() throws Exception {
        RuleChain ruleChain = createRuleChain("My rule chain");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Test");
        ruleNode.setType(TbMsgGeneratorNode.class.getName());
        TbMsgGeneratorNodeConfiguration config = new TbMsgGeneratorNodeConfiguration();
        ruleNode.setConfiguration(JacksonUtil.valueToTree(config));
        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        assertThat(ruleChainMetaData.getVersion()).isEqualTo(2);

        ruleChain = doGet("/api/ruleChain/" + ruleChain.getId(), RuleChain.class);
        assertThat(ruleChain.getVersion()).isEqualTo(2);

        ruleChain.setName("Updated");
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        assertThat(ruleChain.getVersion()).isEqualTo(3);

        ruleChain.setVersion(1L);
        doPost("/api/ruleChain", ruleChain)
                .andExpect(status().isConflict());
        ruleChainMetaData.setVersion(1L);
        doPost("/api/ruleChain/metadata", ruleChainMetaData)
                .andExpect(status().isConflict());

        ruleChainMetaData.setVersion(3L);
        ruleChainMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        assertThat(ruleChainMetaData.getVersion()).isEqualTo(4);
        ruleChain.setVersion(4L);
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        assertThat(ruleChain.getVersion()).isEqualTo(5);
    }

    private RuleChain createRuleChain(String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

}
