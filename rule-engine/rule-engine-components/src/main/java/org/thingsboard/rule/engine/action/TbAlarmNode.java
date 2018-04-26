/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutorService;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "alarm", relationTypes = {"Created", "Updated", "Cleared", "False"},
        configClazz = TbAlarmNodeConfiguration.class,
        nodeDescription = "Create/Update/Clear Alarm",
        nodeDetails = "isAlarm - JS function that verifies if Alarm should be CREATED for incoming message.\n" +
                "isCleared - JS function that verifies if Alarm should be CLEARED for incoming message.\n" +
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                "Node output:\n" +
                "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'matadata' will contains one of those properties 'isNewAlarm/isExistingAlarm/isClearedAlarm' " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>" +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAlarmConfig")

public class TbAlarmNode implements TbNode {

    static final String IS_NEW_ALARM = "isNewAlarm";
    static final String IS_EXISTING_ALARM = "isExistingAlarm";
    static final String IS_CLEARED_ALARM = "isClearedAlarm";

    private final ObjectMapper mapper = new ObjectMapper();

    private TbAlarmNodeConfiguration config;
    private ScriptEngine createJsEngine;
    private ScriptEngine clearJsEngine;
    private ScriptEngine buildDetailsJsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAlarmNodeConfiguration.class);
        this.createJsEngine = ctx.createJsScriptEngine(config.getCreateConditionJs(), "isAlarm");
        this.clearJsEngine = ctx.createJsScriptEngine(config.getClearConditionJs(), "isCleared");
        this.buildDetailsJsEngine = ctx.createJsScriptEngine(config.getAlarmDetailsBuildJs(), "Details");
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ListeningExecutor jsExecutor = ctx.getJsExecutor();

        ListenableFuture<Boolean> shouldCreate = jsExecutor.executeAsync(() -> createJsEngine.executeFilter(msg));
        ListenableFuture<AlarmResult> transform = Futures.transform(shouldCreate, (AsyncFunction<Boolean, AlarmResult>) create -> {
            if (create) {
                return createOrUpdate(ctx, msg);
            } else {
                return checkForClearIfExist(ctx, msg);
            }
        }, ctx.getDbCallbackExecutor());

        withCallback(transform,
                alarmResult -> {
                    if (alarmResult.alarm == null) {
                        ctx.tellNext(msg, "False");
                    } else if (alarmResult.isCreated) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Created");
                    } else if (alarmResult.isUpdated) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Updated");
                    } else if (alarmResult.isCleared) {
                        ctx.tellNext(toAlarmMsg(ctx, alarmResult, msg), "Cleared");
                    }
                },
                t -> ctx.tellError(msg, t));

    }

    private ListenableFuture<AlarmResult> createOrUpdate(TbContext ctx, TbMsg msg) {
        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), config.getAlarmType());
        return Futures.transform(latest, (AsyncFunction<Alarm, AlarmResult>) a -> {
            if (a == null || a.getStatus().isCleared()) {
                return createNewAlarm(ctx, msg);
            } else {
                return updateAlarm(ctx, msg, a);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<AlarmResult> checkForClearIfExist(TbContext ctx, TbMsg msg) {
        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), config.getAlarmType());
        return Futures.transform(latest, (AsyncFunction<Alarm, AlarmResult>) a -> {
            if (a != null && !a.getStatus().isCleared()) {
                return clearAlarm(ctx, msg, a);
            }
            return Futures.immediateFuture(new AlarmResult(false, false, false, null));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<AlarmResult> createNewAlarm(TbContext ctx, TbMsg msg) {
        ListenableFuture<Alarm> asyncAlarm = Futures.transform(buildAlarmDetails(ctx, msg),
                (Function<JsonNode, Alarm>) details -> buildAlarm(msg, details, ctx.getTenantId()));
        ListenableFuture<Alarm> asyncCreated = Futures.transform(asyncAlarm,
                (Function<Alarm, Alarm>) alarm -> ctx.getAlarmService().createOrUpdateAlarm(alarm), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, (Function<Alarm, AlarmResult>) alarm -> new AlarmResult(true, false, false, alarm));
    }

    private ListenableFuture<AlarmResult> updateAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ListenableFuture<Alarm> asyncUpdated = Futures.transform(buildAlarmDetails(ctx, msg), (Function<JsonNode, Alarm>) details -> {
            alarm.setSeverity(config.getSeverity());
            alarm.setPropagate(config.isPropagate());
            alarm.setDetails(details);
            alarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().createOrUpdateAlarm(alarm);
        }, ctx.getDbCallbackExecutor());

        return Futures.transform(asyncUpdated, (Function<Alarm, AlarmResult>) a -> new AlarmResult(false, true, false, a));
    }

    private ListenableFuture<AlarmResult> clearAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ListenableFuture<Boolean> shouldClear = ctx.getJsExecutor().executeAsync(() -> clearJsEngine.executeFilter(msg));
        return Futures.transform(shouldClear, (AsyncFunction<Boolean, AlarmResult>) clear -> {
            if (clear) {
                ListenableFuture<Boolean> clearFuture = ctx.getAlarmService().clearAlarm(alarm.getId(), System.currentTimeMillis());
                return Futures.transform(clearFuture, (Function<Boolean, AlarmResult>) cleared -> {
                    alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
                    return new AlarmResult(false, false, true, alarm);
                });
            }
            return Futures.immediateFuture(new AlarmResult(false, false, false, null));
        });
    }

    private Alarm buildAlarm(TbMsg msg, JsonNode details, TenantId tenantId) {
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(config.getSeverity())
                .propagate(config.isPropagate())
                .type(config.getAlarmType())
                //todo-vp: alarm date should be taken from Message or current Time should be used?
//                .startTs(System.currentTimeMillis())
//                .endTs(System.currentTimeMillis())
                .details(details)
                .build();
    }

    private ListenableFuture<JsonNode> buildAlarmDetails(TbContext ctx, TbMsg msg) {
        return ctx.getJsExecutor().executeAsync(() -> buildDetailsJsEngine.executeJson(msg));
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
        return ctx.newMsg("ALARM", originalMsg.getOriginator(), metaData, data);
    }


    @Override
    public void destroy() {
        if (createJsEngine != null) {
            createJsEngine.destroy();
        }
        if (clearJsEngine != null) {
            clearJsEngine.destroy();
        }
        if (buildDetailsJsEngine != null) {
            buildDetailsJsEngine.destroy();
        }
    }

    private static class AlarmResult {
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
