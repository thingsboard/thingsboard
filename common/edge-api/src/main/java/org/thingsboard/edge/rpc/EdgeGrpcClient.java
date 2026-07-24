/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.edge.rpc;

import io.grpc.HttpConnectProxiedSocketAddress;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.exception.EdgeConnectionException;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.SyncRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
public class EdgeGrpcClient implements EdgeRpcClient {

    @Value("${cloud.rpc.host}")
    private String rpcHost;
    @Value("${cloud.rpc.port}")
    private int rpcPort;
    @Value("${cloud.rpc.timeout}")
    private int timeoutSecs;
    @Value("${cloud.rpc.keep_alive_time_sec:10}")
    private int keepAliveTimeSec;
    @Value("${cloud.rpc.keep_alive_timeout_sec:5}")
    private int keepAliveTimeoutSec;
    @Value("${cloud.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${cloud.rpc.ssl.cert:}")
    private String certResource;
    @Value("${cloud.rpc.max_inbound_message_size:4194304}")
    private int maxInboundMessageSize;
    @Value("${cloud.rpc.proxy.enabled}")
    private boolean proxyEnabled;
    @Value("${cloud.rpc.proxy.host:}")
    private String proxyHost;
    @Value("${cloud.rpc.proxy.port:0}")
    private int proxyPort;
    @Value("${cloud.rpc.proxy.username:}")
    private String proxyUsername;
    @Value("${cloud.rpc.proxy.password:}")
    private String proxyPassword;
    @Getter
    private int serverMaxInboundMessageSize;

    private ManagedChannel channel;

    private StreamObserver<RequestMsg> inputStream;

    private final ReentrantLock connectionLock = new ReentrantLock();

    @Override
    public void connect(String edgeKey,
                        String edgeSecret,
                        Consumer<UplinkResponseMsg> onUplinkResponse,
                        Consumer<EdgeConfiguration> onEdgeUpdate,
                        Consumer<DownlinkMsg> onDownlink,
                        Consumer<Exception> onError) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(rpcHost, rpcPort)
                .maxInboundMessageSize(maxInboundMessageSize)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true);

        if (sslEnabled) {
            try {
                SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
                if (StringUtils.isNotEmpty(certResource)) {
                    sslContextBuilder.trustManager(ResourceUtils.getInputStream(this, certResource));
                }
                builder.sslContext(sslContextBuilder.build());
            } catch (SSLException e) {
                log.error("Failed to initialize channel!", e);
                throw new RuntimeException(e);
            }
        } else {
            builder.usePlaintext();
        }

        if (proxyEnabled && StringUtils.isNotEmpty(proxyHost) && proxyPort > 0) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
            InetSocketAddress targetAddress = new InetSocketAddress(rpcHost, rpcPort);
            builder.proxyDetector(socketAddress -> HttpConnectProxiedSocketAddress.newBuilder()
                    .setTargetAddress(targetAddress)
                    .setProxyAddress(proxyAddress)
                    .setUsername(proxyUsername)
                    .setPassword(proxyPassword)
                    .build());
        }

        connectionLock.lock();
        try {
            channel = builder.build();
            EdgeRpcServiceGrpc.EdgeRpcServiceStub stub = EdgeRpcServiceGrpc.newStub(channel);
            log.info("[{}] Sending a connect request to the TB!", edgeKey);
            this.inputStream = stub.withCompression("gzip").handleMsgs(initOutputStream(edgeKey, channel, onUplinkResponse, onEdgeUpdate, onDownlink, onError));
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.CONNECT_RPC_MESSAGE)
                    .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                            .setEdgeRoutingKey(edgeKey)
                            .setEdgeSecret(edgeSecret)
                            .setEdgeVersion(getNewestEdgeVersion())
                            .setMaxInboundMessageSize(maxInboundMessageSize)
                            .build())
                    .build());
        } catch (RuntimeException e) {
            // Release the half-built channel, otherwise every failed reconnect attempt leaks
            // its event loops and executors, and the next attempt may not recover at all.
            log.error("[{}] Failed to establish the connection, shutting down the channel!", edgeKey, e);
            if (channel != null) {
                try {
                    channel.shutdownNow();
                } catch (Exception shutdownEx) {
                    log.error("[{}] Exception during shutdownNow of the failed channel", edgeKey, shutdownEx);
                }
            }
            channel = null;
            inputStream = null;
            throw e;
        } finally {
            connectionLock.unlock();
        }
    }

    public static EdgeVersion getNewestEdgeVersion() {
        return EdgeVersionComparator.getNewestEdgeVersion();
    }

    private StreamObserver<ResponseMsg> initOutputStream(String edgeKey,
                                                         ManagedChannel streamChannel,
                                                         Consumer<UplinkResponseMsg> onUplinkResponse,
                                                         Consumer<EdgeConfiguration> onEdgeUpdate,
                                                         Consumer<DownlinkMsg> onDownlink,
                                                         Consumer<Exception> onError) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                if (responseMsg.hasConnectResponseMsg()) {
                    ConnectResponseMsg connectResponseMsg = responseMsg.getConnectResponseMsg();
                    if (connectResponseMsg.getResponseCode().equals(ConnectResponseCode.ACCEPTED)) {
                        if (connectResponseMsg.hasMaxInboundMessageSize()) {
                            log.debug("[{}] Server max inbound message size: {}", edgeKey, connectResponseMsg.getMaxInboundMessageSize());
                            serverMaxInboundMessageSize = connectResponseMsg.getMaxInboundMessageSize();
                        }
                        log.info("[{}] Configuration received: {}", edgeKey, connectResponseMsg.getConfiguration());
                        onEdgeUpdate.accept(connectResponseMsg.getConfiguration());
                    } else {
                        log.error("[{}] Failed to establish the connection! Code: {}. Error message: {}.", edgeKey, connectResponseMsg.getResponseCode(), connectResponseMsg.getErrorMsg());
                        try {
                            EdgeGrpcClient.this.disconnect(true, streamChannel);
                        } catch (InterruptedException e) {
                            log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                        }
                        onError.accept(new EdgeConnectionException("Failed to establish the connection! Response code: " + connectResponseMsg.getResponseCode().name()));
                    }
                } else if (responseMsg.hasEdgeUpdateMsg()) {
                    log.debug("[{}] Edge update message received {}", edgeKey, responseMsg.getEdgeUpdateMsg());
                    onEdgeUpdate.accept(responseMsg.getEdgeUpdateMsg().getConfiguration());
                } else if (responseMsg.hasUplinkResponseMsg()) {
                    log.debug("[{}] Uplink response message received {}", edgeKey, responseMsg.getUplinkResponseMsg());
                    onUplinkResponse.accept(responseMsg.getUplinkResponseMsg());
                } else if (responseMsg.hasDownlinkMsg()) {
                    log.debug("[{}] Downlink message received {}", edgeKey, responseMsg.getDownlinkMsg());
                    onDownlink.accept(responseMsg.getDownlinkMsg());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("[{}] Stream was terminated due to error:", edgeKey, t);
                try {
                    EdgeGrpcClient.this.disconnect(true, streamChannel);
                } catch (InterruptedException e) {
                    log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                }
                onError.accept(new RuntimeException(t));
            }

            @Override
            public void onCompleted() {
                log.info("[{}] Stream was closed and completed successfully!", edgeKey);
            }
        };
    }

    @Override
    public void disconnect(boolean onError) throws InterruptedException {
        disconnect(onError, null);
    }

    private void disconnect(boolean onError, ManagedChannel expectedChannel) throws InterruptedException {
        ManagedChannel channelToShutdown;
        connectionLock.lock();
        try {
            // A terminated stream reports its error asynchronously and may lose the race with a
            // reconnect: ignore such stale callbacks instead of tearing down the fresh channel.
            if (expectedChannel != null && channel != expectedChannel) {
                log.info("The channel was already replaced by a reconnect. Skipping disconnect of the stale channel.");
                return;
            }
            if (!onError) {
                try {
                    if (inputStream != null) {
                        inputStream.onCompleted();
                    }
                } catch (Exception e) {
                    log.error("Exception during onCompleted", e);
                }
            }
            // Clear the references under the lock so that no send can start against a stream
            // whose channel is shutting down - such a write leaks the compressed ByteBuf.
            inputStream = null;
            channelToShutdown = channel;
            channel = null;
        } finally {
            connectionLock.unlock();
        }
        if (channelToShutdown != null) {
            channelToShutdown.shutdown();
            int attempt = 0;
            do {
                try {
                    channelToShutdown.awaitTermination(timeoutSecs, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Channel await termination was interrupted", e);
                }
                if (attempt > 5) {
                    log.warn("We had reached maximum of termination attempts. Force closing channel");
                    try {
                        channelToShutdown.shutdownNow();
                    } catch (Exception e) {
                        log.error("Exception during shutdownNow", e);
                    }
                    break;
                }
                attempt++;
            } while (!channelToShutdown.isTerminated());
        }
    }

    @Override
    public void sendUplinkMsg(UplinkMsg msg) {
        sendRequestMsg(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                .setUplinkMsg(msg)
                .build());
    }

    @Override
    public void sendSyncRequestMsg(boolean fullSyncRequired) {
        SyncRequestMsg syncRequestMsg = SyncRequestMsg.newBuilder()
                .setFullSync(fullSyncRequired)
                .build();
        sendRequestMsg(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)
                .setSyncRequestMsg(syncRequestMsg)
                .build());
    }

    @Override
    public void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg) {
        sendRequestMsg(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                .setDownlinkResponseMsg(downlinkResponseMsg)
                .build());
    }

    private void sendRequestMsg(RequestMsg requestMsg) {
        connectionLock.lock();
        try {
            if (inputStream == null) {
                throw new IllegalStateException("Edge is not connected to the cloud. The message is discarded!");
            }
            inputStream.onNext(requestMsg);
        } finally {
            connectionLock.unlock();
        }
    }

}
