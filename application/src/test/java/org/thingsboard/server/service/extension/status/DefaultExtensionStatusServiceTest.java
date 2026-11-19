// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ExtensionStatus;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link DefaultExtensionStatusService#describe} (which computes a fresh snapshot on
 * a registry-cache miss). Focus: the {@code lastError} display contract — a recovered (ACTIVE)
 * extension must surface no error, while a still-degraded one must keep its last known error even when
 * the live reason is momentarily null.
 */
class DefaultExtensionStatusServiceTest {

    private final ExtensionReconciler reconciler = mock(ExtensionReconciler.class);

    private DefaultExtensionStatusService service() {
        return service(mock(ExtensionService.class), mock(PartitionService.class));
    }

    private DefaultExtensionStatusService service(ExtensionService extensionService, PartitionService partitionService) {
        // @PostConstruct (scheduler start) is not invoked for a plain `new`, so no poll loop runs here.
        return new DefaultExtensionStatusService(
                reconciler,
                extensionService,
                partitionService,
                "/api/extension/route",
                10_000L,
                2,
                0L,      // log cache disabled: these cases exercise status, not the log read path
                131_072,
                8L * 1024 * 1024,
                4,
                20_000L);
    }

    private Extension extension(ExtensionState state, String storedError, Long storedErrorAt) {
        Extension ext = new Extension(new ExtensionId(UUID.randomUUID()));
        ext.setTenantId(new TenantId(UUID.randomUUID()));
        ext.setSlug("whoami-1d61");
        ext.setName("Whoami Test");
        ext.setState(state);
        ext.setLastError(storedError);
        ext.setLastErrorAt(storedErrorAt);
        return ext;
    }

    private DeploymentHealth health(boolean exists, int desired, int ready, int updated, String reason) {
        return new DeploymentHealth(exists, desired, ready, updated, Map.of("Running", ready), reason);
    }

    @Test
    void recoveredActiveExtensionShowsNoStaleError() {
        // Pod crash-looped (stored FAILED + "CrashLoopBackOff"), then self-healed to 2/2 ready with no
        // live error. The derived state is ACTIVE, so the stale stored error must NOT be surfaced.
        Extension ext = extension(ExtensionState.FAILED, "CrashLoopBackOff", 123L);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null));

        ExtensionStatus status = service().describe(ext.getTenantId(), ext);

        assertThat(status.getState()).isEqualTo(ExtensionState.ACTIVE);
        assertThat(status.getLastError()).isNull();
        assertThat(status.getLastErrorAt()).isNull();
        assertThat(status.getReadyReplicas()).isEqualTo(2);
        assertThat(status.getDesiredReplicas()).isEqualTo(2);
    }

    @Test
    void failedExtensionSurfacesLiveReason() {
        // A live error reason always wins and is reported with the stored error timestamp.
        Extension ext = extension(ExtensionState.PROVISIONING, null, null);
        ext.setLastErrorAt(777L);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 0, 0, "ImagePullBackOff"));

        ExtensionStatus status = service().describe(ext.getTenantId(), ext);

        assertThat(status.getState()).isEqualTo(ExtensionState.FAILED);
        assertThat(status.getLastError()).isEqualTo("ImagePullBackOff");
        assertThat(status.getLastErrorAt()).isEqualTo(777L);
    }

    @Test
    void stillDegradedExtensionKeepsStoredErrorWhenReasonMomentarilyNull() {
        // Partial readiness, fully rolled out => DEGRADED with no live reason. We must NOT blank the
        // last known error on an extension that is genuinely still degraded.
        Extension ext = extension(ExtensionState.DEGRADED, "previously crashed", 555L);
        when(reconciler.describe(any())).thenReturn(health(true, 3, 2, 3, null));

        ExtensionStatus status = service().describe(ext.getTenantId(), ext);

        assertThat(status.getState()).isEqualTo(ExtensionState.DEGRADED);
        assertThat(status.getLastError()).isEqualTo("previously crashed");
        assertThat(status.getLastErrorAt()).isEqualTo(555L);
    }

    @Test
    void stoppedExtensionWithNoReasonHasNoLiveError() {
        // Scaled to zero: STOPPED, no live reason. The (non-healthy) fallback keeps any stored error,
        // but with none stored there is nothing to show.
        Extension ext = extension(ExtensionState.STOPPED, null, null);
        when(reconciler.describe(any())).thenReturn(health(true, 0, 0, 0, null));

        ExtensionStatus status = service().describe(ext.getTenantId(), ext);

        assertThat(status.getState()).isEqualTo(ExtensionState.STOPPED);
        assertThat(status.getLastError()).isNull();
        assertThat(status.getLastErrorAt()).isNull();
    }

    // The poll reads the row, then spends a round-trip in describe(), then writes a state derived from
    // that pre-describe snapshot. A user stop committed inside that window must not be overwritten, so
    // the write is conditional on the state the poll actually observed — never a blind last-writer-wins.
    @Test
    void pollPersistsConditionallyOnThePreDescribeState() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.PROVISIONING, null, null);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(true);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null)); // pods came up -> ACTIVE

        service.register(ext);
        service.pollAll();

        verify(extensionService).updateStateIf(ext.getTenantId(), ext.getId(),
                ExtensionState.PROVISIONING, ExtensionState.ACTIVE, null);
        verify(extensionService, never()).updateState(any(), any(), any(), any());
    }

    // A list page must cost a bounded number of substrate calls, not two per row. Rows this node owns are
    // served from the poller's cache; the rest go out in ONE batched call, never describe() per row.
    @Test
    void listResolvesUncachedRowsInASingleBatchedCall() {
        DefaultExtensionStatusService service = service();
        Extension a = extension(ExtensionState.PROVISIONING, null, null);
        Extension b = extension(ExtensionState.PROVISIONING, null, null);
        b.setSlug("billing-4c2f");
        when(reconciler.describeAll(any())).thenReturn(Map.of(
                a.getSlug(), health(true, 2, 2, 2, null),
                b.getSlug(), health(true, 1, 1, 1, null)));

        Map<ExtensionId, ExtensionStatus> statuses = service.describeAll(a.getTenantId(), List.of(a, b));

        assertThat(statuses).containsOnlyKeys(a.getId(), b.getId());
        verify(reconciler, never()).describe(any());
        verify(reconciler, times(1)).describeAll(any());
    }

    // One unreachable substrate must not fail the whole page: the rows simply carry no live snapshot and
    // the caller falls back to what the DB row already says.
    @Test
    void listSurvivesASubstrateFailure() {
        DefaultExtensionStatusService service = service();
        Extension ext = extension(ExtensionState.ACTIVE, null, null);
        when(reconciler.describeAll(any())).thenThrow(new RuntimeException("api server unavailable"));

        Map<ExtensionId, ExtensionStatus> statuses = service.describeAll(ext.getTenantId(), List.of(ext));

        assertThat(statuses).isEmpty();
    }

    // Rows are read in ONE bulk query per pass. The per-id form cost a SELECT per registered extension
    // every poll interval — with no cache in front of the extension DAO, that is the poller's dominant
    // cost on a node owning hundreds of them.
    @Test
    void pollReadsEveryRowInOneQuery() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension a = extension(ExtensionState.ACTIVE, null, null);
        Extension b = extension(ExtensionState.ACTIVE, null, null);
        b.setSlug("billing-4c2f");
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(a, b));

        service.register(a);
        service.register(b);
        service.pollAll();

        verify(extensionService, times(1)).findExtensionsByIds(any());
        verify(extensionService, never()).findExtensionById(any(), any());
    }

    // A row missing from the bulk answer means the extension is gone, so the poller unregisters it. A
    // FAILED query returns nothing either — reading that as "all gone" would silently stop polling every
    // extension on the node the first time the database hiccups.
    @Test
    void failedRowReadKeepsRegistrations() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.ACTIVE, null, null);
        when(extensionService.findExtensionsByIds(any())).thenThrow(new RuntimeException("db down"));

        service.register(ext);
        service.pollAll();

        assertThat(service.registeredIds()).containsExactly(ext.getId());
        verify(reconciler, never()).describeAll(any());
    }

    // A non-owner node computes the snapshot for display but must never write transitions.
    @Test
    void nonOwnerPollDoesNotPersist() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.PROVISIONING, null, null);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(false);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null));

        service.register(ext);
        service.pollAll();

        verify(extensionService, never()).updateStateIf(any(), any(), any(), any(), any());
        verify(extensionService, never()).updateState(any(), any(), any(), any());
    }

    // The blink this whole debounce exists for: one replica crash-loops behind replicas that are still
    // serving, and its reason is only visible between restarts. The hysteresis used to guard the WRITE
    // only, while the cached snapshot — what GET /status and the list page serve — took the raw state
    // every pass, so the UI alternated Active/Failed at the poll rate. A single deviating poll must
    // change neither the reported state nor the reported error.
    @Test
    void oneOffSignalDoesNotChangeTheReportedStatus() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.ACTIVE, null, null);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(true);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null));

        service.register(ext);
        service.pollAll(); // ACTIVE — the state to hold

        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, "CrashLoopBackOff"));
        service.pollAll(); // the blip

        ExtensionStatus held = service.describe(ext.getTenantId(), ext);
        assertThat(held.getState()).isEqualTo(ExtensionState.ACTIVE);
        assertThat(held.getLastError()).isNull();
        verify(extensionService, never()).updateStateIf(any(), any(), any(), any(), any());
    }

    // ...and the other half of the contract: a signal that HOLDS is reported, both to the reader and to
    // the row, once it has survived the hysteresis window.
    @Test
    void aPersistentSignalIsReportedAfterTheHysteresisWindow() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.ACTIVE, null, null);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(true);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null));

        service.register(ext);
        service.pollAll();

        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, "CrashLoopBackOff"));
        service.pollAll();
        service.pollAll(); // hysteresis = 2 consecutive observations

        ExtensionStatus status = service.describe(ext.getTenantId(), ext);
        assertThat(status.getState()).isEqualTo(ExtensionState.FAILED);
        assertThat(status.getLastError()).isEqualTo("CrashLoopBackOff");
        verify(extensionService).updateStateIf(ext.getTenantId(), ext.getId(),
                ExtensionState.ACTIVE, ExtensionState.FAILED, "CrashLoopBackOff");
    }

    // The reconciler wrote FAILED "apply failed: ..." because the NEW spec never reached the cluster. The
    // workload the poller sees is the OLD one, still healthy, and says nothing about the desired spec.
    // The poller used to derive ACTIVE from it and persist FAILED -> ACTIVE with a null error: the user's
    // new spec never ran, the error text was gone and the UI showed Active until the next resync failed
    // again — a FAILED/ACTIVE flap at the resync rate.
    @Test
    void reconcilerApplyFailureIsNotErasedByAHealthyOldWorkload() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.FAILED,
                ExtensionStateDeriver.APPLY_FAILED_PREFIX + "Unresolved secret reference(s): db-pass", 123L);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(true);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null)); // old pods, all ready

        service.register(ext);
        service.pollAll();
        service.pollAll();

        ExtensionStatus status = service.describe(ext.getTenantId(), ext);
        assertThat(status.getState()).isEqualTo(ExtensionState.FAILED);
        assertThat(status.getLastError()).startsWith(ExtensionStateDeriver.APPLY_FAILED_PREFIX);
        assertThat(status.getLastErrorAt()).isEqualTo(123L);
        verify(extensionService, never()).updateStateIf(any(), any(), any(), any(), any());
    }

    // A hold is a display-side debounce of the live reading, never a value for the row. While the poller
    // held ACTIVE through the hysteresis window it used to write that hold onto a row the reconciler had
    // meanwhile moved (PROVISIONING after a re-save whose pods now ImagePullBackOff), reverting the
    // writer and blanking its error. Only a STABLE derived state may be persisted.
    @Test
    void aDebounceHoldIsNeverWrittenToTheRow() {
        ExtensionService extensionService = mock(ExtensionService.class);
        PartitionService partitionService = mock(PartitionService.class);
        DefaultExtensionStatusService service = service(extensionService, partitionService);

        Extension ext = extension(ExtensionState.ACTIVE, null, null);
        when(extensionService.findExtensionsByIds(any())).thenReturn(List.of(ext));
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(true);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 2, 2, null));

        service.register(ext);
        service.pollAll(); // ACTIVE — the state to hold

        // user re-saves: the reconciler moves the row to PROVISIONING; the new pods cannot pull
        ext.setState(ExtensionState.PROVISIONING);
        when(reconciler.describe(any())).thenReturn(health(true, 2, 0, 2, "ImagePullBackOff"));
        service.pollAll(); // derived FAILED, held as ACTIVE

        // the hold (ACTIVE) must not have been written over PROVISIONING
        verify(extensionService, never()).updateStateIf(any(), any(), any(), eq(ExtensionState.ACTIVE), any());

        service.pollAll(); // second observation: FAILED is stable now
        verify(extensionService).updateStateIf(ext.getTenantId(), ext.getId(),
                ExtensionState.PROVISIONING, ExtensionState.FAILED, "ImagePullBackOff");
    }

}
