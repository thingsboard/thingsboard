/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc.EdgeRpcServiceImplBase;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.edge.rpc.service.EdgeRpcServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class GrpcServer {

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    private String privateKeyResource;
    @Value("${edges.rpc.client_max_keep_alive_time_sec:1}")
    private int clientMaxKeepAliveTimeSec;
    @Value("${edges.rpc.max_inbound_message_size:4194304}")
    private int maxInboundMessageSize;
    @Value("${edges.rpc.keep_alive_time_sec:10}")
    private int keepAliveTimeSec;
    @Value("${edges.rpc.keep_alive_timeout_sec:5}")
    private int keepAliveTimeoutSec;

    private final EdgeRpcServiceImplBase edgeGrpcService;

    private Server server;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onStartUp() {
        log.info("Initializing Edge RPC server!");
        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxInboundMessageSize)
                .addService(edgeGrpcService);

        if (sslEnabled) {
            addSslConfiguration(serverBuilder);
        }
        server = serverBuilder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        log.info("Edge RPC server initialized!");
    }

    private void addSslConfiguration(NettyServerBuilder serverBuilder) {
        try {
            InputStream certFileIs = ResourceUtils.getInputStream(this, certFileResource);
            InputStream privateKeyFileIs = ResourceUtils.getInputStream(this, privateKeyResource);
            serverBuilder.useTransportSecurity(certFileIs, privateKeyFileIs);
        } catch (Exception e) {
            log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
            throw new RuntimeException("Unable to set up SSL context!", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }
}
