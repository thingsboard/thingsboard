/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.cache.TbAbstractCacheBasedRuleNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
public class TbMsgDelayNode extends TbAbstractCacheBasedRuleNode<TbMsgDelayNodeConfiguration, Map<UUID, TbMsg>> {

    private static final String TB_MSG_DELAY_NODE_MSG = "TbMsgDelayNodeMsg";
    private static final String DELAYED_ORIGINATOR_IDS_CACHE_KEY = "delayed_originator_ids";

    @Override
    protected TbMsgDelayNodeConfiguration loadRuleNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
    }

    @Override
    protected void getValuesFromCacheAndSchedule(TbContext ctx, RuleNodeCacheService cache, Integer partition, EntityId id) {
        long currentTs = System.currentTimeMillis();
        Set<TbMsg> pendingMsgs = cache.getTbMsgs(id, partition);
        if (pendingMsgs.isEmpty()) {
            return;
        }
        Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdValuesMap.computeIfAbsent(id, k -> new HashMap<>());
        pendingMsgs.forEach(pendingMsg -> {
            long delayMsgScheduledTs = getDelayTimeout(ctx, pendingMsg);
            if (currentTs >= delayMsgScheduledTs) {
                processEnqueue(ctx, pendingMsg, partition);
            } else {
                originatorPendingMsgsMap.put(pendingMsg.getId(), pendingMsg);
                scheduleTickMsg(ctx, delayMsgScheduledTs - currentTs, id, pendingMsg.getId());
            }
        });
    }

    @Override
    protected void processOnTickMsg(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getTopicPartitionInfo(originator);
        if (!tpi.isMyPartition()) {
            return;
        }
        Map<UUID, TbMsg> pendingMsgs = entityIdValuesMap.get(originator);
        if (pendingMsgs == null) {
            return;
        }
        TbMsg pendingMsg = pendingMsgs.remove(UUID.fromString(msg.getData()));
        if (pendingMsg == null) {
            return;
        }
        processEnqueue(ctx, pendingMsg, tpi.getPartition().orElse(DEFAULT_PARTITION));
    }

    @Override
    protected void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getTopicPartitionInfo(id);
        if (!tpi.isMyPartition()) {
            log.trace("[{}][{}][{}] Ignore msg from entity that doesn't belong to local partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
        } else {
            Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
            Set<EntityId> entityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
            boolean entityIdAdded = entityIds.add(id);
            Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdValuesMap.computeIfAbsent(id, k -> new HashMap<>());
            if (originatorPendingMsgsMap.size() >= config.getMaxPendingMsgs()) {
                log.trace("[{}] Max limit of pending messages reached for originator id: [{}]", ctx.getSelfId(), id);
                ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for originator id: [" + id + "]"));
            } else {
                long delay = getDelay(msg);
                UUID msgId = msg.getId();
                TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
                metaDataCopy.putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + delay));
                TbMsg transformedMsg = TbMsg.transformMsg(msg, metaDataCopy);
                TbMsg added = originatorPendingMsgsMap.put(msgId, transformedMsg);
                getCacheIfPresentAndExecute(ctx, cache -> {
                    if (entityIdAdded) {
                        cache.add(getEntityIdsCacheKey(), id);
                    }
                    if (added == null) {
                        cache.add(id, partition, transformedMsg);
                    }
                });
                ctx.ack(msg);
                scheduleTickMsg(ctx, delay, id, msgId);
            }
        }
    }

    @Override
    protected String getTickMsgType() {
        return TB_MSG_DELAY_NODE_MSG;
    }

    @Override
    protected String getEntityIdsCacheKey() {
        return DELAYED_ORIGINATOR_IDS_CACHE_KEY;
    }

    private long getDelayTimeout(TbContext ctx, TbMsg msg) {
        String delayMsgTimeoutStr = msg.getMetaData().getData().get(ctx.getSelfId().getId().toString());
        if (StringUtils.isNotEmpty(delayMsgTimeoutStr)) {
            try {
                return Long.parseLong(delayMsgTimeoutStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void processEnqueue(TbContext ctx, TbMsg pendingMsg, Integer partition) {
        EntityId originator = pendingMsg.getOriginator();
        TbMsgMetaData metaData = pendingMsg.getMetaData().copy();
        metaData.getData().remove(ctx.getSelfId().getId().toString());
        ctx.enqueueForTellNext(
                TbMsg.newMsg(
                        pendingMsg.getQueueName(),
                        pendingMsg.getType(),
                        pendingMsg.getOriginator(),
                        pendingMsg.getCustomerId(),
                        metaData,
                        pendingMsg.getData()
                ), SUCCESS,
                () -> log.trace("[{}][{}] Successfully enqueue delayed message!", ctx.getSelfId(), originator),
                throwable -> log.trace("[{}][{}][{}] Failed to enqueue delayed message due to: ", ctx.getSelfId(), originator, throwable));
        getCacheIfPresentAndExecute(ctx, cache -> cache.removeTbMsgList(originator, partition, Collections.singletonList(pendingMsg)));
    }

    private void scheduleTickMsg(TbContext ctx, long delay, EntityId originator, UUID msgId) {
        log.trace("[{}] Schedule delay tick msg for entity: [{}], msgId: [{}], delay: [{}]", ctx.getSelfId(), originator, msgId, delay);
        TbMsg tickMsg = ctx.newMsg(null, getTickMsgType(), originator, EMPTY_META_DATA, msgId.toString());
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

}
