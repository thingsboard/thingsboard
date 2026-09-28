// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.dao.subscription.LicenseStateChangedEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateType;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class LicenseUpdateBroadcasterTest {

    @Test
    public void broadcastsABareLicenseSignalToBothServiceTypes() {
        // Both, not core alone: DefaultDashboardReportService reads isDevelopment and stamps the bytes itself,
        // and one of its callers is TbGenerateReportNode - a rule node.
        TbClusterService clusterService = mock(TbClusterService.class);
        LicenseUpdateBroadcaster broadcaster = new LicenseUpdateBroadcaster(clusterService);
        broadcaster.init();
        try {
            broadcaster.onLicenseStateChanged(new LicenseStateChangedEvent());

            ArgumentCaptor<ToCoreNotificationMsg> toCore = ArgumentCaptor.forClass(ToCoreNotificationMsg.class);
            ArgumentCaptor<ToRuleEngineNotificationMsg> toRuleEngine =
                    ArgumentCaptor.forClass(ToRuleEngineNotificationMsg.class);
            verify(clusterService, timeout(5000)).broadcastToCore(toCore.capture());
            verify(clusterService, timeout(5000)).broadcastToRuleEngine(toRuleEngine.capture());

            assertThat(toCore.getValue().getSystemUpdateMsg().getType()).isEqualTo(SystemUpdateType.LICENSE);
            assertThat(toRuleEngine.getValue().getSystemUpdateMsg().getType()).isEqualTo(SystemUpdateType.LICENSE);
        } finally {
            broadcaster.stop();
        }
    }

    @Test
    public void aFailedCoreFanOutStillReachesTheRuleEngine() {
        // TbKafkaProducerTemplate.send rethrows, so one unreachable topic must not swallow the other service
        // type's signal - the two are independent recipients, not two steps of one delivery.
        TbClusterService clusterService = mock(TbClusterService.class);
        doThrow(new RuntimeException("broker down")).when(clusterService).broadcastToCore(any());
        LicenseUpdateBroadcaster broadcaster = new LicenseUpdateBroadcaster(clusterService);
        broadcaster.init();
        try {
            broadcaster.onLicenseStateChanged(new LicenseStateChangedEvent());

            verify(clusterService, timeout(5000)).broadcastToRuleEngine(any());
        } finally {
            broadcaster.stop();
        }
    }

    @Test
    public void doesNotBroadcastOnThePublishingThread() {
        // The two emit sites hold BasicLicenseActivationService's activation lock, and the fan-out makes
        // blocking Kafka calls. Holding that monitor across them would queue every entitlement path behind a
        // broker outage.
        TbClusterService clusterService = mock(TbClusterService.class);
        LicenseUpdateBroadcaster broadcaster = new LicenseUpdateBroadcaster(clusterService);
        broadcaster.init();
        try {
            Thread publishing = Thread.currentThread();
            AtomicReference<Thread> broadcasting = new AtomicReference<>();
            doAnswer(invocation -> {
                broadcasting.set(Thread.currentThread());
                return null;
            }).when(clusterService).broadcastToCore(any());

            broadcaster.onLicenseStateChanged(new LicenseStateChangedEvent());

            await().atMost(5, TimeUnit.SECONDS).untilAtomic(broadcasting, notNullValue());
            assertThat(broadcasting.get()).isNotSameAs(publishing);
        } finally {
            broadcaster.stop();
        }
    }
}
