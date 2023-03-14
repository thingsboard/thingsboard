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
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.cache.TbAbstractCacheBasedRuleNode;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class TbMsgDeduplicationNode extends TbAbstractCacheBasedRuleNode<TbMsgDeduplicationNodeConfiguration, DeduplicationData> {

    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeduplicationNodeMsg";
    private static final String DEDUPLICATION_IDS_CACHE_KEY = "deduplication_ids";
    private static final int TB_MSG_DEDUPLICATION_RETRY_DELAY = 10;

    private long deduplicationInterval;

    @Override
    protected TbMsgDeduplicationNodeConfiguration loadRuleNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgDeduplicationNodeConfiguration config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        return config;
    }

    @Override
    protected void processGetValuesFromCacheAndSchedule(TbContext ctx, EntityId id, Integer partition) {
        List<TbMsg> tbMsgs = new ArrayList<>(ctx.getRuleNodeCacheService().getTbMsgs(id, partition));
        DeduplicationData deduplicationData = entityIdValuesMap.computeIfAbsent(id, k -> new DeduplicationData());
        if (deduplicationData.isEmpty() && tbMsgs.isEmpty()) {
            return;
        }
        deduplicationData.addAll(tbMsgs);
        scheduleTickMsg(ctx, id, deduplicationData, 0);
    }

    @Override
    protected void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getEntityTopicPartition(id);
        if (tpi.isMyPartition()) {
            Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
            Set<EntityId> entityIds = partitionsEntityIdsMap.computeIfAbsent(partition, k -> new HashSet<>());
            boolean entityIdAdded = entityIds.add(id);
            DeduplicationData deduplicationMsgs = entityIdValuesMap.computeIfAbsent(id, k -> new DeduplicationData());
            if (deduplicationMsgs.size() < config.getMaxPendingMsgs()) {
                log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), id, msg.getId(), msg.getMetaDataTs());
                if (entityIdAdded) {
                    ctx.getRuleNodeCacheService().add(getEntityIdsCacheKey(), id);
                }
                deduplicationMsgs.add(msg);
                ctx.getRuleNodeCacheService().add(id, partition, msg);
                ctx.ack(msg);
                scheduleTickMsg(ctx, id, deduplicationMsgs);
            } else {
                log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), id);
                ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + id + "]"));
            }
        } else {
            log.trace("[{}][{}][{}] Ignore msg from entity that doesn't belong to local partition!", ctx.getSelfId(), tpi.getFullTopicName(), id);
        }
    }

    @Override
    protected void processOnTickMsg(TbContext ctx, TbMsg msg) {
        EntityId deduplicationId = msg.getOriginator();
        TopicPartitionInfo tpi = ctx.getEntityTopicPartition(deduplicationId);
        if (!tpi.isMyPartition()) {
            return;
        }
        Integer partition = tpi.getPartition().orElse(DEFAULT_PARTITION);
        DeduplicationData data = entityIdValuesMap.get(deduplicationId);
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
                        TbMsg next = iterator.next();
                        long msgTs = next.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            pack.add(next);
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
                        TbMsg next = iterator.next();
                        long msgTs = next.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            pack.add(next);
                            iterator.remove();
                            if (resultMsg == null
                                    || (searchMin && next.getMetaDataTs() < resultMsg.getMetaDataTs())
                                    || (!searchMin && next.getMetaDataTs() > resultMsg.getMetaDataTs())) {
                                resultMsg = next;
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
    }

    @Override
    protected String getTickMsgType() {
        return TB_MSG_DEDUPLICATION_TIMEOUT_MSG;
    }

    @Override
    protected String getEntityIdsCacheKey() {
        return DEDUPLICATION_IDS_CACHE_KEY;
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data, long delayMs) {
        if (!data.isTickScheduled()) {
            log.trace("[{}] Schedule deduplication tick msg for entity: [{}], delay: [{}]", ctx.getSelfId(), deduplicationId, delayMs);
            scheduleTickMsg(ctx, deduplicationId, delayMs);
            data.setTickScheduled(true);
        }
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data) {
        scheduleTickMsg(ctx, deduplicationId, data, deduplicationInterval + 1);
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, long delayMs) {
        ctx.tellSelf(ctx.newMsg(null, getTickMsgType(), deduplicationId, EMPTY_META_DATA, EMPTY_DATA), delayMs);
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
                        ctx.getRuleNodeCacheService().removeTbMsgList(outMsg.getOriginator(), partition, msgsToRemoveFromCache);
                    },
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), outMsg.getOriginator(), retryAttempt, throwable);
                        ctx.schedule(() -> enqueueForTellNextWithRetry(ctx, partition, result, retryAttempt + 1), TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                    });
        } else {
            log.trace("[{}][{}] Removing deduplication messages pack due to max enqueue retry attempts exhausted!", ctx.getSelfId(), outMsg.getOriginator());
            ctx.getRuleNodeCacheService().removeTbMsgList(outMsg.getOriginator(), partition, msgsToRemoveFromCache);
        }
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
