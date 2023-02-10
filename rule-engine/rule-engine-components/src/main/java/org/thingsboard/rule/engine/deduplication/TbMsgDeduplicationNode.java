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
package org.thingsboard.rule.engine.deduplication;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "deduplication",
        configClazz = TbMsgDeduplicationNodeConfiguration.class,
        nodeDescription = "Deduplicate messages for a configurable period based on a specified deduplication strategy.",
        nodeDetails = "Rule node allows you to select one of the following strategy to deduplicate messages: <br></br>" +
                "<b>FIRST</b> - return first message that arrived during deduplication period.<br></br>" +
                "<b>LAST</b> - return last message that arrived during deduplication period.<br></br>" +
                "<b>ALL</b> - return all messages as a single JSON array message. Where each element represents object with <b>msg</b> and <b>metadata</b> inner properties.<br></br>",
        icon = "content_copy",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDeduplicationConfig"
)
@Slf4j
public class TbMsgDeduplicationNode implements TbNode {

    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeduplicationNodeMsg";
    private static final String DEDUPLICATION_IDS_CACHE_KEY = "deduplication_ids";
    public static final int TB_MSG_DEDUPLICATION_RETRY_DELAY = 10;
    private static final String EMPTY_DATA = "";
    private static final TbMsgMetaData EMPTY_META_DATA = new TbMsgMetaData();

    private TbMsgDeduplicationNodeConfiguration config;

    private final Map<Integer, Set<EntityId>> partitionsEntityIdsMap;
    private final Map<EntityId, DeduplicationData> entityIdDeduplicationMsgsMap;

    private long deduplicationInterval;

    public TbMsgDeduplicationNode() {
        this.partitionsEntityIdsMap = new HashMap<>();
        this.entityIdDeduplicationMsgsMap = new HashMap<>();
    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        getDeduplicationDataFromCacheAndSchedule(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (TB_MSG_DEDUPLICATION_TIMEOUT_MSG.equals(msg.getType())) {
            processDeduplication(ctx, msg.getOriginator());
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    public void destroy(TbContext ctx, ComponentLifecycleEvent reason) {
        if (ComponentLifecycleEvent.DELETED.equals(reason)) {
            if (!partitionsEntityIdsMap.isEmpty()) {
                partitionsEntityIdsMap.forEach((partition, entityIds) ->
                        entityIds.forEach(id -> ctx.getRuleNodeCacheService().evictTbMsgs(id.toString(), partition)));
            }
            ctx.getRuleNodeCacheService().evict(DEDUPLICATION_IDS_CACHE_KEY);
        }
        partitionsEntityIdsMap.clear();
        entityIdDeduplicationMsgsMap.clear();
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        Set<Integer> myPartitions = new HashSet<>();
        msg.getPartitions().forEach(tpi -> tpi.getPartition().ifPresent(myPartitions::add));
        partitionsEntityIdsMap.keySet().removeIf(partition -> !myPartitions.contains(partition));
        entityIdDeduplicationMsgsMap.keySet().removeIf(entityId -> !ctx.isLocalEntity(entityId));
    }

    private void getDeduplicationDataFromCacheAndSchedule(TbContext ctx) {
        ctx.getRuleNodeCacheService().getEntityIds(DEDUPLICATION_IDS_CACHE_KEY).forEach(id -> {
            TopicPartitionInfo tpi = ctx.getEntityTopicPartition(id);
            if (tpi.isMyPartition()) {
                tpi.getPartition().ifPresentOrElse(
                        partition -> {
                            List<TbMsg> tbMsgs = new ArrayList<>(ctx.getRuleNodeCacheService().getTbMsgs(id.toString(), partition));
                            Set<EntityId> entityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
                            entityIds.add(id);
                            DeduplicationData deduplicationData = entityIdDeduplicationMsgsMap.computeIfAbsent(id, k -> new DeduplicationData());
                            deduplicationData.addAll(tbMsgs);
                            scheduleTickMsg(ctx, id, deduplicationData, 0);
                        },
                        () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), id));
            } else {
                log.trace("[{}][{}][{}] Ignore entity that doesn't belong to my partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
            }
        });
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
                        DeduplicationData deduplicationMsgs = entityIdDeduplicationMsgsMap.computeIfAbsent(id, k -> new DeduplicationData());
                        if (deduplicationMsgs.size() < config.getMaxPendingMsgs()) {
                            log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), id, msg.getId(), msg.getMetaDataTs());
                            if (entityIdAdded) {
                                ctx.getRuleNodeCacheService().add(DEDUPLICATION_IDS_CACHE_KEY, id);
                            }
                            deduplicationMsgs.add(msg);
                            ctx.getRuleNodeCacheService().add(id.toString(), partition, msg);
                            ctx.ack(msg);
                            scheduleTickMsg(ctx, id, deduplicationMsgs);
                        } else {
                            log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), id);
                            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + id + "]"));
                        }
                    },
                    () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), id)
            );
        } else {
            log.trace("[{}][{}][{}] Ignore msg from entity that doesn't belong to my partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
        }
    }

    private void processDeduplication(TbContext ctx, EntityId deduplicationId) {
        TopicPartitionInfo tpi = ctx.getEntityTopicPartition(deduplicationId);
        if (!tpi.isMyPartition()) {
            return;
        }
        // todo: check if we should use -999999 partition instead of orElse runnable.
        tpi.getPartition().ifPresentOrElse(partition -> {
                    DeduplicationData data = entityIdDeduplicationMsgsMap.get(deduplicationId);
                    if (data == null) {
                        return;
                    }
                    data.setTickScheduled(false);
                    if (data.isEmpty()) {
                        return;
                    }
                    long deduplicationTimeoutMs = System.currentTimeMillis();
                    try {
                        List<TbPair<TbMsg, List<TbMsg>>> deduplicationResults = new ArrayList<>();
                        List<TbMsg> msgList = data.getMsgList();
                        Optional<TbPair<Long, Long>> packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
                        while (packBoundsOpt.isPresent()) {
                            TbPair<Long, Long> packBounds = packBoundsOpt.get();
                            List<TbMsg> pack = new ArrayList<>();
                            if (DeduplicationStrategy.ALL.equals(config.getStrategy())) {
                                for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                                    TbMsg msg = iterator.next();
                                    long msgTs = msg.getMetaDataTs();
                                    if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                                        pack.add(msg);
                                        iterator.remove();
                                    }
                                }
                                deduplicationResults.add(new TbPair<>(TbMsg.newMsg(
                                        config.getQueueName(),
                                        config.getOutMsgType(),
                                        deduplicationId,
                                        getMetadata(packBounds.getFirst()),
                                        getMergedData(pack)), pack));
                            } else {
                                TbMsg resultMsg = null;
                                boolean searchMin = DeduplicationStrategy.FIRST.equals(config.getStrategy());
                                for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                                    TbMsg msg = iterator.next();
                                    long msgTs = msg.getMetaDataTs();
                                    if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                                        pack.add(msg);
                                        iterator.remove();
                                        if (resultMsg == null
                                                || (searchMin && msg.getMetaDataTs() < resultMsg.getMetaDataTs())
                                                || (!searchMin && msg.getMetaDataTs() > resultMsg.getMetaDataTs())) {
                                            resultMsg = msg;
                                        }
                                    }
                                }
                                if (resultMsg != null) {
                                    deduplicationResults.add(new TbPair<>(resultMsg, pack));
                                }
                            }
                            packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
                        }
                        deduplicationResults.forEach(result -> enqueueForTellNextWithRetry(ctx, partition, result, 0));
                    } finally {
                        if (!data.isEmpty()) {
                            scheduleTickMsg(ctx, deduplicationId, data);
                        }
                    }
                },
                () -> log.trace("[{}][{}][{}] Ignore msg from entity that belong to invalid partition!", ctx.getSelfId(), tpi.getFullTopicName(), deduplicationId)
        );
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data, long delayMs) {
        if (!data.isTickScheduled()) {
            scheduleTickMsg(ctx, deduplicationId, delayMs);
            data.setTickScheduled(true);
        }
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data) {
        scheduleTickMsg(ctx, deduplicationId, data, deduplicationInterval + 1);
    }

    private Optional<TbPair<Long, Long>> findValidPack(List<TbMsg> msgs, long deduplicationTimeoutMs) {
        Optional<TbMsg> min = msgs.stream().min(Comparator.comparing(TbMsg::getMetaDataTs));
        return min.map(minTsMsg -> {
            long packStartTs = minTsMsg.getMetaDataTs();
            long packEndTs = packStartTs + deduplicationInterval;
            if (packEndTs <= deduplicationTimeoutMs) {
                return new TbPair<>(packStartTs, packEndTs);
            }
            return null;
        });
    }

    private void enqueueForTellNextWithRetry(TbContext ctx, Integer partition, TbPair<TbMsg, List<TbMsg>> result, int retryAttempt) {
        TbMsg outMsg = result.getFirst();
        List<TbMsg> msgsToRemoveFromCache = result.getSecond();
        if (config.getMaxRetries() > retryAttempt) {
            ctx.enqueueForTellNext(outMsg, TbRelationTypes.SUCCESS,
                    () -> {
                        log.trace("[{}][{}][{}] Successfully enqueue deduplication result message!", ctx.getSelfId(), outMsg.getOriginator(), retryAttempt);
                        ctx.getRuleNodeCacheService().removeTbMsgList(outMsg.getOriginator().toString(), partition, msgsToRemoveFromCache);
                    },
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), outMsg.getOriginator(), retryAttempt, throwable);
                        ctx.schedule(() -> enqueueForTellNextWithRetry(ctx, partition, result, retryAttempt + 1), TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                    });
        } else {
            log.trace("[{}][{}] Removing deduplication messages pack due to max enqueue retry attempts exhausted!", ctx.getSelfId(), outMsg.getOriginator());
            ctx.getRuleNodeCacheService().removeTbMsgList(outMsg.getOriginator().toString(), partition, msgsToRemoveFromCache);
        }
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, long delayMs) {
        ctx.tellSelf(ctx.newMsg(null, TB_MSG_DEDUPLICATION_TIMEOUT_MSG, deduplicationId, EMPTY_META_DATA, EMPTY_DATA), delayMs);
    }

    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData().getData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

    private TbMsgMetaData getMetadata(long packStartTs) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(packStartTs));
        return metaData;
    }

}
