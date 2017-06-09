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
package org.thingsboard.server.extensions.core.action.telemetry;

import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.core.GetAttributesRequest;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.*;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Action(name = "Telemetry Plugin Action", descriptor = "TelemetryPluginActionDescriptor.json", configuration = TelemetryPluginActionConfiguration.class)
public class TelemetryPluginAction extends SimpleRuleLifecycleComponent implements PluginAction<TelemetryPluginActionConfiguration> {

    protected TelemetryPluginActionConfiguration configuration;
    protected long ttl;

    @Override
    public void init(TelemetryPluginActionConfiguration configuration) {
        this.configuration = configuration;
        if (StringUtils.isEmpty(configuration.getTimeUnit()) || configuration.getTtlValue() == 0L) {
            this.ttl = 0L;
        } else {
            this.ttl = TimeUnit.valueOf(configuration.getTimeUnit()).toSeconds(configuration.getTtlValue());
        }
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData deviceMsgMd) {
        FromDeviceMsg msg = toDeviceActorMsg.getPayload();
        if (msg.getMsgType() == MsgType.POST_TELEMETRY_REQUEST) {
            TelemetryUploadRequest payload = (TelemetryUploadRequest) msg;
            return Optional.of(new TelemetryUploadRequestRuleToPluginMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(),
                    toDeviceActorMsg.getDeviceId(), payload, ttl));
        } else if (msg.getMsgType() == MsgType.POST_ATTRIBUTES_REQUEST) {
            UpdateAttributesRequest payload = (UpdateAttributesRequest) msg;
            return Optional.of(new UpdateAttributesRequestRuleToPluginMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(),
                    toDeviceActorMsg.getDeviceId(), payload));
        } else if (msg.getMsgType() == MsgType.GET_ATTRIBUTES_REQUEST) {
            GetAttributesRequest payload = (GetAttributesRequest) msg;
            return Optional.of(new GetAttributesRequestRuleToPluginMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(),
                    toDeviceActorMsg.getDeviceId(), payload));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        if (response instanceof ResponsePluginToRuleMsg) {
            return Optional.of(((ResponsePluginToRuleMsg) response).getPayload());
        }
        return Optional.empty();
    }

    @Override
    public boolean isOneWayAction() {
        return false;
    }
}
