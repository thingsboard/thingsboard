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
package org.thingsboard.server.rules.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.event.EventService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineLifecycleIntegrationTest extends AbstractRuleEngineControllerTest {

    @Autowired
    protected ActorSystemContext actorSystem;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected EventService eventService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testRuleChainWithOneRule() throws Exception {
        // Creating Rule Chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Simple Rule Chain");
        ruleChain.setTenantId(tenantId);
        ruleChain.setRoot(true);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        Assert.assertNull(ruleChain.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Simple Rule Node");
        ruleNode.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setFetchTo(TbMsgSource.METADATA);
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey"));
        ruleNode.setConfiguration(JacksonUtil.valueToTree(configuration));

        metaData.setNodes(Collections.singletonList(ruleNode));
        metaData.setFirstNodeIndex(0);

        metaData = saveRuleChainMetaData(metaData);
        Assert.assertNotNull(metaData);

        final RuleChain ruleChainFinal = getRuleChain(ruleChain.getId());
        Assert.assertNotNull(ruleChainFinal.getFirstRuleNodeId());

        //TODO find out why RULE_NODE update event did not appear all the time
        List<EventInfo> rcEvents = Awaitility.await("Rule Node started successfully")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            List<EventInfo> debugEvents = getEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), EventType.LC_EVENT, 1000)
                                    .getData().stream().filter(e -> {
                                        var body = e.getBody();
                                        return body.has("event") && body.get("event").asText().equals("STARTED")
                                                && body.has("success") && body.get("success").asBoolean();
                                    }).collect(Collectors.toList());
                            debugEvents.forEach((e) -> log.trace("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 1);

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        log.warn("before update attr");
        attributesService.save(device.getTenantId(), device.getId(), AttributeScope.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey", "serverAttributeValue"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        log.warn("attr updated");
        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        Mockito.when(tbMsgCallback.isMsgValid()).thenReturn(true);
        TbMsg tbMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(device.getId())
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .callback(tbMsgCallback)
                .build();
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(tenantId, tbMsg, null, null);
        // Pushing Message to the system
        log.warn("before tell tbMsgCallback");
        actorSystem.tell(qMsg);
        log.warn("awaiting tbMsgCallback");
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();
        log.warn("awaiting events");
        List<EventInfo> events = Awaitility.await("get debug by post telemetry event")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            List<EventInfo> debugEvents = getDebugEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), 1000)
                                    .getData().stream().filter(filterByPostTelemetryEventType()).collect(Collectors.toList());
                            log.warn("filtered debug events [{}]", debugEvents.size());
                            debugEvents.forEach((e) -> log.warn("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 2);
        log.warn("asserting..");

        EventInfo inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        EventInfo outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        log.warn("OUT event {}", outEvent);
        log.warn("OUT event metadata {}", getMetadata(outEvent));

        Assert.assertNotNull("metadata has ss_serverAttributeKey", getMetadata(outEvent).get("ss_serverAttributeKey"));
        Assert.assertEquals("serverAttributeValue", getMetadata(outEvent).get("ss_serverAttributeKey").asText());
    }

}
