// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.subscription.LicenseStateChangedEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateType;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Turns this node's licence change into the cluster-wide reconverge signal. The seam between {@code dao},
 * which owns the licence state and cannot see the cluster service, and the queue.
 * <p>
 * Both service types, not core alone: a licence verdict is read only inside this module, and this module runs
 * on every REST node and every rule engine node. {@code DefaultDashboardReportService} reads
 * {@code isDevelopment} and stamps the report bytes itself, and one of its two callers is
 * {@code TbGenerateReportNode}.
 * <p>
 * A monolith therefore receives the signal twice - it runs both consumers and its service id is in both
 * {@code getAllServiceIds} sets. That is deliberate and harmless: the second arrival either loses the
 * {@code tryLock} in {@code reconcileLicenseState()} or finds the stored secret already applied and returns.
 * There is no cheaper option either, because {@link TbClusterService} exposes no broadcast primitive that
 * reaches each node once regardless of service type.
 * <p>
 * <b>Its own thread, deliberately.</b> A plain {@code @EventListener} runs on the publishing thread, and both
 * publishers hold {@code BasicLicenseActivationService}'s activation lock. The fan-out is one queue send per peer
 * service id, and the Kafka implementation blocks on topic creation and on producer metadata - so running it
 * inline would hold that lock across a broker outage and queue every activation path behind it. One thread
 * rather than a pool: it bounds how many fan-outs run at once and keeps the publisher off the queue. Ordering
 * is not the reason - the signal is bare and its receiver reads the current state.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LicenseUpdateBroadcaster {

    private final TbClusterService clusterService;

    private ExecutorService broadcastExecutor;

    @PostConstruct
    public void init() {
        broadcastExecutor = Executors.newSingleThreadExecutor(
                ThingsBoardThreadFactory.forName("license-update-broadcast"));
    }

    @PreDestroy
    public void stop() {
        if (broadcastExecutor != null) {
            broadcastExecutor.shutdownNow();
        }
    }

    @EventListener
    public void onLicenseStateChanged(LicenseStateChangedEvent event) {
        broadcastExecutor.submit(this::broadcast);
    }

    private void broadcast() {
        log.info("Broadcasting a license change to the cluster.");
        SystemUpdateMsg systemUpdateMsg = SystemUpdateMsg.newBuilder()
                .setType(SystemUpdateType.LICENSE)
                .build();
        // Independently, so one unreachable topic does not swallow the other service type's signal: the queue
        // send rethrows, and these are two recipients rather than two steps of one delivery.
        send(() -> clusterService.broadcastToCore(ToCoreNotificationMsg.newBuilder()
                .setSystemUpdateMsg(systemUpdateMsg)
                .build()), ServiceType.TB_CORE);
        send(() -> clusterService.broadcastToRuleEngine(ToRuleEngineNotificationMsg.newBuilder()
                .setSystemUpdateMsg(systemUpdateMsg)
                .build()), ServiceType.TB_RULE_ENGINE);
    }

    private void send(Runnable broadcast, ServiceType serviceType) {
        try {
            broadcast.run();
        } catch (Exception e) {
            log.warn("Failed to broadcast the license change to {} nodes. They will pick it up on their next " +
                    "reconciliation tick.", serviceType, e);
        }
    }
}
