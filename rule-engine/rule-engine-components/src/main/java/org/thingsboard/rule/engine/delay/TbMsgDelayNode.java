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
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private static final String DELAYED_ORIGINATOR_IDS_CACHE_KEY = "delayed_originator_ids";
    private static final TbMsgMetaData EMPTY_META_DATA = new TbMsgMetaData();

    private TbMsgDelayNodeConfiguration config;
    private final Map<Integer, Set<EntityId>> partitionsEntityIdsMap;
    private final Map<EntityId, Map<UUID, TbMsg>> entityIdPendingMsgs;

    public TbMsgDelayNode() {
        this.partitionsEntityIdsMap = new HashMap<>();
        this.entityIdPendingMsgs = new HashMap<>();
    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        getPendingMsgsFromCacheAndSchedule(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(TB_MSG_DELAY_NODE_MSG)) {
            processDelayMsg(ctx, msg);
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    private void getPendingMsgsFromCacheAndSchedule(TbContext ctx) {
        log.trace("[{}] Going to fetch delayed messages from cache ...", ctx.getSelfId());
        Set<EntityId> entityIds = ctx.getRuleNodeCacheService().getEntityIds(DELAYED_ORIGINATOR_IDS_CACHE_KEY);
        if (entityIds.isEmpty()) {
            return;
        }
        long currentTs = System.currentTimeMillis();
        entityIds.forEach(id -> {
            TopicPartitionInfo tpi = ctx.getEntityTopicPartition(id);
            if (tpi.isMyPartition()) {
                // todo: check if we should use -999999 partition instead of orElse runnable.
                tpi.getPartition().ifPresentOrElse(
                        partition -> {
                            Set<EntityId> partitionEntityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
                            boolean added = partitionEntityIds.add(id);
                            if (added) {
                                Set<TbMsg> pendingMsgs = ctx.getRuleNodeCacheService().getTbMsgs(id, partition);
                                if (!pendingMsgs.isEmpty()) {
                                    Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdPendingMsgs.computeIfAbsent(id, k -> new HashMap<>());
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
                            }
                        },
                        () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), id)
                );
            }
        });
    }

    private void processDelayMsg(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getEntityTopicPartition(originator);
        if (!tpi.isMyPartition()) {
            return;
        }
        // todo: check if we should use -999999 partition instead of orElse runnable.
        tpi.getPartition().ifPresentOrElse(
                partition -> {
                    Map<UUID, TbMsg> pendingMsgs = entityIdPendingMsgs.get(originator);
                    if (pendingMsgs != null) {
                        TbMsg pendingMsg = pendingMsgs.remove(UUID.fromString(msg.getData()));
                        if (pendingMsg != null) {
                            processEnqueue(ctx, pendingMsg, partition);
                        }
                    }
                },
                () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), originator)
        );
    }

    private void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getEntityTopicPartition(id);
        if (tpi.isMyPartition()) {
            // todo: check if we should use -999999 partition instead of orElse runnable.
            tpi.getPartition().ifPresentOrElse(
                    partition -> {
                        Set<EntityId> entityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
                        boolean entityIdAdded = entityIds.add(id);
                        Map<UUID, TbMsg> originatorPendingMsgsMap = entityIdPendingMsgs.computeIfAbsent(id, k -> new HashMap<>());
                        if (originatorPendingMsgsMap.size() < config.getMaxPendingMsgs()) {
                            if (entityIdAdded) {
                                ctx.getRuleNodeCacheService().add(DELAYED_ORIGINATOR_IDS_CACHE_KEY, id);
                            }
                            long delay = getDelay(msg);
                            UUID msgId = msg.getId();
                            TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
                            metaDataCopy.putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + delay));
                            TbMsg transformedMsg = TbMsg.transformMsg(msg, metaDataCopy);
                            TbMsg added = originatorPendingMsgsMap.put(msgId, transformedMsg);
                            if (added == null) {
                                ctx.getRuleNodeCacheService().add(id, partition, transformedMsg);
                            }
                            scheduleTickMsg(ctx, delay, id, msgId);
                            ctx.ack(msg);
                        } else {
                            log.trace("[{}] Max limit of pending messages reached for originator id: [{}]", ctx.getSelfId(), id);
                            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for originator id: [" + id + "]"));
                        }
                    },
                    () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), id)
            );
        } else {
            log.trace("[{}][{}][{}] Ignore msg from entity that doesn't belong to local partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
        }
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
        ctx.enqueueForTellNext(TbMsg.transformMsg(pendingMsg, metaData), SUCCESS,
                () -> log.trace("[{}][{}] Successfully enqueue delayed message!", ctx.getSelfId(), originator),
                throwable -> log.trace("[{}][{}][{}] Failed to enqueue delayed message due to: ", ctx.getSelfId(), originator, throwable));
        ctx.getRuleNodeCacheService().removeTbMsgList(originator, partition, Collections.singletonList(pendingMsg));
    }

    private void scheduleTickMsg(TbContext ctx, long delay, EntityId originator, UUID msgId) {
        log.trace("[{}] Schedule delay tick msg for entity: [{}], msgId: [{}], delay: [{}]", ctx.getSelfId(), originator, msgId, delay);
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_DELAY_NODE_MSG, originator, EMPTY_META_DATA, msgId.toString());
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
            if (!partitionsEntityIdsMap.isEmpty()) {
                partitionsEntityIdsMap.forEach((partition, entityIds) ->
                        entityIds.forEach(id -> ctx.getRuleNodeCacheService().evictTbMsgs(id, partition)));
            }
            ctx.getRuleNodeCacheService().evict(DELAYED_ORIGINATOR_IDS_CACHE_KEY);
        }
        partitionsEntityIdsMap.clear();
        entityIdPendingMsgs.clear();
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        Set<Integer> currentPartitions = partitionsEntityIdsMap.keySet();
        RuleNodeId ruleNodeId = ctx.getSelfId();
        log.trace("[{}] On partition change msg: {}, current partitions: {}", ruleNodeId, msg, currentPartitions);
        Set<Integer> newPartitions = msg.getPartitions().stream()
                .map(TopicPartitionInfo::getPartition)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        currentPartitions.removeIf(partition -> {
            boolean remove = !newPartitions.contains(partition);
            if (remove) {
                log.trace("[{}] Removed odd partition: [{}] from the partitions map!", ruleNodeId, partition);
                Set<EntityId> entityIds = partitionsEntityIdsMap.get(partition);
                entityIds.forEach(entityId -> {
                    log.trace("[{}] Removed non-local entity: [{}] from the entityId pending msgs map!", ruleNodeId, entityId);
                    entityIdPendingMsgs.remove(entityId);
                });
            }
            return remove;
        });
        boolean checkCache = newPartitions.stream().anyMatch(newPartition -> !currentPartitions.contains(newPartition));
        if (checkCache) {
            getPendingMsgsFromCacheAndSchedule(ctx);
        }
    }

}
