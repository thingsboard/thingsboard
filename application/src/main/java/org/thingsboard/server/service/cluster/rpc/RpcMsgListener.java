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
package org.thingsboard.server.service.cluster.rpc;

import org.thingsboard.server.actors.rpc.RpcBroadcastMsg;
import org.thingsboard.server.actors.rpc.RpcSessionCreateRequestMsg;
import org.thingsboard.server.actors.rpc.RpcSessionTellMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;
import org.thingsboard.server.common.msg.core.ToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginActorMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.PluginRpcMsg;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

/**
 * @author Andrew Shvayka
 */
public interface RpcMsgListener {

    void onMsg(ToDeviceActorMsg msg);

    void onMsg(ToDeviceActorNotificationMsg msg);

    void onMsg(ToDeviceSessionActorMsg msg);

    void onMsg(ToAllNodesMsg nodeMsg);

    void onMsg(ToPluginActorMsg msg);

    void onMsg(RpcSessionCreateRequestMsg msg);

    void onMsg(RpcSessionTellMsg rpcSessionTellMsg);

    void onMsg(RpcBroadcastMsg rpcBroadcastMsg);

}
