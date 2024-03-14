/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

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

    private static final int DEFAULT_PARTITION = -999999;
    private static final String DELAYED_ORIGINATOR_IDS_CACHE_KEY = "delayed_originator_ids";

    private TbMsgDelayNodeConfiguration config;

    private final Map<Integer, Set<EntityId>> partitionsEntityIdsMap;
    private final Map<EntityId, Map<UUID, TbMsg>> entityIdValuesMap;

    public TbMsgDelayNode() {
        partitionsEntityIdsMap = new HashMap<>();
        entityIdValuesMap = new HashMap<>();
    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        getValuesFromCacheAndSchedule(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.isTypeOf(getTickMsgType())) {
            processOnTickMsg(ctx, msg);
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    public void destroy(TbContext ctx, ComponentLifecycleEvent reason) {
        if (ComponentLifecycleEvent.DELETED.equals(reason)) {
            if (!partitionsEntityIdsMap.isEmpty()) {
                partitionsEntityIdsMap.forEach((partition, entityIds) ->
                        entityIds.forEach(id ->
                                getCacheIfPresentAndExecute(ctx, cache -> cache.evictTbMsgs(id, partition))));
            }
            getCacheIfPresentAndExecute(ctx, cache -> cache.evict(getEntityIdsCacheKey()));
        }
        partitionsEntityIdsMap.clear();
        entityIdValuesMap.clear();
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        Set<Map.Entry<Integer, Set<EntityId>>> currentPartitions = partitionsEntityIdsMap.entrySet();
        RuleNodeId ruleNodeId = ctx.getSelfId();
        log.trace("[{}] On partition change msg: {}, current partitions: {}", ruleNodeId, msg, currentPartitions);
        Set<Integer> newPartitions = msg.getPartitions();
        currentPartitions.removeIf(entry -> {
            Integer partition = entry.getKey();
            boolean remove = !newPartitions.contains(partition);
            if (remove) {
                log.trace("[{}] Removed old partition: [{}] from the partitions map!", ruleNodeId, partition);
                Set<EntityId> entityIds = entry.getValue();
                entityIdValuesMap.keySet().removeAll(entityIds);
                if (log.isTraceEnabled()) {
                    entityIds.forEach(entityId -> log.trace("[{}] Removed non-local entity: [{}] from the entityId values map!", ruleNodeId, entityId));
                }
            }
            return remove;
        });
        boolean checkCache = newPartitions.stream().anyMatch(newPartition -> !partitionsEntityIdsMap.containsKey(newPartition));
        if (checkCache) {
            getValuesFromCacheAndSchedule(ctx);
        }
    }

    private void getValuesFromCacheAndSchedule(TbContext ctx) {
        log.trace("[{}] Going to fetch values from cache ...", ctx.getSelfId());
        getCacheIfPresentAndExecute(ctx, cache -> {
            Set<EntityId> entityIds = cache.getEntityIds(getEntityIdsCacheKey());
            if (entityIds.isEmpty()) {
                return;
            }
            entityIds.forEach(id -> {
                TopicPartitionInfo tpi = ctx.getTopicPartitionInfo(id);
                if (!tpi.isMyPartition()) {
                    log.trace("[{}][{}][{}] Ignore entity that doesn't belong to my partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
                } else {
                    Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
                    Set<EntityId> partitionEntityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
                    boolean added = partitionEntityIds.add(id);
                    if (added) {
                        getValuesFromCacheAndSchedule(ctx, cache, partition, id);
                    }
                }
            });
        });
    }

    private void getCacheIfPresentAndExecute(TbContext ctx, Consumer<RuleNodeCacheService> cacheOperation) {
        ctx.getRuleNodeCacheService().ifPresent(cacheOperation);
    }

    private void getValuesFromCacheAndSchedule(TbContext ctx, RuleNodeCacheService cache, Integer partition, EntityId id) {
        long currentTs = System.currentTimeMillis();
        Set<TbMsg> pendingMsgs = cache.getTbMsgs(id, partition);
        if (pendingMsgs.isEmpty()) {
            return;
        }
        Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdValuesMap.computeIfAbsent(id, k -> new HashMap<>());
        pendingMsgs.forEach(pendingMsg -> {
            long delayMsgScheduledTs = getDelayMsgScheduledTs(ctx, pendingMsg);
            if (currentTs >= delayMsgScheduledTs) {
                processEnqueue(ctx, pendingMsg, partition);
            } else {
                originatorPendingMsgsMap.put(pendingMsg.getId(), pendingMsg);
                scheduleTickMsg(ctx, delayMsgScheduledTs - currentTs, id, pendingMsg.getId());
            }
        });
    }

    private void processOnTickMsg(TbContext ctx, TbMsg msg) {
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

    private void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getTopicPartitionInfo(id);
        if (!tpi.isMyPartition()) {
            log.trace("[{}][{}][{}] Ignore msg from entity that doesn't belong to local partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
            return;
        }
        Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
        Set<EntityId> entityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
        boolean entityIdAdded = entityIds.add(id);
        Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdValuesMap.computeIfAbsent(id, k -> new HashMap<>());
        if (originatorPendingMsgsMap.size() >= config.getMaxPendingMsgs()) {
            log.trace("[{}] Max limit of pending messages reached for originator id: [{}]", ctx.getSelfId(), id);
            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for originator id: [" + id + "]"));
            return;
        }
        long delay = getDelay(msg);
        UUID msgId = msg.getId();
        TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
        metaDataCopy.putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + delay));
        TbMsg transformedMsg = TbMsg.transformMsgMetadata(msg, metaDataCopy);
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

    private TbMsgType getTickMsgType() {
        return TbMsgType.DELAY_TIMEOUT_SELF_MSG;
    }

    private String getEntityIdsCacheKey() {
        return DELAYED_ORIGINATOR_IDS_CACHE_KEY;
    }

    private long getDelayMsgScheduledTs(TbContext ctx, TbMsg msg) {
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
                        pendingMsg.getInternalType(),
                        pendingMsg.getOriginator(),
                        pendingMsg.getCustomerId(),
                        metaData,
                        pendingMsg.getData()
                ), SUCCESS,
                () -> log.trace("[{}][{}] Successfully enqueue delayed message!", ctx.getSelfId(), originator),
                throwable -> log.trace("[{}][{}] Failed to enqueue delayed message due to: ", ctx.getSelfId(), originator, throwable));
        getCacheIfPresentAndExecute(ctx, cache -> cache.removeTbMsgList(originator, partition, Collections.singletonList(pendingMsg)));
    }

    private void scheduleTickMsg(TbContext ctx, long delay, EntityId originator, UUID msgId) {
        log.trace("[{}] Schedule delay tick msg for entity: [{}], msgId: [{}], delay: [{}]", ctx.getSelfId(), originator, msgId, delay);
        TbMsg tickMsg = ctx.newMsg(null, getTickMsgType(), originator, TbMsgMetaData.EMPTY, msgId.toString());
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
