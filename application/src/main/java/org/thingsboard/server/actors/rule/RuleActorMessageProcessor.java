/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.actors.rule;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.util.StringUtils;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.plugin.RuleToPluginMsgWrapper;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.core.BasicRequest;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.common.msg.core.RuleEngineError;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.common.msg.session.ex.ProcessingTimeoutException;
import org.thingsboard.server.extensions.api.rules.*;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

class RuleActorMessageProcessor extends ComponentMsgProcessor<RuleId> {

    private final RuleProcessingContext ruleCtx;
    private final Map<UUID, RuleProcessingMsg> pendingMsgMap;

    private RuleMetaData ruleMd;
    private ComponentLifecycleState state;
    private List<RuleFilter> filters;
    private RuleProcessor processor;
    private PluginAction action;

    private TenantId pluginTenantId;
    private PluginId pluginId;

    protected RuleActorMessageProcessor(TenantId tenantId, RuleId ruleId, ActorSystemContext systemContext, LoggingAdapter logger) {
        super(systemContext, logger, tenantId, ruleId);
        this.pendingMsgMap = new HashMap<>();
        this.ruleCtx = new RuleProcessingContext(systemContext, ruleId);
    }

    @Override
    public void start() throws Exception {
        logger.info("[{}][{}] Starting rule actor.", entityId, tenantId);
        ruleMd = systemContext.getRuleService().findRuleById(entityId);
        if (ruleMd == null) {
            throw new RuleInitializationException("Rule not found!");
        }
        state = ruleMd.getState();
        if (state == ComponentLifecycleState.ACTIVE) {
            logger.info("[{}] Rule is active. Going to initialize rule components.", entityId);
            initComponent();
        } else {
            logger.info("[{}] Rule is suspended. Skipping rule components initialization.", entityId);
        }

        logger.info("[{}][{}] Started rule actor.", entityId, tenantId);
    }

    @Override
    public void stop() throws Exception {
        onStop();
    }


    private void initComponent() throws RuleException {
        try {
            if (!ruleMd.getFilters().isArray()) {
                throw new RuntimeException("Filters are not array!");
            }
            fetchPluginInfo();
            initFilters();
            initProcessor();
            initAction();
        } catch (RuntimeException e) {
            throw new RuleInitializationException("Unknown runtime exception!", e);
        } catch (InstantiationException e) {
            throw new RuleInitializationException("No default constructor for rule implementation!", e);
        } catch (IllegalAccessException e) {
            throw new RuleInitializationException("Illegal Access Exception during rule initialization!", e);
        } catch (ClassNotFoundException e) {
            throw new RuleInitializationException("Rule Class not found!", e);
        } catch (Exception e) {
            throw new RuleException(e.getMessage(), e);
        }
    }

    private void initAction() throws Exception {
        if (ruleMd.getAction() != null && !ruleMd.getAction().isNull()) {
            action = initComponent(ruleMd.getAction());
        }
    }

    private void initProcessor() throws Exception {
        if (ruleMd.getProcessor() != null && !ruleMd.getProcessor().isNull()) {
            processor = initComponent(ruleMd.getProcessor());
        }
    }

    private void initFilters() throws Exception {
        filters = new ArrayList<>(ruleMd.getFilters().size());
        for (int i = 0; i < ruleMd.getFilters().size(); i++) {
            filters.add(initComponent(ruleMd.getFilters().get(i)));
        }
    }

    private void fetchPluginInfo() {
        if (!StringUtils.isEmpty(ruleMd.getPluginToken())) {
            PluginMetaData pluginMd = systemContext.getPluginService().findPluginByApiToken(ruleMd.getPluginToken());
            pluginTenantId = pluginMd.getTenantId();
            pluginId = pluginMd.getId();
        }
    }

    protected void onRuleProcessingMsg(ActorContext context, RuleProcessingMsg msg) throws RuleException {
        if (state != ComponentLifecycleState.ACTIVE) {
            pushToNextRule(context, msg.getCtx(), RuleEngineError.NO_ACTIVE_RULES);
            return;
        }
        ChainProcessingContext chainCtx = msg.getCtx();
        ToDeviceActorMsg inMsg = chainCtx.getInMsg();

        ruleCtx.update(inMsg, chainCtx.getDeviceMetaData());

        logger.debug("[{}] Going to filter in msg: {}", entityId, inMsg);
        for (RuleFilter filter : filters) {
            if (!filter.filter(ruleCtx, inMsg)) {
                logger.debug("[{}] In msg is NOT valid for processing by current rule: {}", entityId, inMsg);
                pushToNextRule(context, msg.getCtx(), RuleEngineError.NO_FILTERS_MATCHED);
                return;
            }
        }
        RuleProcessingMetaData inMsgMd;
        if (processor != null) {
            logger.debug("[{}] Going to process in msg: {}", entityId, inMsg);
            inMsgMd = processor.process(ruleCtx, inMsg);
        } else {
            inMsgMd = new RuleProcessingMetaData();
        }
        logger.debug("[{}] Going to convert in msg: {}", entityId, inMsg);
        if (action != null) {
            Optional<RuleToPluginMsg<?>> ruleToPluginMsgOptional = action.convert(ruleCtx, inMsg, inMsgMd);
            if (ruleToPluginMsgOptional.isPresent()) {
                RuleToPluginMsg<?> ruleToPluginMsg = ruleToPluginMsgOptional.get();
                logger.debug("[{}] Device msg is converter to: {}", entityId, ruleToPluginMsg);
                context.parent().tell(new RuleToPluginMsgWrapper(pluginTenantId, pluginId, tenantId, entityId, ruleToPluginMsg), context.self());
                if (action.isOneWayAction()) {
                    pushToNextRule(context, msg.getCtx(), RuleEngineError.NO_TWO_WAY_ACTIONS);
                    return;
                } else {
                    pendingMsgMap.put(ruleToPluginMsg.getUid(), msg);
                    scheduleMsgWithDelay(context, new RuleToPluginTimeoutMsg(ruleToPluginMsg.getUid()), systemContext.getPluginProcessingTimeout());
                    return;
                }
            }
        }
        logger.debug("[{}] Nothing to send to plugin: {}", entityId, pluginId);
        pushToNextRule(context, msg.getCtx(), RuleEngineError.NO_TWO_WAY_ACTIONS);
    }

    void onPluginMsg(ActorContext context, PluginToRuleMsg<?> msg) {
        RuleProcessingMsg pendingMsg = pendingMsgMap.remove(msg.getUid());
        if (pendingMsg != null) {
            ChainProcessingContext ctx = pendingMsg.getCtx();
            Optional<ToDeviceMsg> ruleResponseOptional = action.convert(msg);
            if (ruleResponseOptional.isPresent()) {
                ctx.mergeResponse(ruleResponseOptional.get());
                pushToNextRule(context, ctx, null);
            } else {
                pushToNextRule(context, ctx, RuleEngineError.NO_RESPONSE_FROM_ACTIONS);
            }
        } else {
            logger.warning("[{}] Processing timeout detected: [{}]", entityId, msg.getUid());
        }
    }

    void onTimeoutMsg(ActorContext context, RuleToPluginTimeoutMsg msg) {
        RuleProcessingMsg pendingMsg = pendingMsgMap.remove(msg.getMsgId());
        if (pendingMsg != null) {
            logger.debug("[{}] Processing timeout detected [{}]: {}", entityId, msg.getMsgId(), pendingMsg);
            ChainProcessingContext ctx = pendingMsg.getCtx();
            pushToNextRule(context, ctx, RuleEngineError.PLUGIN_TIMEOUT);
        }
    }

    private void pushToNextRule(ActorContext context, ChainProcessingContext ctx, RuleEngineError error) {
        if (error != null) {
            ctx = ctx.withError(error);
        }
        if (ctx.isFailure()) {
            logger.debug("[{}][{}] Forwarding processing chain to device actor due to failure.", ruleMd.getId(), ctx.getInMsg().getDeviceId());
            ctx.getDeviceActor().tell(new RulesProcessedMsg(ctx), ActorRef.noSender());
        } else if (!ctx.hasNext()) {
            logger.debug("[{}][{}] Forwarding processing chain to device actor due to end of chain.", ruleMd.getId(), ctx.getInMsg().getDeviceId());
            ctx.getDeviceActor().tell(new RulesProcessedMsg(ctx), ActorRef.noSender());
        } else {
            logger.debug("[{}][{}] Forwarding processing chain to next rule actor.", ruleMd.getId(), ctx.getInMsg().getDeviceId());
            ChainProcessingContext nextTask = ctx.getNext();
            nextTask.getCurrentActor().tell(new RuleProcessingMsg(nextTask), context.self());
        }
    }

    @Override
    public void onCreated(ActorContext context) {
        logger.info("[{}] Going to process onCreated rule.", entityId);
    }

    @Override
    public void onUpdate(ActorContext context) throws RuleException {
        RuleMetaData oldRuleMd = ruleMd;
        ruleMd = systemContext.getRuleService().findRuleById(entityId);
        logger.info("[{}] Rule configuration was updated from {} to {}.", entityId, oldRuleMd, ruleMd);
        try {
            fetchPluginInfo();
            if (filters == null || !Objects.equals(oldRuleMd.getFilters(), ruleMd.getFilters())) {
                logger.info("[{}] Rule filters require restart due to json change from {} to {}.",
                        entityId, mapper.writeValueAsString(oldRuleMd.getFilters()), mapper.writeValueAsString(ruleMd.getFilters()));
                stopFilters();
                initFilters();
            }
            if (processor == null || !Objects.equals(oldRuleMd.getProcessor(), ruleMd.getProcessor())) {
                logger.info("[{}] Rule processor require restart due to configuration change.", entityId);
                stopProcessor();
                initProcessor();
            }
            if (action == null || !Objects.equals(oldRuleMd.getAction(), ruleMd.getAction())) {
                logger.info("[{}] Rule action require restart due to configuration change.", entityId);
                stopAction();
                initAction();
            }
        } catch (RuntimeException e) {
            throw new RuleInitializationException("Unknown runtime exception!", e);
        } catch (InstantiationException e) {
            throw new RuleInitializationException("No default constructor for rule implementation!", e);
        } catch (IllegalAccessException e) {
            throw new RuleInitializationException("Illegal Access Exception during rule initialization!", e);
        } catch (ClassNotFoundException e) {
            throw new RuleInitializationException("Rule Class not found!", e);
        } catch (JsonProcessingException e) {
            throw new RuleInitializationException("Rule configuration is invalid!", e);
        } catch (Exception e) {
            throw new RuleInitializationException(e.getMessage(), e);
        }
    }

    @Override
    public void onActivate(ActorContext context) throws Exception {
        logger.info("[{}] Going to process onActivate rule.", entityId);
        this.state = ComponentLifecycleState.ACTIVE;
        if (filters != null) {
            filters.forEach(RuleLifecycleComponent::resume);
            if (processor != null) {
                processor.resume();
            } else {
                initProcessor();
            }
            if (action != null) {
                action.resume();
            }
            logger.info("[{}] Rule resumed.", entityId);
        } else {
            start();
        }
    }

    @Override
    public void onSuspend(ActorContext context) {
        logger.info("[{}] Going to process onSuspend rule.", entityId);
        this.state = ComponentLifecycleState.SUSPENDED;
        if (filters != null) {
            filters.forEach(f -> f.suspend());
        }
        if (processor != null) {
            processor.suspend();
        }
        if (action != null) {
            action.suspend();
        }
    }

    @Override
    public void onStop(ActorContext context) {
        logger.info("[{}] Going to process onStop rule.", entityId);
        onStop();
        scheduleMsgWithDelay(context, new RuleTerminationMsg(entityId), systemContext.getRuleActorTerminationDelay());
    }

    private void onStop() {
        this.state = ComponentLifecycleState.SUSPENDED;
        stopFilters();
        stopProcessor();
        stopAction();
    }

    @Override
    public void onClusterEventMsg(ClusterEventMsg msg) throws Exception {

    }

    private void stopAction() {
        if (action != null) {
            action.stop();
        }
    }

    private void stopProcessor() {
        if (processor != null) {
            processor.stop();
        }
    }

    private void stopFilters() {
        if (filters != null) {
            filters.forEach(f -> f.stop());
        }
    }
}
