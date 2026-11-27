// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionStateDeriverTest {

    private DeploymentHealth health(boolean exists, int desired, int ready, int updated, String reason) {
        return new DeploymentHealth(exists, desired, ready, updated, Map.of(), reason);
    }

    @Test
    void deletingAndStoppedAreSticky() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.DELETING, health(true, 2, 2, 2, null), null)).isEqualTo(ExtensionState.DELETING);
        assertThat(ExtensionStateDeriver.derive(ExtensionState.STOPPED, health(true, 0, 0, 0, null), null)).isEqualTo(ExtensionState.STOPPED);
    }

    @Test
    void missingDeploymentIsDegradedWhenWasActive() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.ACTIVE, DeploymentHealth.missing(), null)).isEqualTo(ExtensionState.DEGRADED);
    }

    @Test
    void missingDeploymentStaysProvisioningWhileComingUp() {
        // A fresh extension whose Deployment hasn't been observed yet must not flap to DEGRADED.
        assertThat(ExtensionStateDeriver.derive(ExtensionState.PROVISIONING, DeploymentHealth.missing(), null))
                .isEqualTo(ExtensionState.PROVISIONING);
    }

    @Test
    void missingDeploymentKeepsFailed() {
        // An initial apply that failed before any workload was created: the row is FAILED and nothing
        // exists in the cluster. DEGRADED would claim a workload that is there but unhealthy.
        assertThat(ExtensionStateDeriver.derive(ExtensionState.FAILED, DeploymentHealth.missing(), null))
                .isEqualTo(ExtensionState.FAILED);
    }

    @Test
    void reconcilerApplyFailureIsStickyWhileTheOldWorkloadIsHealthy() {
        // The reconciler wrote FAILED "apply failed: ..." because the NEW spec never reached the cluster
        // (unresolved secret, substrate timeout). The workload the poller sees is the OLD one, so a
        // healthy reading says nothing about the desired spec and must not flip the row to ACTIVE.
        String applyError = ExtensionStateDeriver.APPLY_FAILED_PREFIX + "Unresolved secret reference(s): db-pass";
        assertThat(ExtensionStateDeriver.derive(ExtensionState.FAILED, health(true, 2, 2, 2, null), null, applyError))
                .isEqualTo(ExtensionState.FAILED);
        assertThat(ExtensionStateDeriver.derive(ExtensionState.FAILED, DeploymentHealth.missing(), null, applyError))
                .isEqualTo(ExtensionState.FAILED);
    }

    @Test
    void pollerRecordedFailureRecoversWithTheWorkload() {
        // The poller wrote FAILED from a live pod reason (CrashLoopBackOff). That failure heals on its
        // own, so a healthy workload IS the desired one and the recovery must be reported.
        assertThat(ExtensionStateDeriver.derive(ExtensionState.FAILED, health(true, 2, 2, 2, null), null, "CrashLoopBackOff"))
                .isEqualTo(ExtensionState.ACTIVE);
    }

    @Test
    void pullOrCrashReasonIsFailed() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.PROVISIONING, health(true, 2, 0, 0, "ImagePullBackOff"), "ImagePullBackOff"))
                .isEqualTo(ExtensionState.FAILED);
    }

    @Test
    void readyEqualsDesiredIsActive() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.PROVISIONING, health(true, 2, 2, 2, null), null)).isEqualTo(ExtensionState.ACTIVE);
    }

    @Test
    void zeroReadyIsProvisioning() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.PROVISIONING, health(true, 2, 0, 0, null), null)).isEqualTo(ExtensionState.PROVISIONING);
    }

    @Test
    void partialReadyDuringRolloutIsUpdating() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.ACTIVE, health(true, 3, 1, 1, null), null)).isEqualTo(ExtensionState.UPDATING);
    }

    @Test
    void partialReadyFullyUpdatedIsDegraded() {
        assertThat(ExtensionStateDeriver.derive(ExtensionState.ACTIVE, health(true, 3, 2, 3, null), null)).isEqualTo(ExtensionState.DEGRADED);
    }

    // --- effectiveError: the lastError display contract shared by both status read-paths ---

    @Test
    void activeExtensionNeverSurfacesStoredError() {
        // FAILED -> ACTIVE recovery: live reason is null, stored error is stale -> show nothing.
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.ACTIVE, null, "CrashLoopBackOff")).isNull();
    }

    @Test
    void liveReasonWinsOnEveryStateButActive() {
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.FAILED, "ImagePullBackOff", "old")).isEqualTo("ImagePullBackOff");
        // ACTIVE reports no error at all, live reason included. derive() never pairs the two, but the
        // status service does: it holds the last stable state through a flickering signal, and an ACTIVE
        // row showing "CrashLoopBackOff" would move the blink out of the state and into the message.
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.ACTIVE, "weird", "old")).isNull();
    }

    @Test
    void nonActiveStateKeepsStoredErrorWhenReasonNull() {
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.DEGRADED, null, "previously crashed")).isEqualTo("previously crashed");
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.FAILED, null, "boom")).isEqualTo("boom");
    }

    @Test
    void blankLiveReasonFallsBackToStored() {
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.DEGRADED, "   ", "stored")).isEqualTo("stored");
        // ...and on an ACTIVE extension a blank reason still yields no error.
        assertThat(ExtensionStateDeriver.effectiveError(ExtensionState.ACTIVE, "   ", "stored")).isNull();
    }

}
