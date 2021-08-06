/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineFlowIntegrationTest extends AbstractRuleEngineControllerTest {

    public static final int PAGE_LIMIT = 1000;
    protected Tenant savedTenant;
    protected User tenantAdmin;

    @Autowired
    protected ActorSystemContext actorSystem;

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
        final RuleChain ruleChainForSave = new RuleChain();
        ruleChainForSave.setName("Simple Rule Chain");
        ruleChainForSave.setTenantId(savedTenant.getId());
        ruleChainForSave.setRoot(true);
        ruleChainForSave.setDebugMode(true);
        final RuleChain ruleChainSaved = saveRuleChain(ruleChainForSave);
        Assert.assertNull(ruleChainSaved.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChainSaved.getId());

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

        final RuleChain ruleChain = getRuleChain(ruleChainSaved.getId());
        Assert.assertNotNull(ruleChain.getFirstRuleNodeId());

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class); //sync

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);

        log.warn("root rule chain {}", ruleChain);
        log.warn("root metadata {}", ruleChain);
        // Pushing Message to the system
        actorSystem.tell(qMsg);
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();

        List<Event> events = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.warn("Fetching events until condition for first rule node {}", ruleChain.getFirstRuleNodeId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), ruleChain.getFirstRuleNodeId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.warn("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the first rule node"));
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the first rule node"));
        Assert.assertEquals(ruleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleNode lastRuleNode = metaData.getNodes().stream().filter(node -> !node.getId().equals(ruleChain.getFirstRuleNodeId())).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present for the last rule node"));

        events = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.warn("Fetching events until condition for the last rule node {}", lastRuleNode.getId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.warn("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the last rule node"));
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the last rule node"));
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());
    }

    @Test
    public void testTwoRuleChainsWithTwoRules() throws Exception {
        // Creating root Rule Chain
        final RuleChain rootRuleChainForSave = new RuleChain();
        rootRuleChainForSave.setName("Root Rule Chain");
        rootRuleChainForSave.setTenantId(savedTenant.getId());
        rootRuleChainForSave.setRoot(true);
        rootRuleChainForSave.setDebugMode(true);
        final RuleChain rootRuleChainSaved = saveRuleChain(rootRuleChainForSave);
        Assert.assertNull(rootRuleChainSaved.getFirstRuleNodeId());

        // Creating secondary Rule Chain
        RuleChain secondaryRuleChain = new RuleChain();
        secondaryRuleChain.setName("Secondary Rule Chain");
        secondaryRuleChain.setTenantId(savedTenant.getId());
        secondaryRuleChain.setRoot(false);
        secondaryRuleChain.setDebugMode(true);
        secondaryRuleChain = saveRuleChain(secondaryRuleChain);
        Assert.assertNull(secondaryRuleChain.getFirstRuleNodeId());

        RuleChainMetaData rootMetaData = new RuleChainMetaData();
        rootMetaData.setRuleChainId(rootRuleChainSaved.getId());

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

        final RuleChain rootRuleChain = getRuleChain(rootRuleChainSaved.getId());
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
        device = doPost("/api/device", device, Device.class); //sync API

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey1", "serverAttributeValue1"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey2", "serverAttributeValue2"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);


        log.warn("root rule chain {}", rootRuleChain);
        log.warn("root metadata {}", rootMetaData);
        log.warn("second rule chain {}", secondaryRuleChain);
        log.warn("second metadata {}", secondaryMetaData);
        // Pushing Message to the system
        actorSystem.tell(qMsg);

        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();

        List<Event> events = await("Fetching events for the first rule node").atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.warn("Fetching events for first rule node {}", rootRuleChain.getFirstRuleNodeId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), rootRuleChain.getFirstRuleNodeId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.warn("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the first rule node"));
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the first rule node"));
        Assert.assertEquals(rootRuleChain.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());

        RuleNode lastRuleNode = secondaryMetaData.getNodes().stream().filter(node -> !node.getId().equals(rootRuleChain.getFirstRuleNodeId())).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present for the last rule node"));

        events = await("Fetching events for the last rule node").atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.warn("Fetching events for the last rule node {}", lastRuleNode.getId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.warn("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the last rule node"));
        Assert.assertEquals(lastRuleNode.getId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the last rule node"));
        Assert.assertEquals(lastRuleNode.getId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        Assert.assertEquals("serverAttributeValue1", getMetadata(outEvent).get("ss_serverAttributeKey1").asText());
        Assert.assertEquals("serverAttributeValue2", getMetadata(outEvent).get("ss_serverAttributeKey2").asText());
    }

}
