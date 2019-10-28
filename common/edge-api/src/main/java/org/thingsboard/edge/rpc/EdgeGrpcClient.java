/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import com.google.common.io.Resources;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.edge.gen.EdgeProtos;
import org.thingsboard.server.common.edge.gen.EdgeRpcServiceGrpc;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URISyntaxException;

@Service
@Slf4j
public class EdgeGrpcClient implements EdgeRpcClient {

    @Value("${cloud.rpc.host}")
    private String rpcHost;
    @Value("${cloud.rpc.port}")
    private int rpcPort;
    @Value("${cloud.rpc.timeout}")
    private int timeoutSecs;
    @Value("${cloud.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${cloud.rpc.ssl.cert}")
    private String certResource;

    private ManagedChannel channel;

    private StreamObserver<EdgeProtos.UplinkMsg> inputStream;

    @Override
    public void connect() {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(rpcHost, rpcPort).usePlaintext();
        if (sslEnabled) {
            try {
                builder.sslContext(GrpcSslContexts.forClient().trustManager(new File(Resources.getResource(certResource).toURI())).build());
            } catch (URISyntaxException | SSLException e) {
                log.error("Failed to initialize channel!", e);
                throw new RuntimeException(e);
            }
        }
        channel = builder.build();
        EdgeRpcServiceGrpc.EdgeRpcServiceStub stub = EdgeRpcServiceGrpc.newStub(channel);
        StreamObserver<EdgeProtos.DownlinkMsg> responseObserver = new StreamObserver<EdgeProtos.DownlinkMsg>() {
            @Override
            public void onNext(EdgeProtos.DownlinkMsg downlinkMsg) {
                log.info("onNext [{}]", downlinkMsg);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };
        inputStream = stub.sendUplink(responseObserver);
        inputStream.onNext(EdgeProtos.UplinkMsg.newBuilder().setMsgType(EdgeProtos.UplinkMsgType.DELETE_DEVICE_MESSAGE).build());
    }
}
