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
package org.thingsboard.server.rules.lifecycle;

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
public abstract class AbstractRuleEngineLifecycleIntegrationTest extends AbstractRuleEngineControllerTest {

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
    public void testRuleChainWithOneRule() throws Exception {
        // Creating Rule Chain
        final RuleChain ruleChainInitial = saveRuleChain(RuleChain.builder()
                .name("Simple Rule Chain")
                .tenantId(savedTenant.getId())
                .root(true)
                .debugMode(true)
                .build());
        assertThat(ruleChainInitial).isNotNull();
        assertThat(ruleChainInitial.getFirstRuleNodeId()).isNull();

        final RuleNode ruleNode = RuleNode.builder()
                .name("Simple Rule Node")
                .type(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName())
                .debugMode(true)
                .configuration(mapper.valueToTree(TbGetAttributesNodeConfiguration.builder()
                        .serverAttributeNames(List.of("serverAttributeKey")).build()))
                .build();

        final RuleChainMetaData metaData = saveRuleChainMetaData(RuleChainMetaData.builder()
                .ruleChainId(ruleChainInitial.getId())
                .nodes(List.of(ruleNode))
                .firstNodeIndex(0)
                .build());
        assertThat(metaData).isNotNull();

        final RuleChain ruleChain = getRuleChain(ruleChainInitial.getId());
        assertThat(ruleChain.getFirstRuleNodeId()).isNotNull();

        // Saving the device
        final Device device = doPost("/api/device", Device.builder().name("My device").type("default").build(), Device.class);

        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                        List.of(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey", "serverAttributeValue"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);

        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(savedTenant.getId(), tbMsg, null, null);

        // Pushing Message to the system
        actorSystem.tell(qMsg);
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();


        final List<Event> events = await("Fetching events for the first rule node")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<Event> eventsPage = getDebugEvents(savedTenant.getId(), ruleChain.getFirstRuleNodeId(), PAGE_LIMIT);
                    return eventsPage.getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
                }, hasSize(2));

        final Event inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type IN for the first rule node"));
        assertThat(inEvent.getEntityId()).isEqualTo(ruleChain.getFirstRuleNodeId());
        assertThat(inEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        final Event outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No event with type OUT for the first rule node"));
        assertThat(ruleChain.getFirstRuleNodeId()).isEqualTo(outEvent.getEntityId());
        assertThat(outEvent.getBody().get("entityId").asText()).isEqualTo(device.getId().getId().toString());

        assertThat(getMetadata(outEvent).get("ss_serverAttributeKey").asText()).isEqualTo("serverAttributeValue");
    }

}
