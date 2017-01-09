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
package org.thingsboard.server.extensions.api.plugins;

import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.component.ConfigurableComponent;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.rest.PluginRestMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.RpcMsg;
import org.thingsboard.server.extensions.api.plugins.ws.msg.PluginWebsocketMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;

public interface Plugin<T> extends ConfigurableComponent<T> {

    void process(PluginContext ctx, PluginWebsocketMsg<?> wsMsg);

    void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException;

    void process(PluginContext ctx, PluginRestMsg msg);

    void process(PluginContext ctx, RpcMsg msg);

    void process(PluginContext ctx, FromDeviceRpcResponse msg);

    void process(PluginContext ctx, TimeoutMsg<?> msg);

    void onServerAdded(PluginContext ctx, ServerAddress server);

    void onServerRemoved(PluginContext ctx, ServerAddress server);

    void resume(PluginContext ctx);

    void suspend(PluginContext ctx);

    void stop(PluginContext ctx);

}
