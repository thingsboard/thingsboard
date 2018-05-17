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
package org.thingsboard.server.service.cluster.rpc;

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.actors.rpc.RpcBroadcastMsg;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public interface ClusterRpcService {

    void init(RpcMsgListener listener);

    void broadcast(RpcBroadcastMsg msg);

    void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream);

    void tell(ClusterAPIProtos.ClusterMessage message);

    void tell(ServerAddress serverAddress, TbActorMsg actorMsg);

    void tell(ServerAddress serverAddress, ClusterAPIProtos.MessageType msgType, byte[] data);
}
