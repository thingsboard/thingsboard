/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.rules.flow;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineFlowIntegrationTest extends AbstractRuleEngineControllerTest {

    protected Tenant savedTenant;
    protected User tenantAdmin;

    @Autowired
    protected ActorService actorService;

    @Autowired
    protected AttributesService attributesService;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        ruleChainService.deleteRuleChainsByTenantId(savedTenant.getId());

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    public void testRuleChainWithTwoRules() throws Exception {
        // Creating Rule Chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Simple Rule Chain");
        ruleChain.setTenantId(savedTenant.getId());
        ruleChain.setRoot(true);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        Assert.assertNull(ruleChain.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(mapper.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(mapper.valueToTree(configuration2));


        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, "Success");
        metaData = saveRuleChainMetaData(metaData);
        Assert.assertNotNull(metaData);

        ruleChain = getRuleChain(ruleChain.getId());
        Assert.assertNotNull(ruleChain.getFirstRuleNodeId());

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        attributesService.save(device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis())));
        attributesService.save(device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis())));


        Thread.sleep(1000);

        // Pushing Message to the system
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(),
                "CUSTOM",
                device.getId(),
                new TbMsgMetaData(),
                "{}", null, null, 0L);
        actorService.onMsg(new ServiceToRuleEngineMsg(savedTenant.getId(), tbMsg));

        Thread.sleep(3000);

        TimePageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), ruleChain.getFirstRuleNodeId(), 1000);
        List<Event> events = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
        Assert.assertEquals(2, events.size());

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleChain finalRuleChain = ruleChain;
        RuleNode lastRuleNode = metaData.getNodes().stream().filter(node -> !node.getId().equals(finalRuleChain.getFirstRuleNodeId())).findFirst().get();

        eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), 1000);
        events = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());

        Assert.assertEquals(2, events.size());

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());

        List<TbMsg> unAckMsgList = Lists.newArrayList(msgQueueService.findUnprocessed(savedTenant.getId(), ruleChain.getId().getId(), 0L));
        Assert.assertEquals(0, unAckMsgList.size());
    }

    @Test
    public void testTwoRuleChainsWithTwoRules() throws Exception {
        // Creating Rule Chain
        RuleChain rootRuleChain = new RuleChain();
        rootRuleChain.setName("Root Rule Chain");
        rootRuleChain.setTenantId(savedTenant.getId());
        rootRuleChain.setRoot(true);
        rootRuleChain.setDebugMode(true);
        rootRuleChain = saveRuleChain(rootRuleChain);
        Assert.assertNull(rootRuleChain.getFirstRuleNodeId());

        // Creating Rule Chain
        RuleChain secondaryRuleChain = new RuleChain();
        secondaryRuleChain.setName("Secondary Rule Chain");
        secondaryRuleChain.setTenantId(savedTenant.getId());
        secondaryRuleChain.setRoot(false);
        secondaryRuleChain.setDebugMode(true);
        secondaryRuleChain = saveRuleChain(secondaryRuleChain);
        Assert.assertNull(secondaryRuleChain.getFirstRuleNodeId());

        RuleChainMetaData rootMetaData = new RuleChainMetaData();
        rootMetaData.setRuleChainId(rootRuleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(mapper.valueToTree(configuration1));

        rootMetaData.setNodes(Collections.singletonList(ruleNode1));
        rootMetaData.setFirstNodeIndex(0);
        rootMetaData.addRuleChainConnectionInfo(0, secondaryRuleChain.getId(), "Success", mapper.createObjectNode());
        rootMetaData = saveRuleChainMetaData(rootMetaData);
        Assert.assertNotNull(rootMetaData);

        rootRuleChain = getRuleChain(rootRuleChain.getId());
        Assert.assertNotNull(rootRuleChain.getFirstRuleNodeId());


        RuleChainMetaData secondaryMetaData = new RuleChainMetaData();
        secondaryMetaData.setRuleChainId(secondaryRuleChain.getId());

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(mapper.valueToTree(configuration2));

        secondaryMetaData.setNodes(Collections.singletonList(ruleNode2));
        secondaryMetaData.setFirstNodeIndex(0);
        secondaryMetaData = saveRuleChainMetaData(secondaryMetaData);
        Assert.assertNotNull(secondaryMetaData);

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        attributesService.save(device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis())));
        attributesService.save(device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis())));


        Thread.sleep(1000);

        // Pushing Message to the system
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(),
                "CUSTOM",
                device.getId(),
                new TbMsgMetaData(),
                "{}", null, null, 0L);
        actorService.onMsg(new ServiceToRuleEngineMsg(savedTenant.getId(), tbMsg));

        Thread.sleep(3000);

        TimePageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), rootRuleChain.getFirstRuleNodeId(), 1000);
        List<Event> events = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());

        Assert.assertEquals(2, events.size());

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleChain finalRuleChain = rootRuleChain;
        RuleNode lastRuleNode = secondaryMetaData.getNodes().stream().filter(node -> !node.getId().equals(finalRuleChain.getFirstRuleNodeId())).findFirst().get();

        eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), 1000);
        events = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());


        Assert.assertEquals(2, events.size());

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());

        List<TbMsg> unAckMsgList = Lists.newArrayList(msgQueueService.findUnprocessed(savedTenant.getId(), rootRuleChain.getId().getId(), 0L));
        Assert.assertEquals(0, unAckMsgList.size());

        unAckMsgList = Lists.newArrayList(msgQueueService.findUnprocessed(savedTenant.getId(), secondaryRuleChain.getId().getId(), 0L));
        Assert.assertEquals(0, unAckMsgList.size());
    }

}
