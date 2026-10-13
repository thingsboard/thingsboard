// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.rpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.exception.IntegrationConnectionException;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.integration.ConnectRequestMsg;
import org.thingsboard.server.gen.integration.ConnectResponseCode;
import org.thingsboard.server.gen.integration.ConnectResponseMsg;
import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.DeviceDownlinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.MessageType;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
public class IntegrationGrpcClient implements IntegrationRpcClient {

    @Value("${rpc.host}")
    private String rpcHost;
    @Value("${rpc.port}")
    private int rpcPort;
    @Value("${rpc.timeout}")
    private int timeoutSecs;
    @Value("${rpc.keep_alive_time_sec:300}")
    private int keepAliveTimeSec;
    @Value("${rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${rpc.ssl.cert:}")
    private String certResource;

    @Autowired
    private EventStorage eventStorage;

    private ManagedChannel channel;
    private StreamObserver<RequestMsg> inputStream;
    private CountDownLatch latch;

    @Override
    public void connect(String integrationKey, String integrationSecret, String serviceId, Consumer<IntegrationConfigurationProto> onIntegrationUpdate
            , Consumer<ConverterConfigurationProto> onConverterUpdate, Consumer<DeviceDownlinkDataProto> onDownlink, Consumer<Exception> onError) {
        NettyChannelBuilder builder = NettyChannelBuilder
                .forAddress(rpcHost, rpcPort)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS);
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
        channel = builder.build();
        IntegrationTransportGrpc.IntegrationTransportStub stub = IntegrationTransportGrpc.newStub(channel);
        log.info("[{}] Sending a connect request to the TB!", integrationKey);
        this.inputStream = stub.handleMsgs(initOutputStream(integrationKey, onIntegrationUpdate, onConverterUpdate, onDownlink, onError));
        this.inputStream.onNext(RequestMsg.newBuilder()
                .setMessageType(MessageType.CONNECT_RPC_MESSAGE)
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                        .setIntegrationRoutingKey(integrationKey)
                        .setIntegrationSecret(integrationSecret)
                        .setServiceId(serviceId)
                        .build())
                .build());
    }

    private StreamObserver<ResponseMsg> initOutputStream(String integrationKey, Consumer<IntegrationConfigurationProto> onIntegrationUpdate, Consumer<ConverterConfigurationProto> onConverterUpdate, Consumer<DeviceDownlinkDataProto> onDownlink, Consumer<Exception> onError) {
        return new StreamObserver<ResponseMsg>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                if (responseMsg.hasConnectResponseMsg()) {
                    ConnectResponseMsg connectResponseMsg = responseMsg.getConnectResponseMsg();
                    if (connectResponseMsg.getResponseCode().equals(ConnectResponseCode.ACCEPTED)) {
                        log.info("[{}] Configuration received: {}", integrationKey, connectResponseMsg.getConfiguration());
                        onIntegrationUpdate.accept(connectResponseMsg.getConfiguration());
                    } else {
                        log.error("[{}] Failed to establish the connection! Code: {}. Error message: {}.", integrationKey, connectResponseMsg.getResponseCode(), connectResponseMsg.getErrorMsg());
                        try {
                            IntegrationGrpcClient.this.disconnect();
                        } catch (InterruptedException e) {
                            log.error("[{}] Got interruption during disconnect!", integrationKey, e);
                        }
                        onError.accept(new IntegrationConnectionException("Failed to establish the connection! Response code: " + connectResponseMsg.getResponseCode().name()));
                    }
                } else if (responseMsg.hasUplinkResponseMsg()) {
                    UplinkResponseMsg msg = responseMsg.getUplinkResponseMsg();
                    if (msg.getSuccess()) {
                        log.debug("[{}] Msg has been processed successfully! {}", integrationKey, msg);
                    } else {
                        log.error("[{}] Msg processing failed! Error msg: {}", integrationKey, msg.getErrorMsg());
                    }
                    latch.countDown();
                } else if (responseMsg.hasIntegrationUpdateMsg()) {
                    log.info("[{}] Configuration updated: {}", integrationKey, responseMsg.getIntegrationUpdateMsg().getConfiguration());
                    onIntegrationUpdate.accept(responseMsg.getIntegrationUpdateMsg().getConfiguration());
                } else if (responseMsg.hasConverterUpdateMsg()) {
                    log.info("[{}] Converter configuration updated: {}", integrationKey, responseMsg.getConverterUpdateMsg().getConfiguration());
                    onConverterUpdate.accept(responseMsg.getConverterUpdateMsg().getConfiguration());
                } else if (responseMsg.hasDownlinkMsg()) {
                    log.debug("[{}] Downlink message received for device {}", integrationKey, responseMsg.getDownlinkMsg().getDeviceData().getDeviceName());
                    onDownlink.accept(responseMsg.getDownlinkMsg().getDeviceData());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[{}] The rpc session received an error!", integrationKey, t);
                onError.accept(new RuntimeException(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[{}] The rpc session was closed!", integrationKey);
            }
        };
    }

    @Override
    public void disconnect() throws InterruptedException {
        try {
            inputStream.onCompleted();
        } catch (Exception e) {
            log.error("Exception during onCompleted", e);
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
    public void handleMsgs() throws InterruptedException {
        List<UplinkMsg> uplinkMsgList = eventStorage.readCurrentBatch();
        latch = new CountDownLatch(uplinkMsgList.size());
        for (UplinkMsg msg : uplinkMsgList) {
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMessageType(MessageType.UPLINK_RPC_MESSAGE)
                    .setUplinkMsg(msg)
                    .build());
        }
        boolean success = latch.await(10, TimeUnit.SECONDS);
        if (!success) {
            log.warn("Failed to deliver the batch: {}", uplinkMsgList);
        }
        if (success && !uplinkMsgList.isEmpty()) {
            eventStorage.discardCurrentBatch();
        } else {
            eventStorage.sleep();
        }
    }
}
