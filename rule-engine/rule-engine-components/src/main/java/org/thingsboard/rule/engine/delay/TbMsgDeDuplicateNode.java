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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RuleNode(
        type = ComponentType.ACTION,
        name = "de-duplicate",
        configClazz = TbMsgDeDuplicateNodeConfiguration.class,
        nodeDescription = "De-duplicate messages for a configurable period based on a specified de-duplication strategy.",
        nodeDetails = "Rule node allows you to select one of the following strategy to de-duplicate messages: <br></br>" +
                "<b>FIRST</b> - return first message that arrived during de-duplication period.<br></br>" +
                "<b>LAST</b> - return last message that arrived during de-duplication period.<br></br>" +
                "<b>ALL</b> - return all messages as a single JSON array message. Where each element represents object with <b>msg</b> and <b>metadata</b> inner properties.<br></br>" +
                "By default rule node <b>De-duplicate messages by message originator</b>, however, there is an option to de-duplicate messages independently from the incoming message's originator. " +
                "In case of the <b>De-duplicate strategy</b> set to <b>ALL</b> and <b>De-duplicate messages by message originator</b> checkbox set to <b>false</b>, you must configure the <b>Queue Name</b> and <b>Out Message Type</b> for the output array message. " +
                "Also in this case the output message originator will be set to the current tenant id.<br></br>",
        icon = "pause",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDeDuplicateConfig"
)
@Slf4j
public class TbMsgDeDuplicateNode implements TbNode {

    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeDuplicateNodeMsg";

    private TbMsgDeDuplicateNodeConfiguration config;

    private Map<String, List<TbMsg>> deDuplicationMap;
    private long delay;
    private long lastScheduledTs;
    private boolean deDuplicateByOriginator;
    private UUID nextTickId;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeDuplicateNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getDelay());
        this.deDuplicateByOriginator = config.isDeDuplicateByOriginator();
        this.deDuplicationMap = new HashMap<>();
        scheduleTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (TB_MSG_DEDUPLICATION_TIMEOUT_MSG.equals(msg.getType())) {
            if (msg.getId().equals(nextTickId)) {
                long deDuplicationTimeoutMs = System.currentTimeMillis();
                List<String> keysToRemove = deDuplicationMap
                        .keySet()
                        .stream()
                        .filter(key -> key.startsWith(String.valueOf(lastScheduledTs)) || Long.parseLong(key.split("_")[0]) < deDuplicationTimeoutMs)
                        .collect(Collectors.toList());
                scheduleTickMsg(ctx);
                keysToRemove.forEach(key -> {
                    List<TbMsg> msgList = deDuplicationMap.get(key);
                    TbMsg outMsg;
                    switch (config.getStrategy()) {
                        case FIRST:
                            outMsg = msgList.get(0);
                            break;
                        case LAST:
                            outMsg = msgList.get(msgList.size() - 1);
                            break;
                        default:
                            EntityId originator;
                            String queueName;
                            String outMsgType;
                            if (deDuplicateByOriginator) {
                                String[] keyElements = key.split("_");
                                originator = EntityIdFactory.getByTypeAndId(keyElements[1], keyElements[2]);
                                queueName = msgList.get(0).getQueueName();
                                outMsgType = msgList.get(0).getType();
                            } else {
                                originator = ctx.getTenantId();
                                queueName = config.getQueueName();
                                outMsgType = config.getOutMsgType();
                            }
                            outMsg = TbMsg.newMsg(queueName, outMsgType, originator, getMetadata(), getMergedData(msgList));
                            break;
                    }
                    ctx.enqueueForTellNext(outMsg, TbRelationTypes.SUCCESS,
                            () -> deDuplicationMap.remove(key),
                            throwable -> log.trace("Failed to enqueue de-duplication output message due to: ", throwable));
                });
            } else {
                log.trace("[{}][{}] Received outdated tick msg: {}", ctx.getTenantId(), ctx.getSelfId(), msg.getId());
            }
        } else {
            String key = String.format("%d_%s", lastScheduledTs, getDeDuplicationId(ctx, msg));
            List<TbMsg> deDuplicateMsgList = deDuplicationMap.computeIfAbsent(key, k -> new ArrayList<>());
            if (deDuplicateMsgList.size() < config.getMaxPendingMsgs()) {
                deDuplicateMsgList.add(msg);
                ctx.ack(msg);
            } else {
                ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached for key: " + key));
            }
        }
    }

    private String getDeDuplicationId(TbContext ctx, TbMsg msg) {
        return deDuplicateByOriginator ?
                msg.getOriginator().getEntityType().name() + "_" + msg.getOriginator().getId() :
                EntityType.RULE_NODE.name() + "_" + ctx.getSelfId().getId();
    }

    private void scheduleTickMsg(TbContext ctx) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TB_MSG_DEDUPLICATION_TIMEOUT_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
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
