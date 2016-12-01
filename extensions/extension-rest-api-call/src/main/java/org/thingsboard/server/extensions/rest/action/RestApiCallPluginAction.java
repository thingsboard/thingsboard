/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.extensions.rest.action;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.FromDeviceRequestMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.core.action.template.AbstractTemplatePluginAction;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import java.util.Optional;

@Action(name = "REST API Call Plugin Action",
        descriptor = "RestApiCallActionDescriptor.json", configuration = RestApiCallPluginActionConfiguration.class)
@Slf4j
public class RestApiCallPluginAction extends AbstractTemplatePluginAction<RestApiCallPluginActionConfiguration> {

    @Override
    protected Optional<RuleToPluginMsg<?>> buildRuleToPluginMsg(RuleContext ctx, ToDeviceActorMsg msg, FromDeviceRequestMsg payload) {
        RestApiCallActionPayload.RestApiCallActionPayloadBuilder builder = RestApiCallActionPayload.builder();
        builder.msgType(payload.getMsgType());
        builder.requestId(payload.getRequestId());
        builder.sync(configuration.isSync());
        builder.actionPath(configuration.getActionPath());
        builder.httpMethod(HttpMethod.valueOf(configuration.getRequestMethod()[0]));
        builder.expectedResultCode(HttpStatus.valueOf(configuration.getExpectedResultCode()));
        builder.msgBody(getMsgBody(ctx, msg));
        return Optional.of(new RestApiCallActionMsg(msg.getTenantId(),
                msg.getCustomerId(),
                msg.getDeviceId(),
                builder.build()));
    }

}
