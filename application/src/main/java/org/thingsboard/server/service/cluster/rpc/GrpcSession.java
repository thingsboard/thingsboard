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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.io.Closeable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
@Slf4j
final public class GrpcSession implements Closeable {
    private final UUID sessionId;
    private final boolean client;
    private final GrpcSessionListener listener;
    private StreamObserver<ClusterAPIProtos.ToRpcServerMessage> inputStream;
    private StreamObserver<ClusterAPIProtos.ToRpcServerMessage> outputStream;

    private boolean connected;
    private ServerAddress remoteServer;

    public GrpcSession(GrpcSessionListener listener) {
        this(null, listener);
    }

    public GrpcSession(ServerAddress remoteServer, GrpcSessionListener listener) {
        this.sessionId = UUID.randomUUID();
        this.listener = listener;
        if (remoteServer != null) {
            this.client = true;
            this.connected = true;
            this.remoteServer = remoteServer;
        } else {
            this.client = false;
        }
    }

    public void initInputStream() {
        this.inputStream = new StreamObserver<ClusterAPIProtos.ToRpcServerMessage>() {
            @Override
            public void onNext(ClusterAPIProtos.ToRpcServerMessage msg) {
                if (!connected) {
                    if (msg.hasConnectMsg()) {
                        connected = true;
                        ClusterAPIProtos.ServerAddress rpcAddress = msg.getConnectMsg().getServerAddress();
                        remoteServer = new ServerAddress(rpcAddress.getHost(), rpcAddress.getPort());
                        listener.onConnected(GrpcSession.this);
                    }
                }
                if (connected) {
                    if (msg.hasToPluginRpcMsg()) {
                        listener.onToPluginRpcMsg(GrpcSession.this, msg.getToPluginRpcMsg());
                    }
                    if (msg.hasToDeviceActorRpcMsg()) {
                        listener.onToDeviceActorRpcMsg(GrpcSession.this, msg.getToDeviceActorRpcMsg());
                    }
                    if (msg.hasToDeviceSessionActorRpcMsg()) {
                        listener.onToDeviceSessionActorRpcMsg(GrpcSession.this, msg.getToDeviceSessionActorRpcMsg());
                    }
                    if (msg.hasToDeviceActorNotificationRpcMsg()) {
                        listener.onToDeviceActorNotificationRpcMsg(GrpcSession.this, msg.getToDeviceActorNotificationRpcMsg());
                    }
                    if (msg.hasToDeviceRpcRequestRpcMsg()) {
                        listener.onToDeviceRpcRequestRpcMsg(GrpcSession.this, msg.getToDeviceRpcRequestRpcMsg());
                    }
                    if (msg.hasToPluginRpcResponseRpcMsg()) {
                        listener.onFromDeviceRpcResponseRpcMsg(GrpcSession.this, msg.getToPluginRpcResponseRpcMsg());
                    }
                    if (msg.hasToAllNodesRpcMsg()) {
                        listener.onToAllNodesRpcMessage(GrpcSession.this, msg.getToAllNodesRpcMsg());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                listener.onError(GrpcSession.this, t);
            }

            @Override
            public void onCompleted() {
                outputStream.onCompleted();
                listener.onDisconnected(GrpcSession.this);
            }
        };
    }

    public void initOutputStream() {
        if (client) {
            listener.onConnected(GrpcSession.this);
        }
    }

    public void sendMsg(ClusterAPIProtos.ToRpcServerMessage msg) {
        outputStream.onNext(msg);
    }

    public void onError(Throwable t) {
        outputStream.onError(t);
    }

    @Override
    public void close() {
        try {
            outputStream.onCompleted();
        } catch (IllegalStateException e) {
            log.debug("[{}] Failed to close output stream: {}", sessionId, e.getMessage());
        }
    }
}
