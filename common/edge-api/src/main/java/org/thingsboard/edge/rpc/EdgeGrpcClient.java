// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.edge.rpc;

import io.grpc.HttpConnectProxiedSocketAddress;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.util.concurrent.DefaultThreadFactory;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
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

    private final EventLoopGroup workerGroup = createWorkerGroup();

    private StreamObserver<RequestMsg> inputStream;

    private volatile boolean connected;

    private volatile boolean streamActive;

    private static final ReentrantLock uplinkMsgLock = new ReentrantLock();

    @Override
    public void connect(String edgeKey,
                        String edgeSecret,
                        Consumer<UplinkResponseMsg> onUplinkResponse,
                        Consumer<EdgeConfiguration> onEdgeUpdate,
                        Consumer<DownlinkMsg> onDownlink,
                        Consumer<Exception> onError) {
        connected = false;
        streamActive = false;
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(rpcHost, rpcPort)
                .eventLoopGroup(workerGroup)
                .channelType(channelType())
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

        channel = builder.build();
        EdgeRpcServiceGrpc.EdgeRpcServiceStub stub = EdgeRpcServiceGrpc.newStub(channel);
        log.info("[{}] Sending a connect request to the TB!", edgeKey);
        this.inputStream = stub.withCompression("gzip").handleMsgs(initOutputStream(edgeKey, onUplinkResponse, onEdgeUpdate, onDownlink, onError));
        streamActive = true;
        this.inputStream.onNext(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.CONNECT_RPC_MESSAGE)
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                        .setEdgeRoutingKey(edgeKey)
                        .setEdgeSecret(edgeSecret)
                        .setEdgeVersion(getNewestEdgeVersion())
                        .setMaxInboundMessageSize(maxInboundMessageSize)
                        .build())
                .build());
    }

    public static EdgeVersion getNewestEdgeVersion() {
        return EdgeVersionComparator.getNewestEdgeVersion();
    }

    private static EventLoopGroup createWorkerGroup() {
        DefaultThreadFactory threadFactory = new DefaultThreadFactory("edge-grpc-worker", true);
        return Epoll.isAvailable() ? new EpollEventLoopGroup(1, threadFactory) : new NioEventLoopGroup(1, threadFactory);
    }

    private static Class<? extends Channel> channelType() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    @PreDestroy
    public void destroy() {
        workerGroup.shutdownGracefully();
    }

    private StreamObserver<ResponseMsg> initOutputStream(String edgeKey,
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
                        connected = true;
                        onEdgeUpdate.accept(connectResponseMsg.getConfiguration());
                    } else {
                        connected = false;
                        log.error("[{}] Failed to establish the connection! Code: {}. Error message: {}.", edgeKey, connectResponseMsg.getResponseCode(), connectResponseMsg.getErrorMsg());
                        try {
                            EdgeGrpcClient.this.disconnect(true);
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
                connected = false;
                streamActive = false;
                log.warn("[{}] Stream was terminated due to error:", edgeKey, t);
                try {
                    EdgeGrpcClient.this.disconnect(true);
                } catch (InterruptedException e) {
                    log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                }
                onError.accept(new RuntimeException(t));
            }

            @Override
            public void onCompleted() {
                connected = false;
                streamActive = false;
                log.info("[{}] Stream was closed and completed successfully!", edgeKey);
            }
        };
    }

    @Override
    public void disconnect(boolean onError) throws InterruptedException {
        connected = false;
        streamActive = false;
        if (!onError) {
            try {
                if (inputStream != null) {
                    inputStream.onCompleted();
                }
            } catch (Exception e) {
                log.error("Exception during onCompleted", e);
            }
        }
        if (channel != null) {
            channel.shutdown();
            int attempt = 0;
            do {
                try {
                    channel.awaitTermination(timeoutSecs, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Channel await termination was interrupted", e);
                }
                if (attempt > 5) {
                    log.warn("We had reached maximum of termination attempts. Force closing channel");
                    try {
                        channel.shutdownNow();
                    } catch (Exception e) {
                        log.error("Exception during shutdownNow", e);
                    }
                    break;
                }
                attempt++;
            } while (!channel.isTerminated());
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void sendUplinkMsg(UplinkMsg msg) {
        uplinkMsgLock.lock();
        try {
            if (!streamActive) {
                log.debug("Uplink msg is skipped, the cloud session is not established: {}", msg);
                return;
            }
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                    .setUplinkMsg(msg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }

    @Override
    public void sendSyncRequestMsg(boolean fullSyncRequired) {
        uplinkMsgLock.lock();
        try {
            if (!streamActive) {
                log.debug("Sync request msg is skipped, the cloud session is not established");
                return;
            }
            SyncRequestMsg syncRequestMsg = SyncRequestMsg.newBuilder()
                    .setFullSync(fullSyncRequired)
                    .build();
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)
                    .setSyncRequestMsg(syncRequestMsg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }

    @Override
    public void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg) {
        uplinkMsgLock.lock();
        try {
            if (!streamActive) {
                log.debug("Downlink response msg is skipped, the cloud session is not established: {}", downlinkResponseMsg);
                return;
            }
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                    .setDownlinkResponseMsg(downlinkResponseMsg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }

}
