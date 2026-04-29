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
import org.thingsboard.server.common.data.rule.RuleChainNote;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public void testFindRuleChainsByIds() throws Exception {
        List<RuleChain> ruleChains = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName("RuleChain " + i);
            ruleChains.add(doPost("/api/ruleChain", ruleChain, RuleChain.class));
        }

        List<RuleChain> expected = ruleChains.subList(5, 15);

        String idsParam = expected.stream()
                .map(rc -> rc.getId().getId().toString())
                .collect(Collectors.joining(","));

        RuleChain[] result = doGet(
                "/api/ruleChains?ruleChainIds=" + idsParam,
                RuleChain[].class
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.length);

        Map<UUID, RuleChain> rcById = Arrays.stream(result)
                .collect(Collectors.toMap(rc -> rc.getId().getId(), Function.identity()));

        for (RuleChain rc : expected) {
            UUID id = rc.getId().getId();
            RuleChain found = rcById.get(id);
            Assert.assertNotNull("RuleChain not found for id " + id, found);

            Assert.assertEquals(rc.getId(), found.getId());
            Assert.assertEquals(rc.getName(), found.getName());
            Assert.assertEquals(rc.getTenantId(), found.getTenantId());
        }
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

    @Test
    public void testSaveAndLoadRuleChainMetaDataWithNotes() throws Exception {
        RuleChain ruleChain = createRuleChain("RuleChain with notes");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setNodes(new ArrayList<>());

        List<RuleChainNote> notes = new ArrayList<>();
        RuleChainNote note1 = new RuleChainNote();
        note1.setId("note-1");
        note1.setX(100);
        note1.setY(200);
        note1.setWidth(300);
        note1.setHeight(150);
        note1.setContent("# Test Note\nSome markdown content");
        note1.setBackgroundColor("#FFF9C4");
        note1.setBorderColor("#E6C800");
        note1.setBorderWidth(2);
        note1.setApplyDefaultMarkdownStyle(true);
        notes.add(note1);

        RuleChainNote note2 = new RuleChainNote();
        note2.setId("note-2");
        note2.setX(500);
        note2.setY(300);
        note2.setWidth(200);
        note2.setHeight(100);
        note2.setContent("Simple note");
        note2.setBackgroundColor("#C8E6C9");
        notes.add(note2);

        ruleChainMetaData.setNotes(notes);

        RuleChainMetaData savedMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        Assert.assertNotNull(savedMetaData);
        Assert.assertNotNull(savedMetaData.getNotes());
        Assert.assertEquals(2, savedMetaData.getNotes().size());

        RuleChainMetaData loadedMetaData = doGet("/api/ruleChain/" + ruleChain.getId().getId() + "/metadata", RuleChainMetaData.class);
        Assert.assertNotNull(loadedMetaData);
        Assert.assertNotNull(loadedMetaData.getNotes());
        Assert.assertEquals(2, loadedMetaData.getNotes().size());

        RuleChainNote loadedNote1 = loadedMetaData.getNotes().stream()
                .filter(n -> "note-1".equals(n.getId())).findFirst().orElse(null);
        Assert.assertNotNull(loadedNote1);
        Assert.assertEquals(100, loadedNote1.getX());
        Assert.assertEquals(200, loadedNote1.getY());
        Assert.assertEquals(300, loadedNote1.getWidth());
        Assert.assertEquals(150, loadedNote1.getHeight());
        Assert.assertEquals("# Test Note\nSome markdown content", loadedNote1.getContent());
        Assert.assertEquals("#FFF9C4", loadedNote1.getBackgroundColor());
        Assert.assertEquals("#E6C800", loadedNote1.getBorderColor());
        Assert.assertEquals(Integer.valueOf(2), loadedNote1.getBorderWidth());
        Assert.assertEquals(Boolean.TRUE, loadedNote1.getApplyDefaultMarkdownStyle());

        RuleChainNote loadedNote2 = loadedMetaData.getNotes().stream()
                .filter(n -> "note-2".equals(n.getId())).findFirst().orElse(null);
        Assert.assertNotNull(loadedNote2);
        Assert.assertEquals(500, loadedNote2.getX());
        Assert.assertEquals(300, loadedNote2.getY());
        Assert.assertEquals("Simple note", loadedNote2.getContent());
        Assert.assertEquals("#C8E6C9", loadedNote2.getBackgroundColor());
    }

    @Test
    public void testUpdateRuleChainNotes() throws Exception {
        RuleChain ruleChain = createRuleChain("RuleChain update notes");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setNodes(new ArrayList<>());

        RuleChainNote note = new RuleChainNote();
        note.setId("note-1");
        note.setX(10);
        note.setY(20);
        note.setWidth(100);
        note.setHeight(50);
        note.setContent("Original content");
        ruleChainMetaData.setNotes(List.of(note));

        RuleChainMetaData savedMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);

        note.setContent("Updated content");
        note.setX(50);
        RuleChainNote newNote = new RuleChainNote();
        newNote.setId("note-2");
        newNote.setX(200);
        newNote.setY(300);
        newNote.setWidth(150);
        newNote.setHeight(75);
        newNote.setContent("New note");
        savedMetaData.setNotes(List.of(note, newNote));

        RuleChainMetaData updatedMetaData = doPost("/api/ruleChain/metadata", savedMetaData, RuleChainMetaData.class);
        Assert.assertEquals(2, updatedMetaData.getNotes().size());

        RuleChainMetaData loadedMetaData = doGet("/api/ruleChain/" + ruleChain.getId().getId() + "/metadata", RuleChainMetaData.class);
        Assert.assertEquals(2, loadedMetaData.getNotes().size());

        RuleChainNote updatedNote = loadedMetaData.getNotes().stream()
                .filter(n -> "note-1".equals(n.getId())).findFirst().orElse(null);
        Assert.assertNotNull(updatedNote);
        Assert.assertEquals("Updated content", updatedNote.getContent());
        Assert.assertEquals(50, updatedNote.getX());
    }

    @Test
    public void testSaveRuleChainDoesNotOverwriteNotes() throws Exception {
        RuleChain ruleChain = createRuleChain("RuleChain preserve notes");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setNodes(new ArrayList<>());

        RuleChainNote note = new RuleChainNote();
        note.setId("note-1");
        note.setX(10);
        note.setY(20);
        note.setWidth(100);
        note.setHeight(50);
        note.setContent("Persistent note");
        ruleChainMetaData.setNotes(List.of(note));

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);

        // Save the RuleChain itself (not metadata) — e.g. rename
        ruleChain = doGet("/api/ruleChain/" + ruleChain.getId().getId(), RuleChain.class);
        ruleChain.setName("Renamed RuleChain");
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        Assert.assertEquals("Renamed RuleChain", ruleChain.getName());

        // Notes must still be present
        RuleChainMetaData loadedMetaData = doGet("/api/ruleChain/" + ruleChain.getId().getId() + "/metadata", RuleChainMetaData.class);
        Assert.assertNotNull(loadedMetaData.getNotes());
        Assert.assertEquals(1, loadedMetaData.getNotes().size());
        Assert.assertEquals("Persistent note", loadedMetaData.getNotes().get(0).getContent());
    }

    @Test
    public void testRemoveNotesByUpdatingMetadata() throws Exception {
        RuleChain ruleChain = createRuleChain("RuleChain remove notes");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setNodes(new ArrayList<>());

        RuleChainNote note = new RuleChainNote();
        note.setId("note-1");
        note.setX(10);
        note.setY(20);
        note.setWidth(100);
        note.setHeight(50);
        note.setContent("Will be removed");
        ruleChainMetaData.setNotes(List.of(note));

        RuleChainMetaData savedMetaData = doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
        Assert.assertEquals(1, savedMetaData.getNotes().size());

        // Save metadata without notes — should clear them
        savedMetaData.setNotes(null);
        RuleChainMetaData updatedMetaData = doPost("/api/ruleChain/metadata", savedMetaData, RuleChainMetaData.class);

        RuleChainMetaData loadedMetaData = doGet("/api/ruleChain/" + ruleChain.getId().getId() + "/metadata", RuleChainMetaData.class);
        Assert.assertTrue(loadedMetaData.getNotes() == null || loadedMetaData.getNotes().isEmpty());
    }

    @Test
    public void testSaveRuleChainNotesExceedsSizeLimit() throws Exception {
        RuleChain ruleChain = createRuleChain("RuleChain oversized notes");

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setNodes(new ArrayList<>());

        List<RuleChainNote> notes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            RuleChainNote note = new RuleChainNote();
            note.setId("note-" + i);
            note.setX(i * 10);
            note.setY(i * 10);
            note.setWidth(300);
            note.setHeight(150);
            note.setContent(StringUtils.randomAlphabetic(60000));
            notes.add(note);
        }
        ruleChainMetaData.setNotes(notes);

        String error = getErrorMessage(doPost("/api/ruleChain/metadata", ruleChainMetaData)
                .andExpect(status().isBadRequest()));
        assertThat(error).contains("Rule chain notes data is too large");
    }

    private RuleChain createRuleChain(String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

}
