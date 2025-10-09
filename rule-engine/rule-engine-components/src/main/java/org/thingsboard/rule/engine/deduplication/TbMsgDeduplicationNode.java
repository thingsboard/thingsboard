/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "deduplication",
        configClazz = TbMsgDeduplicationNodeConfiguration.class,
        version = 1,
        hasQueueName = true,
        nodeDescription = "Deduplicate messages within the same originator entity for a configurable period " +
                "based on a specified deduplication strategy.",
        nodeDetails = "Deduplication strategies: <ul><li><strong>FIRST</strong> - return first message that arrived during deduplication period.</li>" +
                "<li><strong>LAST</strong> - return last message that arrived during deduplication period.</li>" +
                "<li><strong>ALL</strong> - return all messages as a single JSON array message. " +
                "Where each element represents object with <strong><i>msg</i></strong> and <strong><i>metadata</i></strong> inner properties.</li></ul>",
        icon = "content_copy",
        configDirective = "tbTransformationNodeDeduplicationConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/deduplication/"
)
public class TbMsgDeduplicationNode implements TbNode {

    public static final long TB_MSG_DEDUPLICATION_RETRY_DELAY = 10L;

    private TbMsgDeduplicationNodeConfiguration config;

    private final Map<EntityId, DeduplicationData> deduplicationMap;
    private long deduplicationInterval;
    private String queueName;

    public TbMsgDeduplicationNode() {
        this.deduplicationMap = new HashMap<>();
    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.queueName = ctx.getQueueName();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.isTypeOf(TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG)) {
            processDeduplication(ctx, msg.getOriginator());
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    public void destroy() {
        deduplicationMap.clear();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has(QUEUE_NAME)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(QUEUE_NAME);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    private void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        DeduplicationData deduplicationMsgs = deduplicationMap.computeIfAbsent(id, k -> new DeduplicationData());
        if (deduplicationMsgs.size() < config.getMaxPendingMsgs()) {
            log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), id, msg.getId(), msg.getMetaDataTs());
            deduplicationMsgs.add(msg);
            ctx.ack(msg);
            scheduleTickMsg(ctx, id, deduplicationMsgs);
        } else {
            log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), id);
            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + id + "]"));
        }
    }

    private void processDeduplication(TbContext ctx, EntityId deduplicationId) {
        DeduplicationData data = deduplicationMap.get(deduplicationId);
        if (data == null) {
            return;
        }
        data.setTickScheduled(false);
        if (data.isEmpty()) {
            return;
        }
        long deduplicationTimeoutMs = System.currentTimeMillis();
        try {
            List<TbMsg> deduplicationResults = new ArrayList<>();
            List<TbMsg> msgList = data.getMsgList();
            Optional<TbPair<Long, Long>> packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
            while (packBoundsOpt.isPresent()) {
                TbPair<Long, Long> packBounds = packBoundsOpt.get();
                if (DeduplicationStrategy.ALL.equals(config.getStrategy())) {
                    List<TbMsg> pack = new ArrayList<>();
                    for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                        TbMsg msg = iterator.next();
                        long msgTs = msg.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            pack.add(msg);
                            iterator.remove();
                        }
                    }
                    deduplicationResults.add(TbMsg.newMsg()
                            .queueName(queueName)
                            .type(config.getOutMsgType())
                            .originator(deduplicationId)
                            .copyMetaData(getMetadata())
                            .data(getMergedData(pack))
                            .build());
                } else {
                    TbMsg resultMsg = null;
                    boolean searchMin = DeduplicationStrategy.FIRST.equals(config.getStrategy());
                    for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                        TbMsg msg = iterator.next();
                        long msgTs = msg.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            iterator.remove();
                            if (resultMsg == null
                                    || (searchMin && msg.getMetaDataTs() < resultMsg.getMetaDataTs())
                                    || (!searchMin && msg.getMetaDataTs() > resultMsg.getMetaDataTs())) {
                                resultMsg = msg;
                            }
                        }
                    }
                    if (resultMsg != null) {
                        String queueName1 = queueName != null ? queueName : resultMsg.getQueueName();
                        deduplicationResults.add(TbMsg.newMsg()
                                .queueName(queueName1)
                                .type(resultMsg.getType())
                                .originator(resultMsg.getOriginator())
                                .customerId(resultMsg.getCustomerId())
                                .copyMetaData(resultMsg.getMetaData())
                                .data(resultMsg.getData())
                                .build());
                    }
                }
                packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
            }
            deduplicationResults.forEach(outMsg -> enqueueForTellNextWithRetry(ctx, outMsg, 0));
        } finally {
            if (!data.isEmpty()) {
                scheduleTickMsg(ctx, deduplicationId, data);
            }
        }
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data) {
        if (!data.isTickScheduled()) {
            scheduleTickMsg(ctx, deduplicationId);
            data.setTickScheduled(true);
        }
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

    private void enqueueForTellNextWithRetry(TbContext ctx, TbMsg msg, int retryAttempt) {
        if (retryAttempt <= config.getMaxRetries()) {
            ctx.enqueueForTellNext(msg, TbNodeConnectionType.SUCCESS,
                    () -> log.trace("[{}][{}][{}] Successfully enqueue deduplication result message!", ctx.getSelfId(), msg.getOriginator(), retryAttempt),
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), msg.getOriginator(), retryAttempt, throwable);
                        if (retryAttempt < config.getMaxRetries()) {
                            ctx.schedule(() -> enqueueForTellNextWithRetry(ctx, msg, retryAttempt + 1), TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                        } else {
                            log.trace("[{}][{}] Max retries [{}] exhausted. Dropping deduplication result message [{}]",
                                    ctx.getSelfId(), msg.getOriginator(), config.getMaxRetries(), msg.getId());
                        }
                    });
        }
    }

    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId) {
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG, deduplicationId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING), deduplicationInterval + 1);
    }

    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.newArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData().getData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

    private TbMsgMetaData getMetadata() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return metaData;
    }

}
