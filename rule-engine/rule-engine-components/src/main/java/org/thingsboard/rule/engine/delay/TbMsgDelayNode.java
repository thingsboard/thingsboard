/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.delay;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delay",
        configClazz = TbMsgDelayNodeConfiguration.class,
        nodeDescription = "Delays incoming message",
        nodeDetails = "Delays messages for a configurable period. " +
                "Please note, this node acknowledges the message from the current queue (message will be removed from queue). " +
                "The acknowledged message stays in memory to be delayed. Additionally, if the cache type is set to <b>Redis</b>, " +
                "the message will be persisted in the cache, which guarantees that message will be processed even after the server restart." +
                "<b>Important note:</b> If the incoming message is processed in the queue with a sequential processing strategy configured, " +
                "the message acknowledgment that used in the rule node logic will trigger the next message to be processed by the queue.",
        icon = "pause",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDelayConfig"
)
public class TbMsgDelayNode implements TbNode {

    private static final String TB_MSG_DELAY_NODE_MSG = "TbMsgDelayNodeMsg";
    private static final String DELAYED_MSG_IDS_CACHE_KEY = "delayed_msg_ids";
    private static final TbMsgMetaData EMPTY_META_DATA = new TbMsgMetaData();

    private TbMsgDelayNodeConfiguration config;
    private Map<UUID, TbMsg> pendingMsgs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        this.pendingMsgs = new HashMap<>();
        getPendingMsgsFromCacheAndSchedule(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(TB_MSG_DELAY_NODE_MSG)) {
            processDelayMsg(ctx, msg);
        } else {
            processRegularMsg(ctx, msg);
        }
    }

    private void processDelayMsg(TbContext ctx, TbMsg msg) {
        UUID pendingMsgId = UUID.fromString(msg.getData());
        TbMsg pendingMsg = pendingMsgs.remove(pendingMsgId);
        if (pendingMsg != null) {
            processEnqueue(ctx, pendingMsg);
        }
    }

    private void processRegularMsg(TbContext ctx, TbMsg msg) {
        if (pendingMsgs.size() < config.getMaxPendingMsgs()) {
            long delay = getDelay(msg);
            UUID msgId = msg.getId();
            String msgIdStr = msgId.toString();
            TbMsg added = pendingMsgs.put(msgId, msg);
            if (added == null) {
                ctx.getRuleNodeCacheService().add(DELAYED_MSG_IDS_CACHE_KEY, msgIdStr);
            }
            TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
            metaDataCopy.putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + delay));
            ctx.getRuleNodeCacheService().add(msgIdStr, TbMsg.transformMsg(msg, metaDataCopy));
            scheduleTickMsg(ctx, delay, msgIdStr);
            ctx.ack(msg);
        } else {
            ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached!"));
        }
    }

    private void getPendingMsgsFromCacheAndSchedule(TbContext ctx) {
        Set<String> pendingMsgIds = ctx.getRuleNodeCacheService().getStrings(DELAYED_MSG_IDS_CACHE_KEY);
        if (pendingMsgIds == null || pendingMsgIds.isEmpty()) {
            return;
        }
        long currentTs = System.currentTimeMillis();
        pendingMsgIds.forEach(msgIdStr -> {
            Set<TbMsg> delayedMsgs = ctx.getRuleNodeCacheService().getTbMsgs(msgIdStr);
            delayedMsgs.forEach(pendingMsg -> {
                long delayMsgTimeout = getDelayTimeout(ctx, pendingMsg);
                if (currentTs >= delayMsgTimeout) {
                    processEnqueue(ctx, pendingMsg);
                } else {
                    scheduleTickMsg(ctx, delayMsgTimeout - currentTs, msgIdStr);
                }
            });
        });
    }

    private long getDelayTimeout(TbContext ctx, TbMsg msg) {
        String delayMsgTimeoutStr = msg.getMetaData().getData().remove(ctx.getSelfId().getId().toString());
        if (StringUtils.isNotEmpty(delayMsgTimeoutStr)) {
            try {
                return Long.parseLong(delayMsgTimeoutStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void processEnqueue(TbContext ctx, TbMsg pendingMsg) {
        ctx.enqueueForTellNext(pendingMsg, SUCCESS,
                () -> log.trace("[{}][{}] Successfully enqueue delayed message!", ctx.getSelfId(), pendingMsg.getOriginator()),
                throwable -> log.trace("[{}][{}][{}] Failed to enqueue delayed message due to: ", ctx.getSelfId(), pendingMsg.getOriginator(), throwable));
        evictMsgFromCache(ctx, pendingMsg.getId().toString());
    }

    private void evictMsgFromCache(TbContext ctx, String pendingMsgId) {
        ctx.getRuleNodeCacheService().evict(pendingMsgId);
        ctx.getRuleNodeCacheService().removeStringList(DELAYED_MSG_IDS_CACHE_KEY, Collections.singletonList(pendingMsgId));
    }

    private void scheduleTickMsg(TbContext ctx, long delay, String msgIdStr) {
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_DELAY_NODE_MSG, ctx.getSelfId(), EMPTY_META_DATA, msgIdStr);
        ctx.tellSelf(tickMsg, delay);
    }

    private long getDelay(TbMsg msg) {
        int periodInSeconds;
        if (config.isUseMetadataPeriodInSecondsPatterns()) {
            if (isParsable(msg, config.getPeriodInSecondsPattern())) {
                periodInSeconds = Integer.parseInt(TbNodeUtils.processPattern(config.getPeriodInSecondsPattern(), msg));
            } else {
                throw new RuntimeException("Can't parse period in seconds from metadata using pattern: " + config.getPeriodInSecondsPattern());
            }
        } else {
            periodInSeconds = config.getPeriodInSeconds();
        }
        return TimeUnit.SECONDS.toMillis(periodInSeconds);
    }

    private boolean isParsable(TbMsg msg, String pattern) {
        return NumberUtils.isParsable(TbNodeUtils.processPattern(pattern, msg));
    }

    @Override
    public void destroy(TbContext ctx, ComponentLifecycleEvent reason) {
        if (ComponentLifecycleEvent.DELETED.equals(reason)) {
            pendingMsgs.keySet().forEach(id -> ctx.getRuleNodeCacheService().evict(id.toString()));
            ctx.getRuleNodeCacheService().evict(DELAYED_MSG_IDS_CACHE_KEY);
        }
        pendingMsgs.clear();
    }

}
