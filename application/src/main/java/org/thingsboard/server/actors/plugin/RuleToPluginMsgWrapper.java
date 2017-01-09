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
package org.thingsboard.server.actors.plugin;

import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.aware.RuleAwareMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginActorMsg;

public class RuleToPluginMsgWrapper implements ToPluginActorMsg, RuleAwareMsg {

    private final TenantId pluginTenantId;
    private final PluginId pluginId;
    private final TenantId ruleTenantId;
    private final RuleId ruleId;
    private final RuleToPluginMsg<?> msg;

    public RuleToPluginMsgWrapper(TenantId pluginTenantId, PluginId pluginId, TenantId ruleTenantId, RuleId ruleId, RuleToPluginMsg<?> msg) {
        super();
        this.pluginTenantId = pluginTenantId;
        this.pluginId = pluginId;
        this.ruleTenantId = ruleTenantId;
        this.ruleId = ruleId;
        this.msg = msg;
    }

    @Override
    public TenantId getPluginTenantId() {
        return pluginTenantId;
    }

    @Override
    public PluginId getPluginId() {
        return pluginId;
    }

    public TenantId getRuleTenantId() {
        return ruleTenantId;
    }

    @Override
    public RuleId getRuleId() {
        return ruleId;
    }


    public RuleToPluginMsg<?> getMsg() {
        return msg;
    }

}
