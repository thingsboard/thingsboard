// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.config.ssl.PemSslCredentials;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc.EdgeRpcServiceImplBase;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class GrpcServer {

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    private String privateKeyResource;
    @Value("${edges.rpc.ssl.key_password:}")
    private String keyPassword;
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
            setupSsl(serverBuilder);
        }
        server = serverBuilder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!", e);
        }
        log.info("Edge RPC server initialized!");
    }

    /**
     * Configures TLS for the Edge gRPC server.
     * <p>
     * Delegates PEM parsing and key management to {@link PemSslCredentials} — the same
     * class used by MQTT, CoAP, and LwM2M transports — which supports:
     * <ul>
     *   <li>Separate certificate and private key files (classic two-file setup)</li>
     *   <li>Combined PEM: certificate chain + private key in a single {@code cert} file
     *       ({@code private_key} left empty)</li>
     *   <li>Encrypted private keys (password supplied via {@code key_password})</li>
     * </ul>
     * Path resolution (for both {@code cert} and {@code private_key}) is handled by
     * {@link org.thingsboard.server.common.data.ResourceUtils#getInputStream ResourceUtils}:
     * absolute path → relative / working-dir → classpath → {@code classpath:} prefix.
     */
    void setupSsl(NettyServerBuilder builder) {
        try {
            PemSslCredentials credentials = new PemSslCredentials();
            credentials.setCertFile(certFileResource);
            credentials.setKeyFile(StringUtils.isEmpty(privateKeyResource) ? null : privateKeyResource);
            credentials.setKeyPassword(keyPassword);
            credentials.init(false);

            SslContext sslContext = GrpcSslContexts.configure(
                    SslContextBuilder.forServer(credentials.createKeyManagerFactory())).build();
            builder.sslContext(sslContext);
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
