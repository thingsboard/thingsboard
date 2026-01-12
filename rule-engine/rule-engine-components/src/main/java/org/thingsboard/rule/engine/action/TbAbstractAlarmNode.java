/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.thingsboard.common.util.DonAsynchron.withCallback;


@Slf4j
public abstract class TbAbstractAlarmNode<C extends TbAbstractAlarmNodeConfiguration> implements TbNode {

    static final String PREV_ALARM_DETAILS = "prevAlarmDetails";

    protected C config;
    private ScriptEngine scriptEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadAlarmNodeConfig(configuration);
        scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getAlarmDetailsBuildTbel() : config.getAlarmDetailsBuildJs());
    }

    protected abstract C loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processAlarm(ctx, msg),
                alarmResult -> {
                    if (alarmResult.alarm == null) {
                        ctx.tellNext(msg, TbNodeConnectionType.FALSE);
                    } else if (alarmResult.isCreated) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ENTITY_CREATED, "Created");
                    } else if (alarmResult.isUpdated || alarmResult.isSeverityUpdated) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ENTITY_UPDATED, "Updated");
                    } else if (alarmResult.isCleared) {
                        tellNext(ctx, msg, alarmResult, TbMsgType.ALARM_CLEAR, "Cleared");
                    } else {
                        ctx.tellSuccess(msg);
                    }
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg);

    protected ListenableFuture<JsonNode> buildAlarmDetails(TbMsg msg, JsonNode previousDetails) {
        try {
            TbMsg dummyMsg = msg;
            if (previousDetails != null) {
                TbMsgMetaData metaData = msg.getMetaData().copy();
                metaData.putValue(PREV_ALARM_DETAILS, JacksonUtil.toString(previousDetails));
                dummyMsg = msg.transform()
                        .metaData(metaData)
                        .build();
            }
            return scriptEngine.executeJsonAsync(dummyMsg);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public static TbMsg toAlarmMsg(TbContext ctx, TbAlarmResult alarmResult, TbMsg originalMsg) {
        JsonNode jsonNodes = JacksonUtil.valueToTree(alarmResult.alarm);
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        if (alarmResult.isCreated) {
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated || alarmResult.isSeverityUpdated) {
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isCleared) {
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        return ctx.transformMsg(originalMsg, TbMsgType.ALARM, originalMsg.getOriginator(), metaData, data);
    }

    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }

    private void tellNext(TbContext ctx, TbMsg msg, TbAlarmResult alarmResult, TbMsgType actionMsgType, String alarmAction) {
        ctx.enqueue(ctx.alarmActionMsg(alarmResult.alarm, ctx.getSelfId(), actionMsgType),
                () -> ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), alarmAction),
                throwable -> ctx.tellFailure(toAlarmMsg(ctx, alarmResult, msg), throwable));
    }
}
