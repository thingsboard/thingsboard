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
package org.thingsboard.server.extensions.mqtt.action;

import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceRequestMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.core.action.template.AbstractTemplatePluginAction;

import java.util.Optional;

@Action(name = "Mqtt Plugin Action", descriptor = "MqttActionDescriptor.json", configuration = MqttPluginActionConfiguration.class)
public class MqttPluginAction extends AbstractTemplatePluginAction<MqttPluginActionConfiguration> {

    @Override
    protected Optional<RuleToPluginMsg<?>> buildRuleToPluginMsg(RuleContext ctx, ToDeviceActorMsg msg, FromDeviceRequestMsg payload) {
        MqttActionPayload.MqttActionPayloadBuilder builder = MqttActionPayload.builder();
        builder.sync(configuration.isSync());
        builder.msgType(payload.getMsgType());
        builder.requestId(payload.getRequestId());
        builder.topic(configuration.getTopic());
        builder.msgBody(getMsgBody(ctx, msg));
        return Optional.of(new MqttActionMsg(msg.getTenantId(),
                msg.getCustomerId(),
                msg.getDeviceId(),
                builder.build()));
    }
}
