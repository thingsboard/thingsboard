// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.config.ssl.PemSslCredentials;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.integration.IntegrationTransportGrpc;
import org.thingsboard.server.gen.integration.RequestMsg;
import org.thingsboard.server.gen.integration.ResponseMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.integration.IntegrationContextComponent;
import org.thingsboard.server.service.integration.RemoteIntegrationRpcService;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && ('${integrations.rpc.enabled:false}'=='true')")
public class TbCoreRemoteIntegrationRpcService extends IntegrationTransportGrpc.IntegrationTransportImplBase implements RemoteIntegrationRpcService {

    private final Map<IntegrationId, IntegrationGrpcSession> sessions = new ConcurrentHashMap<>();

    @Value("${integrations.rpc.port}")
    private int rpcPort;
    @Value("${integrations.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${integrations.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${integrations.rpc.ssl.privateKey}")
    private String privateKeyResource;
    @Value("${integrations.rpc.ssl.key_password:}")
    private String keyPassword;
    @Value("${integrations.rpc.client_max_keep_alive_time_sec:300}")
    private int clientMaxKeepAliveTimeSec;

    private final TbServiceInfoProvider serviceInfoProvider;
    private final IntegrationContextComponent ctx;
    private final RemoteIntegrationSessionService sessionsCache;
    private final TbClusterService clusterService;
    private final DeviceService deviceService;
    private Server server;

    public TbCoreRemoteIntegrationRpcService(TbServiceInfoProvider serviceInfoProvider, IntegrationContextComponent ctx,
                                             RemoteIntegrationSessionService sessionsCache, TbClusterService clusterService, DeviceService deviceService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.ctx = ctx;
        this.sessionsCache = sessionsCache;
        this.clusterService = clusterService;
        this.deviceService = deviceService;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing RPC service!");
        NettyServerBuilder builder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .addService(this);
        if (sslEnabled) {
            try {
                setupSsl(builder);
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
        log.info("Going to start RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start RPC server!", e);
            throw new RuntimeException("Failed to start RPC server!", e);
        }
        log.info("RPC service initialized!");
    }

    /**
     * Configures TLS for the Integration gRPC server.
     * <p>
     * Delegates PEM parsing and key management to {@link PemSslCredentials} — the same
     * class used by MQTT, CoAP, and LwM2M transports — which supports:
     * <ul>
     *   <li>Separate certificate and private key files (classic two-file setup)</li>
     *   <li>Combined PEM: certificate chain + private key in a single {@code cert} file
     *       ({@code privateKey} left empty)</li>
     *   <li>Encrypted private keys (password supplied via {@code key_password})</li>
     * </ul>
     * Path resolution (for both {@code cert} and {@code privateKey}) is handled by
     * {@link org.thingsboard.server.common.data.ResourceUtils#getInputStream ResourceUtils}:
     * absolute path → relative / working-dir → classpath → {@code classpath:} prefix.
     */
    void setupSsl(NettyServerBuilder builder) throws Exception {
        PemSslCredentials credentials = new PemSslCredentials();
        credentials.setCertFile(certFileResource);
        credentials.setKeyFile(StringUtils.isEmpty(privateKeyResource) ? null : privateKeyResource);
        credentials.setKeyPassword(keyPassword);
        credentials.init(false);

        SslContext sslContext = GrpcSslContexts.configure(
                SslContextBuilder.forServer(credentials.createKeyManagerFactory())).build();
        builder.sslContext(sslContext);
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> responseObserver) {
        return new IntegrationGrpcSession(ctx, responseObserver, this::onIntegrationConnect, this::onIntegrationDisconnect).getInputStream();
    }

    @Override
    public void updateIntegration(Integration configuration) {
        IntegrationGrpcSession session = sessions.get(configuration.getId());
        if (session != null && session.isConnected()) {
            session.onConfigurationUpdate(configuration);
        }
    }

    @Override
    public void updateConverter(Converter converter) {
        for (Map.Entry<IntegrationId, IntegrationGrpcSession> entry : sessions.entrySet()) {
            Integration configuration = entry.getValue().getConfiguration();
            if (entry.getValue().isConnected()
                    && (configuration.getDefaultConverterId().equals(converter.getId()) || converter.getId().equals(configuration.getDownlinkConverterId()))) {
                try {
                    entry.getValue().onConverterUpdate(converter);
                } catch (Exception e) {
                    log.error("Failed to update integration [{}] with converter [{}]", entry.getKey().getId(), converter, e);
                }
            }
        }
    }

    @Override
    public boolean handleRemoteDownlink(IntegrationDownlinkMsg msg) {
        boolean sessionFound = false;
        IntegrationGrpcSession session = sessions.get(msg.getIntegrationId());
        if (session != null) {
            log.debug("[{}] Remote integration session found for [{}] downlink.", msg.getIntegrationId(), msg.getEntityId());
            Device device = deviceService.findDeviceById(msg.getTenantId(), new DeviceId(msg.getEntityId().getId()));
            if (device != null) {
                session.onDownlink(device, msg);
            } else {
                log.debug("[{}] device [{}] not found.", msg.getIntegrationId(), msg.getEntityId());
            }
            sessionFound = true;
        } else {
            IntegrationSession remoteSession = sessionsCache.findIntegrationSession(msg.getIntegrationId());
            if (remoteSession != null && !remoteSession.getServiceId().equals(serviceInfoProvider.getServiceId())) {
                log.debug("[{}] Remote integration session found for [{}] downlink @ Server [{}].", msg.getIntegrationId(), msg.getEntityId(), remoteSession.getServiceId());
                clusterService.pushNotificationToCore(remoteSession.getServiceId(), msg, null);
                sessionFound = true;
            }
        }
        return sessionFound;
    }

    private void onIntegrationConnect(IntegrationId integrationId, IntegrationGrpcSession integrationGrpcSession) {
        sessions.put(integrationId, integrationGrpcSession);
        sessionsCache.putIntegrationSession(integrationId, new IntegrationSession(serviceInfoProvider.getServiceId()));
    }

    private void onIntegrationDisconnect(IntegrationId integrationId) {
        IntegrationGrpcSession integrationGrpcSession = sessions.get(integrationId);
        if (integrationGrpcSession != null) {
            TenantId tenantId = integrationGrpcSession.getConfiguration().getTenantId();
            LifecycleEvent event = LifecycleEvent.builder()
                    .tenantId(tenantId)
                    .entityId(integrationId.getId())
                    .success(true)
                    .lcEventType("STOPPED")
                    .serviceId(integrationGrpcSession.getServiceId())
                    .build();

            ListenableFuture<Void> future = ctx.getEventService().saveAsync(event);
            Futures.transform(future, r -> {
                String key = "integration_status_" + event.getServiceId().toLowerCase();
                ctx.getAttributesService().removeAll(tenantId, integrationId, AttributeScope.SERVER_SCOPE, Collections.singletonList(key));
                return null;
            }, MoreExecutors.directExecutor());

            sessions.remove(integrationId);
        }

        sessionsCache.removeIntegrationSession(integrationId);
    }
}
