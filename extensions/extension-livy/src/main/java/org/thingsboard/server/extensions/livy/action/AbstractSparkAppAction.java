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
package org.thingsboard.server.extensions.livy.action;

import org.thingsboard.server.common.data.SparkApplication;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;

import java.util.Optional;

public abstract class AbstractSparkAppAction<T extends LivyPluginActionConfiguration> extends SimpleRuleLifecycleComponent implements PluginAction<T>{

    protected T configuration;

    @Override
    public void init(T configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg msg, RuleProcessingMetaData deviceMsgMd) {
        String application = configuration.getApplication();
        LivyActionPayload.LivyActionPayloadBuilder builder = LivyActionPayload.builder();
        builder.sparkApplication(application);
        builder.actionPath(configuration.getActionPath());
        String request = buildLivyRequest();
        builder.msgBody(request);
        return Optional.of(new LivyActionMessage(msg.getTenantId(),
                msg.getCustomerId(), msg.getDeviceId(), builder.build()));
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
        return true;
    }

    protected abstract String buildLivyRequest();

    protected abstract String[] args();
}
