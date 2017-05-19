/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.plugin.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.ToServerRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToServerRpcResponseMsg;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RpcResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.core.action.rpc.RpcPluginAction;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "Time Plugin", actions = {RpcPluginAction.class},
        descriptor = "TimePluginDescriptor.json", configuration = TimePluginConfiguration.class)
@Slf4j
public class TimePlugin extends AbstractPlugin<TimePluginConfiguration> implements RuleMsgHandler {

    private DateTimeFormatter formatter;
    private String format;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg.getPayload() instanceof ToServerRpcRequestMsg) {
            ToServerRpcRequestMsg request = (ToServerRpcRequestMsg) msg.getPayload();

            String reply;
            if (!StringUtils.isEmpty(format)) {
                reply = "\"" + formatter.format(ZonedDateTime.now()) + "\"";
            } else {
                reply = Long.toString(System.currentTimeMillis());
            }
            ToServerRpcResponseMsg response = new ToServerRpcResponseMsg(request.getRequestId(), "{\"time\":" + reply + "}");
            ctx.reply(new RpcResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, response));
        } else {
            throw new RuntimeException("Not supported msg type: " + msg.getPayload().getClass() + "!");
        }
    }

    @Override
    public void init(TimePluginConfiguration configuration) {
        format = configuration.getTimeFormat();
        if (!StringUtils.isEmpty(format)) {
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }

    @Override
    public void resume(PluginContext ctx) {

    }

    @Override
    public void suspend(PluginContext ctx) {

    }

    @Override
    public void stop(PluginContext ctx) {

    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return this;
    }
}
