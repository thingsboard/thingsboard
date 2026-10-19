// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.ruleChain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RuleNodeActorMessageProcessorTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

    private ActorSystemContext systemContext;
    private RuleChainService ruleChainService;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        ruleChainService = mock(RuleChainService.class);
        ComponentDiscoveryService componentService = mock(ComponentDiscoveryService.class);
        when(systemContext.getRuleChainService()).thenReturn(ruleChainService);
        when(systemContext.getComponentService()).thenReturn(componentService);
        when(componentService.getRuleNodeInfo(any())).thenReturn(Optional.empty());
    }

    @Test
    public void usesPreloadedRuleNodeWithoutDatabaseLookup() {
        RuleNode preloaded = ruleNode();

        RuleNodeActorMessageProcessor processor = new RuleNodeActorMessageProcessor(tenantId, "chain", ruleNodeId,
                systemContext, mock(TbActorCtx.class), preloaded);

        verify(ruleChainService, never()).findRuleNodeById(any(), any());
        assertThat(processor.getComponentName()).isEqualTo(preloaded.getName());
    }

    @Test
    public void readsRuleNodeFromDatabaseWhenNotPreloaded() {
        when(ruleChainService.findRuleNodeById(tenantId, ruleNodeId)).thenReturn(ruleNode());

        new RuleNodeActorMessageProcessor(tenantId, "chain", ruleNodeId, systemContext, mock(TbActorCtx.class), null);

        verify(ruleChainService).findRuleNodeById(tenantId, ruleNodeId);
    }

    @Test
    public void readsRuleNodeFromDatabaseWhenPreloadedNodeHasDifferentId() {
        when(ruleChainService.findRuleNodeById(tenantId, ruleNodeId)).thenReturn(ruleNode());
        RuleNode other = ruleNode();
        other.setId(new RuleNodeId(UUID.randomUUID()));

        new RuleNodeActorMessageProcessor(tenantId, "chain", ruleNodeId, systemContext, mock(TbActorCtx.class), other);

        verify(ruleChainService).findRuleNodeById(tenantId, ruleNodeId);
    }

    @Test
    public void actorUsesPreloadedRuleNodeOnlyForFirstInitAttempt() {
        RuleNode preloaded = ruleNode();
        RuleNode fixed = ruleNode();
        fixed.setName("Save timeseries (fixed)");
        when(ruleChainService.findRuleNodeById(tenantId, ruleNodeId)).thenReturn(fixed);
        RuleNodeActor actor = (RuleNodeActor) new RuleNodeActor.ActorCreator(systemContext, tenantId,
                preloaded.getRuleChainId(), "chain", ruleNodeId, preloaded).createActor();

        RuleNodeActorMessageProcessor first = actor.createProcessor(mock(TbActorCtx.class));
        verify(ruleChainService, never()).findRuleNodeById(any(), any());
        assertThat(first.getComponentName()).isEqualTo(preloaded.getName());

        // an init retry, or a re-init after a failed start, must pick up the current rule node
        RuleNodeActorMessageProcessor retry = actor.createProcessor(mock(TbActorCtx.class));
        verify(ruleChainService).findRuleNodeById(tenantId, ruleNodeId);
        assertThat(retry.getComponentName()).isEqualTo(fixed.getName());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void copyOfKeepsAllFieldsAndDetachesConfiguration() {
        RuleNode original = ruleNode();

        RuleNode copy = RuleNodeActorMessageProcessor.copyOf(original);

        assertThat(copy).isEqualTo(original);
        assertThat(copy.getQueueName()).isEqualTo("HighPriority");
        assertThat(copy.getConfigurationVersion()).isEqualTo(3);
        assertThat(copy.isSingletonMode()).isTrue();
        assertThat(copy.getConfiguration()).isNotSameAs(original.getConfiguration());

        // secret substitution edits the configuration in place; the original must not see it
        ((ObjectNode) copy.getConfiguration()).put("password", "resolved-secret");
        assertThat(original.getConfiguration().get("password").asText()).isEqualTo("${secret:pwd}");
    }

    private RuleNode ruleNode() {
        RuleNode ruleNode = new RuleNode(ruleNodeId);
        ruleNode.setRuleChainId(new RuleChainId(UUID.randomUUID()));
        ruleNode.setName("Save timeseries");
        ruleNode.setType("org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode");
        ruleNode.setQueueName("HighPriority");
        ruleNode.setConfigurationVersion(3);
        ruleNode.setSingletonMode(true);
        ruleNode.setConfiguration(JacksonUtil.toJsonNode("{\"password\":\"${secret:pwd}\",\"ttl\":0}"));
        ruleNode.setAdditionalInfo(JacksonUtil.toJsonNode("{\"layoutX\":420,\"layoutY\":150}"));
        return ruleNode;
    }

}
