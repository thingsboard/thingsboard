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

import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

/**
 * @author Andrew Shvayka
 */
public interface GrpcSessionListener {

    void onConnected(GrpcSession session);

    void onDisconnected(GrpcSession session);

    void onToPluginRpcMsg(GrpcSession session, ClusterAPIProtos.ToPluginRpcMessage msg);

    void onToDeviceActorRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceActorRpcMessage msg);

    void onToDeviceActorNotificationRpcMsg(GrpcSession grpcSession, ClusterAPIProtos.ToDeviceActorNotificationRpcMessage msg);

    void onToDeviceSessionActorRpcMsg(GrpcSession session, ClusterAPIProtos.ToDeviceSessionActorRpcMessage msg);

    void onToAllNodesRpcMessage(GrpcSession grpcSession, ClusterAPIProtos.ToAllNodesRpcMessage toAllNodesRpcMessage);

    void onToDeviceRpcRequestRpcMsg(GrpcSession grpcSession, ClusterAPIProtos.ToDeviceRpcRequestRpcMessage toDeviceRpcRequestRpcMsg);

    void onFromDeviceRpcResponseRpcMsg(GrpcSession grpcSession, ClusterAPIProtos.ToPluginRpcResponseRpcMessage toPluginRpcResponseRpcMsg);

    void onError(GrpcSession session, Throwable t);

}
