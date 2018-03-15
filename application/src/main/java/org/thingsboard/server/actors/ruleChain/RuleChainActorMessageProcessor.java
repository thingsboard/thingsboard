/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.plugin.*;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.plugins.Plugin;
import org.thingsboard.server.extensions.api.plugins.PluginInitializationException;
import org.thingsboard.server.extensions.api.plugins.msg.FromDeviceRpcResponse;
import org.thingsboard.server.extensions.api.plugins.msg.TimeoutMsg;
import org.thingsboard.server.extensions.api.plugins.rest.PluginRestMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.PluginRpcMsg;
import org.thingsboard.server.extensions.api.plugins.ws.msg.PluginWebsocketMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;

/**
 * @author Andrew Shvayka
 */
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private ComponentLifecycleState state;

    protected RuleChainActorMessageProcessor(TenantId tenantId, RuleChainId pluginId, ActorSystemContext systemContext
            , LoggingAdapter logger, ActorRef parent, ActorRef self) {
        super(systemContext, logger, tenantId, pluginId);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void onCreated(ActorContext context) throws Exception {

    }

    @Override
    public void onUpdate(ActorContext context) throws Exception {

    }

    @Override
    public void onActivate(ActorContext context) throws Exception {

    }

    @Override
    public void onSuspend(ActorContext context) throws Exception {

    }

    @Override
    public void onStop(ActorContext context) throws Exception {

    }

    @Override
    public void onClusterEventMsg(ClusterEventMsg msg) throws Exception {

    }
}
