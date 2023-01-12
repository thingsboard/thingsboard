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
package org.thingsboard.rule.engine.deduplicate;

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

    private Map<EntityId, List<TbMsgDeDuplicateState>> deDuplicateStateMap;
    private long deDuplicationInterval;
    private long lastScheduledTs;
    private boolean deDuplicateByOriginator;
    private UUID nextTickId;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeDuplicateNodeConfiguration.class);
        this.deDuplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.deDuplicateByOriginator = config.isDeDuplicateByOriginator();
        this.deDuplicateStateMap = new HashMap<>();
        scheduleTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (TB_MSG_DEDUPLICATION_TIMEOUT_MSG.equals(msg.getType())) {
            if (msg.getId().equals(nextTickId)) {
                if (deDuplicateByOriginator) {
                    List<DeDuplicateData> deDuplicateDataList = getDeDuplicateDataList();
                    if (!deDuplicateDataList.isEmpty()) {
                        deDuplicateDataList.forEach(deDuplicateData -> {
                            EntityId entityId = deDuplicateData.getEntityId();
                            List<DeDuplicatePack> deDuplicateStates = deDuplicateData.getDeDuplicateStates();
                            deDuplicateStates.forEach(pack ->
                                    ctx.enqueueForTellNext(getOutputMsg(pack), TbRelationTypes.SUCCESS,
                                            () -> {
                                                deDuplicateStateMap.computeIfPresent(entityId, (key, states) -> {
                                                    pack.getStates().forEach(states::remove);
                                                    return states;
                                                });
                                            },
                                            throwable -> {
                                                log.trace("Failed to enqueue de-duplication output message due to: ", throwable);
                                                deDuplicateStateMap.computeIfPresent(entityId, (key, states) -> {
                                                    List<TbMsgDeDuplicateState> statesToRemove = new ArrayList<>();
                                                    states.forEach(state -> {
                                                        if (pack.getStates().contains(state)) {
                                                            int retries = state.getRetries();
                                                            if (config.getMaxRetries() <= retries) {
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
                } else {
                    // todo implement
                }
                scheduleTickMsg(ctx);
            } else {
                log.trace("[{}][{}] Received outdated tick msg: {}", ctx.getTenantId(), ctx.getSelfId(), msg.getId());
            }
        } else {
            List<TbMsgDeDuplicateState> deDuplicateStateList = deDuplicateStateMap.computeIfAbsent(msg.getOriginator(), k -> new ArrayList<>());
            if (deDuplicateStateList.size() < config.getMaxPendingMsgs()) {
                log.trace("[{}] Adding msg: [{}][{}] to the pending msgs map ...", msg.getOriginator(), msg.getId(), msg.getMetaDataTs());
                deDuplicateStateList.add(new TbMsgDeDuplicateState(msg));
                ctx.ack(msg);
            } else {
                log.trace("[{}] Max limit of pending messages reached for entity!", msg.getOriginator());
                ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached for entity: " + msg.getOriginator()));
            }
        }
    }

    @Override
    public void destroy() {
        deDuplicateStateMap.clear();
    }

    private List<DeDuplicateData> getDeDuplicateDataList() {
        if (deDuplicateStateMap.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<DeDuplicateData> result = new ArrayList<>();
            long deDuplicationTimeoutMs = System.currentTimeMillis();
            deDuplicateStateMap.forEach((entityId, tbMsgDeDuplicateStates) -> {
                if (!tbMsgDeDuplicateStates.isEmpty()) {
                    DeDuplicateData deDuplicateData = new DeDuplicateData(entityId);
                    TbMsgDeDuplicateState firstDeDuplicateState = getFirstDeDuplicateState(tbMsgDeDuplicateStates, null);
                    TbMsgDeDuplicateState lastStateInPack = firstDeDuplicateState;
                    long deDuplicationStartTs = firstDeDuplicateState.getTs();
                    long deDuplicationEndTs = deDuplicationStartTs + deDuplicationInterval;
                    boolean hasNextDeduplicationPack = deDuplicationEndTs < deDuplicationTimeoutMs;
                    while (hasNextDeduplicationPack) {
                        long finalDeDuplicationStartTs = deDuplicationStartTs;
                        List<TbMsgDeDuplicateState> statesPack = new ArrayList<>();
                        for (TbMsgDeDuplicateState state : tbMsgDeDuplicateStates) {
                            if (state.getTs() >= finalDeDuplicationStartTs && state.getTs() < deDuplicationEndTs) {
                                statesPack.add(state);
                                if (state.getTs() > lastStateInPack.getTs()) {
                                    lastStateInPack = state;
                                }
                            }
                        }
                        deDuplicateData.addDeDuplicatePack(statesPack, statesPack.indexOf(firstDeDuplicateState), statesPack.indexOf(lastStateInPack));
                        if (deDuplicateData.getStatesCount() == tbMsgDeDuplicateStates.size()) {
                            hasNextDeduplicationPack = false;
                        } else {
                            firstDeDuplicateState = getFirstDeDuplicateState(tbMsgDeDuplicateStates, deDuplicationEndTs);
                            if (firstDeDuplicateState == null) {
                                hasNextDeduplicationPack = false;
                            } else {
                                deDuplicationStartTs = firstDeDuplicateState.getTs();
                                deDuplicationEndTs = deDuplicationStartTs + deDuplicationInterval;
                                hasNextDeduplicationPack = deDuplicationEndTs < deDuplicationTimeoutMs;
                            }
                        }
                    }
                    result.add(deDuplicateData);
                }
            });
            return result;
        }
    }

    private TbMsgDeDuplicateState getFirstDeDuplicateState(List<TbMsgDeDuplicateState> tbMsgDeDuplicateStates, Long previousPackEndTs) {
        if (previousPackEndTs != null) {
            tbMsgDeDuplicateStates = tbMsgDeDuplicateStates.stream().filter(state -> state.getTs() >= previousPackEndTs).collect(Collectors.toList());
        }
        TbMsgDeDuplicateState first = null;
        for (TbMsgDeDuplicateState state : tbMsgDeDuplicateStates) {
            if (first == null || state.getTs() < first.getTs()) {
                first = state;
            }
        }
        return first;
    }

    private TbMsg getOutputMsg(DeDuplicatePack pack) {
        switch (config.getStrategy()) {
            case FIRST:
                return pack.getFirst().getTbMsg();
            case LAST:
                return pack.getLast().getTbMsg();
            default:
                EntityId originator = pack.getStates().get(0).getTbMsg().getOriginator();
                String queueName = pack.getStates().get(0).getTbMsg().getQueueName();
                String outMsgType = pack.getStates().get(0).getTbMsg().getType();
                String data = getMergedData(pack.getStates().stream()
                        .map(TbMsgDeDuplicateState::getTbMsg)
                        .collect(Collectors.toList()));
                return TbMsg.newMsg(queueName, outMsgType, originator, getMetadata(), data);
        }
    }

    private void scheduleTickMsg(TbContext ctx) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + deDuplicationInterval;
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
