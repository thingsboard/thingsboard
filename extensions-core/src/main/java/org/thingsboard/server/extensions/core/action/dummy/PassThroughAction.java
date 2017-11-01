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
package org.thingsboard.server.extensions.core.action.dummy;

import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.component.EmptyComponentConfiguration;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;

import java.util.Optional;

@Action(name = "Pass Through Plugin Action")
public class PassThroughAction extends SimpleRuleLifecycleComponent implements PluginAction<EmptyComponentConfiguration> {
    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ruleContext, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData ruleProcessingMetaData) {
        return Optional.empty();
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> pluginToRuleMsg) {
        return Optional.empty();
    }

    @Override
    public boolean isOneWayAction() {
        return true;
    }

    @Override
    public void init(EmptyComponentConfiguration emptyComponentConfiguration) {
    }
}
