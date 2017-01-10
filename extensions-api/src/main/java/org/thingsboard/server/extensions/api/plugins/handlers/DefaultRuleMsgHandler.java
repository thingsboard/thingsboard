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
package org.thingsboard.server.extensions.api.plugins.handlers;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.msg.GetAttributesRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TelemetryUploadRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.UpdateAttributesRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DefaultRuleMsgHandler implements RuleMsgHandler {

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg instanceof TelemetryUploadRequestRuleToPluginMsg) {
            handleTelemetryUploadRequest(ctx, tenantId, ruleId, (TelemetryUploadRequestRuleToPluginMsg) msg);
        } else if (msg instanceof UpdateAttributesRequestRuleToPluginMsg) {
            handleUpdateAttributesRequest(ctx, tenantId, ruleId, (UpdateAttributesRequestRuleToPluginMsg) msg);
        } else if (msg instanceof GetAttributesRequestRuleToPluginMsg) {
            handleGetAttributesRequest(ctx, tenantId, ruleId, (GetAttributesRequestRuleToPluginMsg) msg);
        }
        //TODO: handle subscriptions to attribute updates.
    }

    protected void handleGetAttributesRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, GetAttributesRequestRuleToPluginMsg msg) {
        msgTypeNotSupported(msg.getPayload().getMsgType());
    }

    protected void handleUpdateAttributesRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, UpdateAttributesRequestRuleToPluginMsg msg) {
        msgTypeNotSupported(msg.getPayload().getMsgType());
    }

    protected void handleTelemetryUploadRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, TelemetryUploadRequestRuleToPluginMsg msg) {
        msgTypeNotSupported(msg.getPayload().getMsgType());
    }

    private void msgTypeNotSupported(MsgType msgType) {
        throw new RuntimeException("Not supported msg type: " + msgType + "!");
    }

}
