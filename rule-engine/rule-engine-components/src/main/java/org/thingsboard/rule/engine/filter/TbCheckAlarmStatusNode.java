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
package org.thingsboard.rule.engine.filter;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Objects;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "alarm status filter",
        configClazz = TbCheckAlarmStatusNodeConfig.class,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Checks alarm status.",
        nodeDetails = "Checks the alarm status to match one of the specified statuses.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>.",
        configDirective = "tbFilterNodeCheckAlarmStatusConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/alarm-status-filter/"
)
public class TbCheckAlarmStatusNode implements TbNode {

    private TbCheckAlarmStatusNodeConfig config;

    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckAlarmStatusNodeConfig.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
            Objects.requireNonNull(alarm, "alarm is null");
            ListenableFuture<Alarm> latest = ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), alarm.getId());

            Futures.addCallback(latest, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Alarm result) {
                    if (result == null) {
                        ctx.tellFailure(msg, new TbNodeException("No such alarm found."));
                        return;
                    }
                    boolean isPresent = config.getAlarmStatusList().stream()
                            .anyMatch(alarmStatus -> result.getStatus() == alarmStatus);
                    ctx.tellNext(msg, isPresent ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
                }

                @Override
                public void onFailure(Throwable t) {
                    ctx.tellFailure(msg, t);
                }
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
                log.debug("[{}][{}] Failed to parse alarm: [{}] error [{}]", ctx.getTenantId(), ctx.getRuleChainName(), msg.getData(), e.getMessage());
            } else {
                log.error("[{}][{}] Failed to parse alarm: [{}]", ctx.getTenantId(), ctx.getRuleChainName(), msg.getData(), e);
            }
            throw new TbNodeException(e);
        }
    }

}
