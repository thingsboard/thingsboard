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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineFlowIntegrationTest extends AbstractRuleEngineControllerTest {

    public static final int PAGE_LIMIT = 1000;
    public static final String SERVER_ATTRIBUTE_KEY_ONE = "serverAttributeKey1";
    public static final String SERVER_ATTRIBUTE_KEY_TWO = "serverAttributeKey2";
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
        final RuleChain ruleChainForSave = RuleChain.builder()
                .name("Simple Rule Chain")
                .tenantId(savedTenant.getId())
                .root(true)
                .debugMode(true)
                .build();
        final RuleChain ruleChainSaved = saveRuleChain(ruleChainForSave);
        assertThat(ruleChainSaved.getFirstRuleNodeId()).isNull();

        final RuleNode ruleNodeOne = RuleNode.builder()
                .name("Simple Rule Node 1")
                .type(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName())
                .debugMode(true)
                .configuration(mapper.valueToTree(TbGetAttributesNodeConfiguration.builder()
                        .serverAttributeNames(List.of(SERVER_ATTRIBUTE_KEY_ONE)).build()))
                .build();

        final RuleNode ruleNodeTwo = RuleNode.builder()
                .name("Simple Rule Node 2")
                .type(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName())
                .debugMode(true)
                .configuration(mapper.valueToTree(TbGetAttributesNodeConfiguration.builder()
                        .serverAttributeNames(List.of(SERVER_ATTRIBUTE_KEY_TWO)).build()))
                .build();

        final RuleChainMetaData metaData = saveRuleChainMetaData(RuleChainMetaData.builder()
                .ruleChainId(ruleChainSaved.getId())
                .nodes(Arrays.asList(ruleNodeOne, ruleNodeTwo))
                .firstNodeIndex(0)
                .build()
                .addConnectionInfo(0, 1, "Success"));
        assertThat(metaData).isNotNull();

        final RuleChain ruleChain = getRuleChain(ruleChainSaved.getId());
        assertThat(ruleChain).isNotNull();
        assertThat(ruleChain.getFirstRuleNodeId()).isNotNull();

        // Saving the device
        final Device device = doPost("/api/device", Device.builder().name("My device").type("default").build(), Device.class); //sync

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        List.of(new BaseAttributeKvEntry(new StringDataEntry(SERVER_ATTRIBUTE_KEY_ONE, "serverAttributeValue1"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        List.of(new BaseAttributeKvEntry(new StringDataEntry(SERVER_ATTRIBUTE_KEY_TWO, "serverAttributeValue2"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);

        // Pushing Message to the system
        actorSystem.tell(qMsg);
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();

        List<Event> events = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.info("Fetching events until condition for first rule node {}", ruleChain.getFirstRuleNodeId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), ruleChain.getFirstRuleNodeId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.info("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the first rule node"));
        assertThat(inEvent.getEntityId()).isEqualTo(ruleChain.getFirstRuleNodeId());
        assertThat(inEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the first rule node"));
        assertThat(ruleChain.getFirstRuleNodeId()).isEqualTo(outEvent.getEntityId());
        assertThat(outEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey1").asText()).isEqualTo("serverAttributeValue1");

        RuleNode lastRuleNode = metaData.getNodes().stream().filter(node -> !node.getId().equals(ruleChain.getFirstRuleNodeId())).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present for the last rule node"));

        events = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.info("Fetching events until condition for the last rule node {}", lastRuleNode.getId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.info("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the last rule node"));
        assertThat(inEvent.getEntityId()).isEqualTo(lastRuleNode.getId());
        assertThat(inEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the last rule node"));
        assertThat(outEvent.getEntityId()).isEqualTo(lastRuleNode.getId());
        assertThat(outEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey1").asText()).isEqualTo("serverAttributeValue1");
        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey2").asText()).isEqualTo("serverAttributeValue2");
    }

    @Test
    public void testTwoRuleChainsWithTwoRules() throws Exception {
        // Creating root Rule Chain
        final RuleChain rootRuleChainSaved = saveRuleChain(RuleChain.builder()
                .name("Root Rule Chain")
                .tenantId(savedTenant.getId())
                .root(true)
                .debugMode(true)
                .build());

        assertThat(rootRuleChainSaved).isNotNull();
        assertThat(rootRuleChainSaved.getId()).isNotNull();
        assertThat(rootRuleChainSaved.getFirstRuleNodeId()).isNull();

        // Creating secondary Rule Chain
        final RuleChain secondaryRuleChainSaved = saveRuleChain(RuleChain.builder()
                .name("Secondary Rule Chain")
                .tenantId(savedTenant.getId())
                .root(false)
                .debugMode(true)
                .build());

        assertThat(secondaryRuleChainSaved).isNotNull();
        assertThat(secondaryRuleChainSaved.getId()).isNotNull();
        assertThat(secondaryRuleChainSaved.getFirstRuleNodeId()).isNull();

        //Add rule first node to the root rule chain and link onSuccess to the non-root rule chain
        RuleNode ruleNodeFirst = RuleNode.builder()
                .name("Simple Rule Node 1")
                .type(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName())
                .debugMode(true)
                .configuration(mapper.valueToTree(TbGetAttributesNodeConfiguration.builder()
                        .serverAttributeNames(List.of(SERVER_ATTRIBUTE_KEY_ONE)).build()))
                .build();

        RuleChainMetaData rootMetaData = saveRuleChainMetaData(RuleChainMetaData.builder()
                .ruleChainId(rootRuleChainSaved.getId())
                .nodes(List.of(ruleNodeFirst))
                .firstNodeIndex(0)
                .build()
                .addRuleChainConnectionInfo(0, secondaryRuleChainSaved.getId(), "Success", mapper.createObjectNode()));
        assertThat(rootMetaData).isNotNull();

        final RuleChain rootRuleChain = getRuleChain(rootRuleChainSaved.getId());
        assertThat(rootRuleChain.getFirstRuleNodeId()).isNotNull();

        final RuleNode ruleNodeSecond = RuleNode.builder()
                .name("Simple Rule Node 2")
                .type(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName())
                .debugMode(true)
                .configuration(mapper.valueToTree(TbGetAttributesNodeConfiguration.builder()
                        .serverAttributeNames(List.of(SERVER_ATTRIBUTE_KEY_TWO)).build()))
                .build();

        final RuleChainMetaData secondaryMetaData = saveRuleChainMetaData(RuleChainMetaData.builder()
                .ruleChainId(secondaryRuleChainSaved.getId())
                .nodes(List.of(ruleNodeSecond))
                .firstNodeIndex(0)
                .build());

        assertThat(secondaryMetaData).isNotNull();
        assertThat(secondaryMetaData.getNodes()).hasSize(1);

        // Saving the device. Sync API
        final Device device = doPost("/api/device", Device.builder()
                .name("My device")
                .type("default")
                .build(), Device.class);
        assertThat(device).isNotNull();

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        List.of(new BaseAttributeKvEntry(new StringDataEntry(SERVER_ATTRIBUTE_KEY_ONE, "serverAttributeValue1"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        List.of(new BaseAttributeKvEntry(new StringDataEntry(SERVER_ATTRIBUTE_KEY_TWO, "serverAttributeValue2"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);

        // Pushing Message to the system
        actorSystem.tell(qMsg);

        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();

        List<Event> events = await("Fetching events for the first rule node")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    log.info("Fetching events for first rule node {}", rootRuleChain.getFirstRuleNodeId());
                    PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), rootRuleChain.getFirstRuleNodeId(), PAGE_LIMIT);
                    List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
                    log.info("Fetched events [{}] {}", eventList.size(), eventList);
                    return eventList;
                }, hasSize(2));

        Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the first rule node"));
        assertThat(inEvent.getEntityId()).isEqualTo(rootRuleChain.getFirstRuleNodeId());
        assertThat(inEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the first rule node"));
        assertThat(outEvent.getEntityId()).isEqualTo(rootRuleChain.getFirstRuleNodeId());
        assertThat(outEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());
        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey1").asText()).isEqualTo("serverAttributeValue1");

        RuleNode lastRuleNode = secondaryMetaData.getNodes().stream().filter(node -> !node.getId().equals(rootRuleChain.getFirstRuleNodeId())).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No value present for the last rule node"));

        events = await("Fetching events for the last rule node").atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            log.info("Fetching events for the last rule node {}", lastRuleNode.getId());
            PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), lastRuleNode.getId(), PAGE_LIMIT);
            List<Event> eventList = eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
            log.info("Fetched events [{}] {}", eventList.size(), eventList);
            return eventList;
        }, hasSize(2));

        inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the last rule node"));
        assertThat(inEvent.getEntityId()).isEqualTo(lastRuleNode.getId());
        assertThat(inEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the last rule node"));
        assertThat(outEvent.getEntityId()).isEqualTo(lastRuleNode.getId());
        assertThat(outEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey1").asText()).isEqualTo("serverAttributeValue1");
        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey2").asText()).isEqualTo("serverAttributeValue2");
    }

}
