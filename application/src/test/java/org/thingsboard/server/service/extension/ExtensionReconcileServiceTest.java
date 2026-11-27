// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension;

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;
import org.thingsboard.server.service.extension.status.ExtensionStateDeriver;
import org.thingsboard.server.service.extension.status.ExtensionStatusService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the cluster-broadcast reconcile trigger ({@link ExtensionReconcileService#onComponentLifecycleMsg}).
 * Every core node receives the broadcast; only the partition owner may act, and it must act on a fresh
 * DB row (not a serialized snapshot). This is the single-node simulation of the multi-node contract.
 */
class ExtensionReconcileServiceTest {

    private final ExtensionReconciler reconciler = mock(ExtensionReconciler.class);
    private final ExtensionStatusService statusService = mock(ExtensionStatusService.class);
    private final ExtensionService extensionService = mock(ExtensionService.class);
    private final PartitionService partitionService = mock(PartitionService.class);
    private final SecretConfigurationService secretConfigurationService = mock(SecretConfigurationService.class);
    private final SecretService secretService = mock(SecretService.class);

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final ExtensionId extensionId = new ExtensionId(UUID.randomUUID());

    private ExtensionReconcileService service;

    @BeforeEach
    void setUp() {
        // secret resolution is identity in unit tests: the reconcile path passes the same fresh row through
        when(secretConfigurationService.replaceSecretUsages(any(), any(Extension.class), eq(Extension.class)))
                .thenAnswer(inv -> inv.getArgument(1));
        // resync interval 0: no periodic resync in unit tests, resyncOwned() is called directly
        // every referenced secret resolves unless a test says otherwise
        when(secretService.findSecretByName(any(), any())).thenReturn(new Secret());
        service = new ExtensionReconcileService(
                Optional.of(reconciler), Optional.of(statusService), extensionService, partitionService,
                secretConfigurationService, secretService, 0);
        service.init();
    }

    @AfterEach
    void tearDown() {
        service.destroy();
    }

    private Extension extension(ExtensionState state) {
        Extension ext = new Extension(extensionId);
        ext.setTenantId(tenantId);
        ext.setSlug("whoami-1d61");
        ext.setState(state);
        return ext;
    }

    private void ownPartition(boolean owned) {
        when(partitionService.isMyPartition(ServiceType.TB_CORE, tenantId, extensionId)).thenReturn(owned);
    }

    @Test
    void ownerAppliesFreshRowFromDb() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.PROVISIONING);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).apply(fresh);
        verify(statusService, timeout(5000)).register(fresh);
    }

    @Test
    void nonOwnerIgnoresLifecycleMsg() {
        ownPartition(false);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(extensionService, never()).findExtensionById(any(), any());
        verifyNoInteractions(reconciler);
    }

    @Test
    void stoppedRowScalesToZeroAndUnregistersPolling() {
        ownPartition(true);
        Extension stopped = extension(ExtensionState.STOPPED);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(stopped);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).scaleTo(stopped, 0);
        verify(statusService, timeout(5000)).unregister(extensionId);
    }

    // A failed stop must keep STOPPED: a FAILED row would be full-applied by the next resync,
    // restarting the workload the user stopped.
    @Test
    void failedStopKeepsStoppedStateAndOnlyRecordsTheError() {
        ownPartition(true);
        Extension stopped = extension(ExtensionState.STOPPED);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(stopped);
        doThrow(new RuntimeException("api server down")).when(reconciler).scaleTo(stopped, 0);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(extensionService, timeout(5000)).updateStateIf(eq(tenantId), eq(extensionId),
                eq(ExtensionState.STOPPED), eq(ExtensionState.STOPPED), any());
        verify(extensionService, never()).updateStateIf(any(), any(), any(), eq(ExtensionState.FAILED), any());
        // Unregistered on a failed stop too: derive() pins STOPPED, so polling has nothing to report, and
        // an entry kept alive here would carry published=STOPPED into the user's next start and revert it.
        verify(statusService, timeout(5000)).unregister(extensionId);
    }

    // A "stop failed: ..." text stays on the row after the next stop (the resync's retry) succeeds — the
    // poller cannot repair it, STOPPED == STOPPED is never a transition for it. The reconciler clears it.
    @Test
    void successfulStopClearsAStaleStopError() {
        ownPartition(true);
        Extension stopped = extension(ExtensionState.STOPPED);
        stopped.setLastError(ExtensionStateDeriver.STOP_FAILED_PREFIX + "api server down");
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(stopped);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).scaleTo(stopped, 0);
        verify(extensionService, timeout(5000)).updateStateIf(tenantId, extensionId,
                ExtensionState.STOPPED, ExtensionState.STOPPED, null);
    }

    // The status deriver pins a FAILED row whose error the reconciler wrote (the desired spec never
    // reached the cluster), because the poller cannot tell the old healthy workload from the new one.
    // Only the reconciler knows when the spec HAS reached the cluster, so it lifts the pin itself.
    @Test
    void successfulApplyClearsAReconcilerRecordedFailure() {
        ownPartition(true);
        Extension failed = extension(ExtensionState.FAILED);
        failed.setLastError(ExtensionStateDeriver.APPLY_FAILED_PREFIX + "Unresolved secret reference(s): db-pass");
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(failed);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).apply(failed);
        verify(extensionService, timeout(5000)).updateStateIf(tenantId, extensionId,
                ExtensionState.FAILED, ExtensionState.PROVISIONING, null);
    }

    // ...but a FAILED the POLLER wrote (a live pod reason) is not the reconciler's to clear: the poller
    // reports the recovery itself once the workload is healthy.
    @Test
    void successfulApplyLeavesAPollerRecordedFailureToThePoller() {
        ownPartition(true);
        Extension failed = extension(ExtensionState.FAILED);
        failed.setLastError("CrashLoopBackOff");
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(failed);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).apply(failed);
        verify(statusService, timeout(5000)).register(failed);
        verify(extensionService, never()).updateStateIf(any(), any(), any(), any(), any());
    }

    // The running/pending handoff must be one atomic step. With two separate flags a request that landed
    // between the runner's "no rerun" check and its guard release was lost. Hammer the boundary: many
    // concurrent requests, then assert the guard is fully released — a fresh request must run a pass.
    @Test
    void resyncGuardIsAlwaysReleasedAfterConcurrentRequests() throws Exception {
        when(partitionService.isMyPartition(eq(ServiceType.TB_CORE), eq(tenantId), any())).thenReturn(true);
        when(reconciler.listManagedSlugs()).thenReturn(Set.of());
        AtomicInteger scans = new AtomicInteger();
        when(extensionService.findAllExtensions(any())).thenAnswer(inv -> {
            scans.incrementAndGet();
            return new PageData<Extension>(List.of(), 0, 0, false);
        });

        for (int round = 0; round < 50; round++) {
            Thread[] callers = new Thread[8];
            for (int i = 0; i < callers.length; i++) {
                callers[i] = new Thread(service::resyncOwned);
                callers[i].start();
            }
            for (Thread caller : callers) {
                caller.join(5000);
            }
        }

        int before = scans.get();
        service.resyncOwned(); // nothing running: MUST scan, i.e. the guard was released every time
        assertThat(scans.get()).isEqualTo(before + 1);
    }

    // Two resync requests landing while a pass runs used to be dropped outright; the rows a partition
    // change handed this node mid-pass were then applied only by the next periodic pass — or never, with
    // resync-interval-ms=0. A request that arrives during a pass must run once that pass is done.
    @Test
    void resyncRequestedDuringAPassRunsAgainAfterIt() throws Exception {
        when(partitionService.isMyPartition(eq(ServiceType.TB_CORE), eq(tenantId), any())).thenReturn(true);
        when(reconciler.listManagedSlugs()).thenReturn(Set.of());
        CountDownLatch firstScanStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstScan = new CountDownLatch(1);
        AtomicInteger scans = new AtomicInteger();
        when(extensionService.findAllExtensions(any())).thenAnswer(inv -> {
            if (scans.incrementAndGet() == 1) {
                firstScanStarted.countDown();
                releaseFirstScan.await(5, TimeUnit.SECONDS);
            }
            return new PageData<Extension>(List.of(), 0, 0, false);
        });

        Thread first = new Thread(service::resyncOwned);
        first.start();
        assertThat(firstScanStarted.await(5, TimeUnit.SECONDS)).isTrue();

        service.resyncOwned(); // arrives mid-pass: returns at once, but must not be forgotten
        assertThat(scans.get()).isEqualTo(1);

        releaseFirstScan.countDown();
        first.join(5000);

        assertThat(scans.get()).isEqualTo(2);
    }

    @Test
    void restartInfoMarkerRoutesToRestartNotApply() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.ACTIVE);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);

        service.onComponentLifecycleMsg(ComponentLifecycleMsg.builder()
                .tenantId(tenantId)
                .entityId(extensionId)
                .event(ComponentLifecycleEvent.UPDATED)
                .info(JacksonUtil.newObjectNode().put("action", ExtensionReconcileService.RESTART_ACTION))
                .build());

        verify(reconciler, timeout(5000)).restart(fresh);
        verify(reconciler, never()).apply(any());
    }

    @Test
    void deletedMsgOnlyDropsStatusPolling() {
        // teardown runs on the node that handled the delete; the broadcast exists so the owner
        // stops polling a deployment that is gone (the row cannot be re-read anymore)
        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.DELETED));

        verify(statusService, timeout(5000)).unregister(extensionId);
        verifyNoInteractions(reconciler, extensionService, partitionService);
    }

    @Test
    void missingRowIsIgnored() {
        ownPartition(true);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(null);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verifyNoInteractions(reconciler);
    }

    @Test
    void nonExtensionEntityIsIgnored() {
        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(
                tenantId, new DeviceId(UUID.randomUUID()), ComponentLifecycleEvent.UPDATED));

        verifyNoInteractions(reconciler, extensionService, partitionService);
    }

    @Test
    void applyFailureMarksExtensionFailed() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.PROVISIONING);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);
        doThrow(new RuntimeException("api server down")).when(reconciler).apply(fresh);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(extensionService, timeout(5000)).updateStateIf(eq(tenantId), eq(extensionId), eq(ExtensionState.PROVISIONING), eq(ExtensionState.FAILED), any());
    }

    // Teardown is not partition-gated and runs on whichever node handled the REST delete, while apply
    // runs on the owner; the per-id lock is JVM-local and cannot serialize them. So the row can vanish
    // between the re-read and the multi-call apply, and the creates resurrect a deleted extension's
    // workload. The orphan sweep is not a sufficient net — with resync-interval-ms=0 it only runs on a
    // partition change — so the apply path re-checks and undoes its own work.
    @Test
    void applyUndoesItselfWhenTheRowIsDeletedWhileItIsInFlight() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.ACTIVE);
        // listener read, re-read under the lock, then the post-apply existence check: deleted meanwhile
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh, fresh, null);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).apply(any());
        verify(reconciler, timeout(5000)).delete(fresh);
    }

    @Test
    void applyKeepsItsWorkWhenTheRowStillExists() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.ACTIVE);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).apply(any());
        verify(reconciler, never()).delete(any());
    }

    // R4-4: a stop submitted after a save must win even when the tasks run out of order — the row
    // re-read under the lock decides, not the snapshot captured at submit time.
    @Test
    void applyActsOnRowStateReReadUnderLock() {
        ownPartition(true);
        Extension staleActive = extension(ExtensionState.ACTIVE);
        Extension nowStopped = extension(ExtensionState.STOPPED);
        // first read happens in the lifecycle listener, second read under the reconcile lock
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(staleActive, nowStopped);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(5000)).scaleTo(nowStopped, 0);
        verify(reconciler, never()).apply(any());
    }

    @Test
    void rowDeletedBetweenSubmitAndRunIsSkipped() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.ACTIVE);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh, (Extension) null);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(extensionService, timeout(5000).times(2)).findExtensionById(tenantId, extensionId);
        verifyNoInteractions(reconciler);
    }

    // R4-5: transient k8s failures (5xx, timeouts, IO errors) are retried in-task ...
    @Test
    void transientApplyFailureIsRetriedAndSucceeds() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.PROVISIONING);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);
        // the reconcile brain delegates the transient/terminal decision to the backend
        when(reconciler.isRetryable(any())).thenReturn(true);
        doThrow(new KubernetesClientException("boom", 503, null)).doNothing().when(reconciler).apply(fresh);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(reconciler, timeout(10000).times(2)).apply(fresh);
        verify(statusService, timeout(5000)).register(fresh);
        verify(extensionService, never()).updateStateIf(any(), any(), any(), eq(ExtensionState.FAILED), any());
    }

    // ... while terminal errors (4xx) fail fast without retries.
    @Test
    void terminalApplyFailureIsNotRetried() {
        ownPartition(true);
        Extension fresh = extension(ExtensionState.PROVISIONING);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(fresh);
        doThrow(new KubernetesClientException("forbidden", 403, null)).when(reconciler).apply(fresh);

        service.onComponentLifecycleMsg(new ComponentLifecycleMsg(tenantId, extensionId, ComponentLifecycleEvent.UPDATED));

        verify(extensionService, timeout(5000)).updateStateIf(eq(tenantId), eq(extensionId), eq(ExtensionState.PROVISIONING), eq(ExtensionState.FAILED), any());
        verify(reconciler, times(1)).apply(fresh);
    }

    @Test
    void restartSkippedWhenRowIsStoppedMeanwhile() {
        ownPartition(true);
        Extension active = extension(ExtensionState.ACTIVE);
        Extension stopped = extension(ExtensionState.STOPPED);
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(active, stopped);

        service.onComponentLifecycleMsg(ComponentLifecycleMsg.builder()
                .tenantId(tenantId)
                .entityId(extensionId)
                .event(ComponentLifecycleEvent.UPDATED)
                .info(JacksonUtil.newObjectNode().put("action", ExtensionReconcileService.RESTART_ACTION))
                .build());

        verify(extensionService, timeout(5000).times(2)).findExtensionById(tenantId, extensionId);
        verify(reconciler, never()).restart(any());
    }

    // R4-5: the resync orphan sweep removes k8s objects whose extension row is gone.
    @Test
    void resyncSweepsOrphanedObjectsAndKeepsLiveOnes() {
        Extension live = extension(ExtensionState.ACTIVE);
        ownPartition(true);
        when(reconciler.listManagedSlugs()).thenReturn(Set.of(live.getSlug(), "orphan-slug"));
        when(extensionService.findAllExtensions(any()))
                .thenReturn(new PageData<>(List.of(live), 1, 1, false));
        when(extensionService.findExtensionById(tenantId, extensionId)).thenReturn(live);

        service.resyncOwned();

        verify(reconciler).deleteBySlug("orphan-slug");
        verify(reconciler, never()).deleteBySlug(live.getSlug());
        verify(reconciler).apply(live);
    }

    // Resync applies run on the reconcile pool, several at a time, instead of one after another on the
    // scanning thread. The pass must still be a barrier: everything it submitted is finished by the time
    // it returns, so the orphan sweep and the status-registry pruning that follow see a settled world.
    @Test
    void resyncAppliesEveryOwnedRowAndWaitsForThemAll() {
        List<Extension> owned = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            Extension ext = new Extension(new ExtensionId(UUID.randomUUID()));
            ext.setTenantId(tenantId);
            ext.setSlug("owned-" + i);
            ext.setState(ExtensionState.ACTIVE);
            owned.add(ext);
            when(extensionService.findExtensionById(tenantId, ext.getId())).thenReturn(ext);
        }
        when(partitionService.isMyPartition(eq(ServiceType.TB_CORE), eq(tenantId), any())).thenReturn(true);
        when(reconciler.listManagedSlugs()).thenReturn(Set.of());
        when(extensionService.findAllExtensions(any()))
                .thenReturn(new PageData<>(owned, 1, owned.size(), false));

        service.resyncOwned();

        // No timeout(): a still-running apply after the pass returned is exactly the regression guarded.
        owned.forEach(ext -> verify(reconciler).apply(ext));
    }

}
