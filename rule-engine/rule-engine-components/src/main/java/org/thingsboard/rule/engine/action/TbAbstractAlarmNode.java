/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import static org.thingsboard.common.util.DonAsynchron.withCallback;


@Slf4j
public abstract class TbAbstractAlarmNode<C extends TbAbstractAlarmNodeConfiguration> implements TbNode {

    static final String PREV_ALARM_DETAILS = "prevAlarmDetails";

    static final String IS_NEW_ALARM = "isNewAlarm";
    static final String IS_EXISTING_ALARM = "isExistingAlarm";
    static final String IS_CLEARED_ALARM = "isClearedAlarm";

    private final ObjectMapper mapper = new ObjectMapper();

    protected C config;
    private ScriptEngine buildDetailsJsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadAlarmNodeConfig(configuration);
        this.buildDetailsJsEngine = ctx.createJsScriptEngine(config.getAlarmDetailsBuildJs());
    }

    protected abstract C loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processAlarm(ctx, msg),
                alarmResult -> {
                    if (alarmResult.alarm == null) {
                        ctx.tellNext(msg, "False");
                    } else if (alarmResult.isCreated) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Created");
                        ctx.sendTbMsgToRuleEngine(ctx.alarmCreatedMsg(alarmResult.alarm, ctx.getSelfId()));
                    } else if (alarmResult.isUpdated) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Updated");
                    } else if (alarmResult.isCleared) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Cleared");
                    }
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<AlarmResult> processAlarm(TbContext ctx, TbMsg msg);

    protected ListenableFuture<JsonNode> buildAlarmDetails(TbContext ctx, TbMsg msg, JsonNode previousDetails) {
        return ctx.getJsExecutor().executeAsync(() -> {
            TbMsg dummyMsg = msg;
            if (previousDetails != null) {
                TbMsgMetaData metaData = msg.getMetaData().copy();
                metaData.putValue(PREV_ALARM_DETAILS, mapper.writeValueAsString(previousDetails));
                dummyMsg = ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, msg.getData());
            }
            return buildDetailsJsEngine.executeJson(dummyMsg);
        });
    }

    private TbMsg toAlarmMsg(TbContext ctx, AlarmResult alarmResult, TbMsg originalMsg) {
        JsonNode jsonNodes = mapper.valueToTree(alarmResult.alarm);
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        if (alarmResult.isCreated) {
            metaData.putValue(IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated) {
            metaData.putValue(IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isCleared) {
            metaData.putValue(IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        return ctx.transformMsg(originalMsg, "ALARM", originalMsg.getOriginator(), metaData, data);
    }


    @Override
    public void destroy() {
        if (buildDetailsJsEngine != null) {
            buildDetailsJsEngine.destroy();
        }
    }

    protected static class AlarmResult {
        boolean isCreated;
        boolean isUpdated;
        boolean isCleared;
        Alarm alarm;

        AlarmResult(boolean isCreated, boolean isUpdated, boolean isCleared, Alarm alarm) {
            this.isCreated = isCreated;
            this.isUpdated = isUpdated;
            this.isCleared = isCleared;
            this.alarm = alarm;
        }
    }
}
