/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
public abstract class BaseRuleChainServiceTest extends AbstractServiceTest {

    private IdComparator<RuleChain> idComparator = new IdComparator<>();
    private IdComparator<RuleNode> ruleNodeIdComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveRuleChain() throws IOException {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");

        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        Assert.assertNotNull(savedRuleChain);
        Assert.assertNotNull(savedRuleChain.getId());
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getTenantId(), savedRuleChain.getTenantId());
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());

        savedRuleChain.setName("My new RuleChain");

        ruleChainService.saveRuleChain(savedRuleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertEquals(foundRuleChain.getName(), savedRuleChain.getName());

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRuleChainWithEmptyName() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChainService.saveRuleChain(ruleChain);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRuleChainWithInvalidTenant() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(new TenantId(UUIDs.timeBased()));
        ruleChainService.saveRuleChain(ruleChain);
    }

    @Test
    public void testFindRuleChainById() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNotNull(foundRuleChain);
        Assert.assertEquals(savedRuleChain, foundRuleChain);
        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
    }

    @Test
    public void testDeleteRuleChain() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNotNull(foundRuleChain);
        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
        foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNull(foundRuleChain);
    }

    @Test
    public void testFindRuleChainsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<RuleChain> ruleChains = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            ruleChain.setName("RuleChain" + i);
            ruleChains.add(ruleChainService.saveRuleChain(ruleChain));
        }

        List<RuleChain> loadedRuleChains = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(16);
        TextPageData<RuleChain> pageData = null;
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChains.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChains, idComparator);
        Collections.sort(loadedRuleChains, idComparator);

        Assert.assertEquals(ruleChains, loadedRuleChains);

        ruleChainService.deleteRuleChainsByTenantId(tenantId);

        pageLink = new TextPageLink(31);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindRuleChainsByTenantIdAndName() {
        String name1 = "RuleChain name 1";
        List<RuleChain> ruleChainsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (Math.random() * 17));
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChainsName1.add(ruleChainService.saveRuleChain(ruleChain));
        }
        String name2 = "RuleChain name 2";
        List<RuleChain> ruleChainsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChainsName2.add(ruleChainService.saveRuleChain(ruleChain));
        }

        List<RuleChain> loadedRuleChainsName1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(19, name1);
        TextPageData<RuleChain> pageData = null;
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChainsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName1, idComparator);
        Collections.sort(loadedRuleChainsName1, idComparator);

        Assert.assertEquals(ruleChainsName1, loadedRuleChainsName1);

        List<RuleChain> loadedRuleChainsName2 = new ArrayList<>();
        pageLink = new TextPageLink(4, name2);
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChainsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName2, idComparator);
        Collections.sort(loadedRuleChainsName2, idComparator);

        Assert.assertEquals(ruleChainsName2, loadedRuleChainsName2);

        for (RuleChain ruleChain : loadedRuleChainsName1) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new TextPageLink(4, name1);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (RuleChain ruleChain : loadedRuleChainsName2) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new TextPageLink(4, name2);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testSaveRuleChainMetaData() throws Exception {

        RuleChainMetaData savedRuleChainMetaData = createRuleChainMetadata();

        Assert.assertEquals(3, savedRuleChainMetaData.getNodes().size());
        Assert.assertEquals(3, savedRuleChainMetaData.getConnections().size());

        for (RuleNode ruleNode : savedRuleChainMetaData.getNodes()) {
            Assert.assertNotNull(ruleNode.getId());
            List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNode.getId());
            if ("name1".equals(ruleNode.getName())) {
                Assert.assertEquals(2, relations.size());
            } else if ("name2".equals(ruleNode.getName())) {
                Assert.assertEquals(1, relations.size());
            } else if ("name3".equals(ruleNode.getName())) {
                Assert.assertEquals(0, relations.size());
            }
        }

        List<RuleNode> loadedRuleNodes = ruleChainService.getRuleChainNodes(tenantId, savedRuleChainMetaData.getRuleChainId());

        Collections.sort(savedRuleChainMetaData.getNodes(), ruleNodeIdComparator);
        Collections.sort(loadedRuleNodes, ruleNodeIdComparator);

        Assert.assertEquals(savedRuleChainMetaData.getNodes(), loadedRuleNodes);

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChainMetaData.getRuleChainId());
    }

    @Test
    public void testUpdateRuleChainMetaData() throws Exception {
        RuleChainMetaData savedRuleChainMetaData = createRuleChainMetadata();

        List<RuleNode> ruleNodes = savedRuleChainMetaData.getNodes();
        int name3Index = -1;
        for (int i=0;i<ruleNodes.size();i++) {
            if ("name3".equals(ruleNodes.get(i).getName())) {
                name3Index = i;
                break;
            }
        }

        RuleNode ruleNode4 = new RuleNode();
        ruleNode4.setName("name4");
        ruleNode4.setType("type4");
        ruleNode4.setConfiguration(mapper.readTree("\"key4\": \"val4\""));

        ruleNodes.set(name3Index, ruleNode4);

        RuleChainMetaData updatedRuleChainMetaData = ruleChainService.saveRuleChainMetaData(tenantId, savedRuleChainMetaData);

        Assert.assertEquals(3, updatedRuleChainMetaData.getNodes().size());
        Assert.assertEquals(3, updatedRuleChainMetaData.getConnections().size());

        for (RuleNode ruleNode : updatedRuleChainMetaData.getNodes()) {
            Assert.assertNotNull(ruleNode.getId());
            List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNode.getId());
            if ("name1".equals(ruleNode.getName())) {
                Assert.assertEquals(2, relations.size());
            } else if ("name2".equals(ruleNode.getName())) {
                Assert.assertEquals(1, relations.size());
            } else if ("name4".equals(ruleNode.getName())) {
                Assert.assertEquals(0, relations.size());
            }
        }

        List<RuleNode> loadedRuleNodes = ruleChainService.getRuleChainNodes(tenantId, savedRuleChainMetaData.getRuleChainId());

        Collections.sort(updatedRuleChainMetaData.getNodes(), ruleNodeIdComparator);
        Collections.sort(loadedRuleNodes, ruleNodeIdComparator);

        Assert.assertEquals(updatedRuleChainMetaData.getNodes(), loadedRuleNodes);

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChainMetaData.getRuleChainId());
    }

    @Test
    public void testGetDefaultEdgeRuleChains() throws Exception {
        RuleChainId ruleChainId = saveRuleChainAndSetDefaultEdge("Default Edge Rule Chain 1");
        saveRuleChainAndSetDefaultEdge("Default Edge Rule Chain 2");
        List<RuleChain> result = ruleChainService.findDefaultEdgeRuleChainsByTenantId(tenantId).get();
        Assert.assertEquals(2, result.size());

        ruleChainService.removeDefaultEdgeRuleChain(tenantId, ruleChainId);

        result = ruleChainService.findDefaultEdgeRuleChainsByTenantId(tenantId).get();
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void setDefaultRootEdgeRuleChain() throws Exception {
        RuleChainId ruleChainId1 = saveRuleChainAndSetDefaultEdge("Default Edge Rule Chain 1");
        RuleChainId ruleChainId2 = saveRuleChainAndSetDefaultEdge("Default Edge Rule Chain 2");

        ruleChainService.setDefaultRootEdgeRuleChain(tenantId, ruleChainId1);
        ruleChainService.setDefaultRootEdgeRuleChain(tenantId, ruleChainId2);

        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, ruleChainId1);
        Assert.assertFalse(ruleChainById.isRoot());

        ruleChainById = ruleChainService.findRuleChainById(tenantId, ruleChainId2);
        Assert.assertTrue(ruleChainById.isRoot());
    }

    private RuleChainId saveRuleChainAndSetDefaultEdge(String name) {
        RuleChain edgeRuleChain = new RuleChain();
        edgeRuleChain.setTenantId(tenantId);
        edgeRuleChain.setType(RuleChainType.EDGE);
        edgeRuleChain.setName(name);
        RuleChain savedEdgeRuleChain = ruleChainService.saveRuleChain(edgeRuleChain);
        ruleChainService.addDefaultEdgeRuleChain(tenantId, savedEdgeRuleChain.getId());
        return savedEdgeRuleChain.getId();
    }

    private RuleChainMetaData createRuleChainMetadata() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());

        ObjectMapper mapper = new ObjectMapper();

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(mapper.readTree("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(mapper.readTree("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(mapper.readTree("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0,1,"success");
        ruleChainMetaData.addConnectionInfo(0,2,"fail");
        ruleChainMetaData.addConnectionInfo(1,2,"success");

        return ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData);
    }


}
