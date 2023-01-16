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
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RuleNode(
        type = ComponentType.ACTION,
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
    private static final int TB_MSG_DEDUPLICATION_TIMEOUT = 5000;
    public static final int TB_MSG_DEDUPLICATION_RETRY_DELAY = 10;

    private TbMsgDeduplicationNodeConfiguration config;

    private Map<EntityId, List<TbMsg>> deduplicationMap;
    private long deduplicationInterval;
    private long lastScheduledTs;
    private DeduplicationId deduplicationId;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.deduplicationId = config.getId();
        this.deduplicationMap = new HashMap<>();
        scheduleTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (TB_MSG_DEDUPLICATION_TIMEOUT_MSG.equals(msg.getType())) {
            try {
                processDeduplication(ctx);
            } finally {
                scheduleTickMsg(ctx);
            }
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    public void destroy() {
        deduplicationMap.clear();
    }

    private void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = getDeduplicationId(ctx, msg);
        List<TbMsg> deduplicationMsgs = deduplicationMap.computeIfAbsent(id, k -> new ArrayList<>());
        if (deduplicationMsgs.size() < config.getMaxPendingMsgs()) {
            log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), id, msg.getId(), msg.getMetaDataTs());
            deduplicationMsgs.add(msg);
            ctx.ack(msg);
        } else {
            log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), id);
            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + id + "]"));
        }
    }

    private EntityId getDeduplicationId(TbContext ctx, TbMsg msg) {
        switch (deduplicationId) {
            case ORIGINATOR:
                return msg.getOriginator();
            case TENANT:
                return ctx.getTenantId();
            case CUSTOMER:
                return msg.getCustomerId();
            default:
                throw new IllegalStateException("Unsupported deduplication id: " + deduplicationId);
        }
    }

    private void processDeduplication(TbContext ctx) {
        if (!deduplicationMap.isEmpty()) {
            List<TbMsg> deduplicationResults = new ArrayList<>();
            long deduplicationTimeoutMs = System.currentTimeMillis();
            deduplicationMap.forEach((entityId, tbMsgs) -> {
                if (!tbMsgs.isEmpty()) {
                    TbMsg firstMsgInPack = getFirstPackMsg(tbMsgs);
                    long packStartTs = firstMsgInPack.getMetaDataTs();
                    long packEndTs = packStartTs + deduplicationInterval;
                    boolean hasNextPack = packEndTs < deduplicationTimeoutMs;
                    while (hasNextPack) {
                        TbMsg lastMsgInPack = firstMsgInPack;
                        List<TbMsg> pack = new ArrayList<>();
                        pack.add(firstMsgInPack);
                        tbMsgs.remove(firstMsgInPack);
                        firstMsgInPack = null;
                        for (TbMsg msg : tbMsgs) {
                            if (msg.getMetaDataTs() > packStartTs && msg.getMetaDataTs() < packEndTs) {
                                pack.add(msg);
                                if (msg.getMetaDataTs() > lastMsgInPack.getMetaDataTs()) {
                                    lastMsgInPack = msg;
                                }
                            } else {
                                if (firstMsgInPack == null || msg.getMetaDataTs() < firstMsgInPack.getMetaDataTs()) {
                                    firstMsgInPack = msg;
                                }
                            }
                        }
                        deduplicationResults.add(createOutMsg(entityId, pack, pack.indexOf(lastMsgInPack)));
                        tbMsgs.removeAll(pack);
                        if (firstMsgInPack == null) {
                            hasNextPack = false;
                        } else {
                            packStartTs = firstMsgInPack.getMetaDataTs();
                            packEndTs = packStartTs + deduplicationInterval;
                            hasNextPack = packEndTs < deduplicationTimeoutMs;
                        }
                    }
                }
            });
            deduplicationResults.forEach(outMsg -> enqueueForTellNextWithRetry(ctx, outMsg, 0));
        }
    }

    private TbMsg getFirstPackMsg(List<TbMsg> tbMsgs) {
        TbMsg firstMsg = null;
        for (TbMsg msg : tbMsgs) {
            if (firstMsg == null || msg.getMetaDataTs() < firstMsg.getMetaDataTs()) {
                firstMsg = msg;
            }
        }
        return firstMsg;
    }

    private TbMsg createOutMsg(EntityId originator, List<TbMsg> pack, int lastIndex) {
        switch (config.getStrategy()) {
            case FIRST:
                return pack.get(0);
            case LAST:
                return pack.get(lastIndex);
            default:
                return TbMsg.newMsg(
                        config.getQueueName(),
                        config.getOutMsgType(),
                        originator,
                        getMetadata(),
                        getMergedData(pack));
        }
    }

    private void enqueueForTellNextWithRetry(TbContext ctx, TbMsg msg, int retryAttempt) {
        if (config.getMaxRetries() > retryAttempt) {
            ctx.enqueueForTellNext(msg, TbRelationTypes.SUCCESS,
                    () -> {
                        log.trace("[{}][{}][{}] Successfully enqueue deduplication result message!", ctx.getSelfId(), msg.getOriginator(), retryAttempt);
                    },
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), msg.getOriginator(), retryAttempt, throwable);
                        ctx.schedule(() -> {
                            enqueueForTellNextWithRetry(ctx, msg, retryAttempt + 1);
                        }, TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                    });
        }
    }

    private void scheduleTickMsg(TbContext ctx) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs += TB_MSG_DEDUPLICATION_TIMEOUT;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_DEDUPLICATION_TIMEOUT_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        ctx.tellSelf(tickMsg, curDelay);
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

    private TbMsgMetaData getMetadata() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return metaData;
    }

}
