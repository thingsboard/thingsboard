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

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.rpc.RpcBroadcastMsg;
import org.thingsboard.server.actors.rpc.RpcSessionCreateRequestMsg;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.gen.cluster.ClusterRpcServiceGrpc;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.service.cluster.discovery.ServerInstanceService;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class ClusterGrpcService extends ClusterRpcServiceGrpc.ClusterRpcServiceImplBase implements ClusterRpcService {

    @Autowired
    private ServerInstanceService instanceService;

    @Autowired
    private DataDecodingEncodingService encodingService;

    private RpcMsgListener listener;

    private Server server;

    private ServerInstance instance;

    private ConcurrentMap<UUID, BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>>> pendingSessionMap =
            new ConcurrentHashMap<>();

    public void init(RpcMsgListener listener) {
        this.listener = listener;
        log.info("Initializing RPC service!");
        instance = instanceService.getSelf();
        server = ServerBuilder.forPort(instance.getPort()).addService(this).build();
        log.info("Going to start RPC server using port: {}", instance.getPort());
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!");
        }
        log.info("RPC service initialized!");
    }

    @Override
    public void onSessionCreated(UUID msgUid, StreamObserver<ClusterAPIProtos.ClusterMessage> inputStream) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = pendingSessionMap.remove(msgUid);
        if (queue != null) {
            try {
                queue.put(inputStream);
            } catch (InterruptedException e) {
                log.warn("Failed to report created session!");
                Thread.currentThread().interrupt();
            }
        } else {
            log.warn("Failed to lookup pending session!");
        }
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ClusterMessage> handleMsgs(
            StreamObserver<ClusterAPIProtos.ClusterMessage> responseObserver) {
        log.info("Processing new session.");
        return createSession(new RpcSessionCreateRequestMsg(UUID.randomUUID(), null, responseObserver));
    }


    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Going to onStop RPC server");
            server.shutdownNow();
            try {
                server.awaitTermination();
                log.info("RPC server stopped!");
            } catch (InterruptedException e) {
                log.warn("Failed to onStop RPC server!");
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public void broadcast(RpcBroadcastMsg msg) {
        listener.onBroadcastMsg(msg);
    }

    private StreamObserver<ClusterAPIProtos.ClusterMessage> createSession(RpcSessionCreateRequestMsg msg) {
        BlockingQueue<StreamObserver<ClusterAPIProtos.ClusterMessage>> queue = new ArrayBlockingQueue<>(1);
        pendingSessionMap.put(msg.getMsgUid(), queue);
        listener.onRpcSessionCreateRequestMsg(msg);
        try {
            StreamObserver<ClusterAPIProtos.ClusterMessage> observer = queue.take();
            log.info("Processed new session.");
            return observer;
        } catch (Exception e) {
            log.info("Failed to process session.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tell(ClusterAPIProtos.ClusterMessage message) {
        listener.onSendMsg(message);
    }

    @Override
    public void tell(ServerAddress serverAddress, TbActorMsg actorMsg) {
        listener.onSendMsg(encodingService.convertToProtoDataMessage(serverAddress, actorMsg));
    }

    @Override
    public void tell(ServerAddress serverAddress, ClusterAPIProtos.MessageType msgType, byte[] data) {
        ClusterAPIProtos.ClusterMessage msg = ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setServerAddress(ClusterAPIProtos.ServerAddress
                        .newBuilder()
                        .setHost(serverAddress.getHost())
                        .setPort(serverAddress.getPort())
                        .build())
                .setMessageType(msgType)
                .setPayload(ByteString.copyFrom(data)).build();
        listener.onSendMsg(msg);
    }
}
