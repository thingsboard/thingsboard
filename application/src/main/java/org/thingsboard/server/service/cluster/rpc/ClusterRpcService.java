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

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;
import org.thingsboard.server.common.msg.core.ToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.ToDeviceActorNotificationMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginRpcResponseDeviceMsg;
import org.thingsboard.server.extensions.api.plugins.rpc.PluginRpcMsg;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public interface ClusterRpcService {

    void init(RpcMsgListener listener);

    void tell(ServerAddress serverAddress, ToDeviceActorMsg toForward);

    void tell(ServerAddress serverAddress, ToDeviceSessionActorMsg toForward);

    void tell(ServerAddress serverAddress, ToDeviceActorNotificationMsg toForward);

    void tell(ServerAddress serverAddress, ToDeviceRpcRequestPluginMsg toForward);

    void tell(ServerAddress serverAddress, ToPluginRpcResponseDeviceMsg toForward);

    void tell(PluginRpcMsg toForward);

    void broadcast(ToAllNodesMsg msg);

    void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ToRpcServerMessage> inputStream);
}
