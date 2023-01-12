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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private TbMsgDeduplicationNodeConfiguration config;

    private Map<EntityId, List<TbMsgDeduplicationState>> deduplicationStateMap;
    private long deduplicationInterval;
    private long lastScheduledTs;
    private DeduplicationId deduplicationId;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.deduplicationId = config.getId();
        this.deduplicationStateMap = new HashMap<>();
        scheduleTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (TB_MSG_DEDUPLICATION_TIMEOUT_MSG.equals(msg.getType())) {
            switch (deduplicationId) {
                case ORIGINATOR:
                    List<DeduplicationData> deduplicationDataList = getDeduplicationDataList();
                    if (!deduplicationDataList.isEmpty()) {
                        deduplicationDataList.forEach(deduplicationData -> {
                            EntityId entityId = deduplicationData.getEntityId();
                            List<DeduplicationPack> deduplicationPacks = deduplicationData.getDeduplicationPacks();
                            deduplicationPacks.forEach(pack ->
                                    ctx.enqueueForTellNext(getOutputMsg(pack), TbRelationTypes.SUCCESS,
                                            () -> deduplicationStateMap.computeIfPresent(entityId, (key, states) -> {
                                                pack.getStates().forEach(states::remove);
                                                return states;
                                            }),
                                            throwable -> {
                                                log.trace("[{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), entityId, throwable);
                                                deduplicationStateMap.computeIfPresent(entityId, (key, states) -> {
                                                    List<TbMsgDeduplicationState> statesToRemove = new ArrayList<>();
                                                    states.forEach(state -> {
                                                        if (pack.getStates().contains(state)) {
                                                            int retries = state.getRetries();
                                                            if (retries < config.getMaxRetries()) {
                                                                state.incrementRetries();
                                                            } else {
                                                                statesToRemove.add(state);
                                                            }
                                                        }
                                                    });
                                                    states.removeAll(statesToRemove);
                                                    return states;
                                                });
                                            }));
                        });
                    }
                    break;
                case CUSTOMER:
                    // todo implement
                    break;
                default:
                    // todo implement
                    break;
            }
            scheduleTickMsg(ctx);
        } else {
            EntityId deduplicationStateId = getDeduplicationStateId(ctx, msg);
            List<TbMsgDeduplicationState> deduplicationStateList = deduplicationStateMap.computeIfAbsent(deduplicationStateId, k -> new ArrayList<>());
            if (deduplicationStateList.size() < config.getMaxPendingMsgs()) {
                log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), deduplicationStateId, msg.getId(), msg.getMetaDataTs());
                deduplicationStateList.add(new TbMsgDeduplicationState(msg));
                ctx.ack(msg);
            } else {
                log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), deduplicationStateId);
                ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + deduplicationStateId + "]"));
            }
        }
    }

    private EntityId getDeduplicationStateId(TbContext ctx, TbMsg msg) {
        switch (deduplicationId) {
            case ORIGINATOR:
                return msg.getOriginator();
            case TENANT:
                return ctx.getTenantId();
            case CUSTOMER:
                return msg.getCustomerId();
            default:
                throw new IllegalStateException("Unsupported deduplicationId: " + deduplicationId);
        }
    }

    @Override
    public void destroy() {
        deduplicationStateMap.clear();
    }

    private List<DeduplicationData> getDeduplicationDataList() {
        if (deduplicationStateMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeduplicationData> result = new ArrayList<>();
        long deduplicationTimeoutMs = System.currentTimeMillis();
        deduplicationStateMap.forEach((entityId, tbMsgDeduplicationStates) -> {
            if (!tbMsgDeduplicationStates.isEmpty()) {
                DeduplicationData deduplicationData = new DeduplicationData(entityId);
                TbMsgDeduplicationState firstDeduplicationState = getFirstDeduplicationState(tbMsgDeduplicationStates, null);
                TbMsgDeduplicationState lastStateInPack = firstDeduplicationState;
                long deduplicationStartTs = firstDeduplicationState.getTs();
                long deduplicationEndTs = deduplicationStartTs + deduplicationInterval;
                boolean hasNextDeduplicationPack = deduplicationEndTs < deduplicationTimeoutMs;
                while (hasNextDeduplicationPack) {
                    long finalDeduplicationStartTs = deduplicationStartTs;
                    List<TbMsgDeduplicationState> statesPack = new ArrayList<>();
                    for (TbMsgDeduplicationState state : tbMsgDeduplicationStates) {
                        if (state.getTs() >= finalDeduplicationStartTs && state.getTs() < deduplicationEndTs) {
                            statesPack.add(state);
                            if (state.getTs() > lastStateInPack.getTs()) {
                                lastStateInPack = state;
                            }
                        }
                    }
                    deduplicationData.addDeduplicationPack(statesPack, statesPack.indexOf(firstDeduplicationState), statesPack.indexOf(lastStateInPack));
                    if (deduplicationData.getStatesCount() == tbMsgDeduplicationStates.size()) {
                        hasNextDeduplicationPack = false;
                    } else {
                        firstDeduplicationState = getFirstDeduplicationState(tbMsgDeduplicationStates, deduplicationEndTs);
                        if (firstDeduplicationState == null) {
                            hasNextDeduplicationPack = false;
                        } else {
                            deduplicationStartTs = firstDeduplicationState.getTs();
                            deduplicationEndTs = deduplicationStartTs + deduplicationInterval;
                            hasNextDeduplicationPack = deduplicationEndTs < deduplicationTimeoutMs;
                        }
                    }
                }
                result.add(deduplicationData);
            }
        });
        return result;
    }

    private TbMsgDeduplicationState getFirstDeduplicationState(List<TbMsgDeduplicationState> tbMsgDeduplicationStates, Long previousPackEndTs) {
        if (previousPackEndTs != null) {
            tbMsgDeduplicationStates = tbMsgDeduplicationStates.stream().filter(state -> state.getTs() >= previousPackEndTs).collect(Collectors.toList());
        }
        TbMsgDeduplicationState first = null;
        for (TbMsgDeduplicationState state : tbMsgDeduplicationStates) {
            if (first == null || state.getTs() < first.getTs()) {
                first = state;
            }
        }
        return first;
    }

    private TbMsg getOutputMsg(DeduplicationPack pack) {
        switch (config.getStrategy()) {
            case FIRST:
                return pack.getFirst().getTbMsg();
            case LAST:
                return pack.getLast().getTbMsg();
            default:
                EntityId originator = pack.getStates().get(0).getTbMsg().getOriginator();
                String data = getMergedData(pack.getStates().stream()
                        .map(TbMsgDeduplicationState::getTbMsg)
                        .collect(Collectors.toList()));
                return TbMsg.newMsg(config.getQueueName(), config.getOutMsgType(), originator, getMetadata(), data);
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
