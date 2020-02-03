/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "清除告警", relationTypes = {"Cleared", "False"},
        configClazz = TbClearAlarmNodeConfiguration.class,
        nodeDescription = "清除告警",
        nodeDetails =
                "详情 - 这是一个根据传入消息创建JSON对象的JS函数。JSON对象将被添加到Alarm.details字段。\n" +
                        "节点输出：\n" +
                        "如果告警未被清除，则返回原始消息；否则返回类型为'ALARM'的新消息。'msg'属性里面的Alarm对象和'matadata'将包含'isClearedAlarm'属性。" +
                        "可以通过<code>msg</code>属性来访问消息的有效负载，例如<code>'temperature = ' + msg.temperature ;</code>。" +
                        "可以通过<code>metadata</code>属性来访问消息的元数据，例如<code>'name = ' + metadata.customerName;</code>。",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeClearAlarmConfig",
        icon = "notifications_off"
)
public class TbClearAlarmNode extends TbAbstractAlarmNode<TbClearAlarmNodeConfiguration> {

    @Override
    protected TbClearAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbClearAlarmNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<AlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg.getMetaData());
        ListenableFuture<Alarm> latest = ctx.getAlarmService().findLatestByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        return Futures.transformAsync(latest, a -> {
            if (a != null && !a.getStatus().isCleared()) {
                return clearAlarm(ctx, msg, a);
            }
            return Futures.immediateFuture(new AlarmResult(false, false, false, null));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<AlarmResult> clearAlarm(TbContext ctx, TbMsg msg, Alarm alarm) {
        ctx.logJsEvalRequest();
        ListenableFuture<JsonNode> asyncDetails = buildAlarmDetails(ctx, msg, alarm.getDetails());
        return Futures.transformAsync(asyncDetails, details -> {
            ctx.logJsEvalResponse();
            ListenableFuture<Boolean> clearFuture = ctx.getAlarmService().clearAlarm(ctx.getTenantId(), alarm.getId(), details, System.currentTimeMillis());
            return Futures.transformAsync(clearFuture, cleared -> {
                ListenableFuture<Alarm> savedAlarmFuture = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());
                return Futures.transformAsync(savedAlarmFuture, savedAlarm -> {
                    if (cleared && savedAlarm != null) {
                        alarm.setDetails(savedAlarm.getDetails());
                        alarm.setEndTs(savedAlarm.getEndTs());
                        alarm.setClearTs(savedAlarm.getClearTs());
                    }
                    alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
                    return Futures.immediateFuture(new AlarmResult(false, false, true, alarm));
                });
            });
        }, ctx.getDbCallbackExecutor());
    }
}
