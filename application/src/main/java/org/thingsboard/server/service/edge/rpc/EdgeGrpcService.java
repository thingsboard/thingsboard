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
package org.thingsboard.server.service.edge.rpc;

import com.google.common.io.Resources;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.edge.gen.EdgeProtos;
import org.thingsboard.server.common.edge.gen.EdgeRpcServiceGrpc;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges.rpc", value = "enabled", havingValue = "true")
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase {

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.privateKey}")
    private String privateKeyResource;

    private Server server;

    @PostConstruct
    public void init() {
        log.info("Initializing Edge RPC service!");
        ServerBuilder builder = ServerBuilder.forPort(rpcPort).addService(this);
        if (sslEnabled) {
            try {
                File certFile = new File(Resources.getResource(certFileResource).toURI());
                File privateKeyFile = new File(Resources.getResource(privateKeyResource).toURI());
                builder.useTransportSecurity(certFile, privateKeyFile);
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        log.info("Edge RPC service initialized!");
    }


    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<EdgeProtos.UplinkMsg> sendUplink(StreamObserver<EdgeProtos.DownlinkMsg> responseObserver) {
        log.info("sendUplink [{}]", responseObserver);
        return new StreamObserver<EdgeProtos.UplinkMsg>() {

            @Override
            public void onNext(EdgeProtos.UplinkMsg uplinkMsg) {
                log.info("onNext [{}]", uplinkMsg);
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("onError", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("onCompleted");
            }
        };
    }
}
