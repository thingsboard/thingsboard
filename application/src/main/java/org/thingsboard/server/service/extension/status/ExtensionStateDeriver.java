// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;

public final class ExtensionStateDeriver {

    private ExtensionStateDeriver() {}

    // Prefixes the reconciler puts in front of a stored lastError. They let the deriver tell a failure
    // that the reconciler recorded (the desired spec never reached the cluster) from one the poller
    // recorded from a live pod reason (which the live workload can heal on its own).
    public static final String APPLY_FAILED_PREFIX = "apply failed: ";
    public static final String STOP_FAILED_PREFIX = "stop failed: ";

    public static boolean isApplyFailure(String storedError) {
        return storedError != null && storedError.startsWith(APPLY_FAILED_PREFIX);
    }

    public static boolean isStopFailure(String storedError) {
        return storedError != null && storedError.startsWith(STOP_FAILED_PREFIX);
    }

    public static ExtensionState derive(ExtensionState entityState, DeploymentHealth health, String errorReason) {
        return derive(entityState, health, errorReason, null);
    }

    /**
     * Derives the state to report from the stored row state and the live workload health.
     *
     * <p>{@code storedError} is the row's lastError. A FAILED row whose error was written by the
     * reconciler ({@link #APPLY_FAILED_PREFIX}) is pinned: the workload the poller observes is the OLD
     * one (the new spec never reached the cluster, e.g. an unresolved secret or a substrate timeout), so
     * a healthy reading says nothing about the desired spec. The reconciler lifts the pin itself when a
     * later apply succeeds (FAILED -> PROVISIONING). A FAILED row whose error is a live pod reason is
     * NOT pinned: those heal on their own and the poller must be able to report the recovery.
     */
    public static ExtensionState derive(ExtensionState entityState, DeploymentHealth health, String errorReason,
                                        String storedError) {
        if (entityState == ExtensionState.DELETING) {
            return ExtensionState.DELETING;
        }
        if (entityState == ExtensionState.STOPPED) {
            return ExtensionState.STOPPED;
        }
        if (entityState == ExtensionState.FAILED && isApplyFailure(storedError)) {
            return ExtensionState.FAILED;
        }
        if (health == null || !health.exists()) {
            // a not-yet-observed Deployment during PROVISIONING is still coming up, not degraded; a
            // FAILED row with no workload is still failed (an initial apply that never created one) —
            // DEGRADED would claim a workload that exists but is unhealthy
            if (entityState == ExtensionState.PROVISIONING || entityState == ExtensionState.FAILED) {
                return entityState;
            }
            return ExtensionState.DEGRADED;
        }
        if (errorReason != null && !errorReason.isBlank()) {
            return ExtensionState.FAILED;
        }
        int desired = health.desiredReplicas();
        int ready = health.readyReplicas();
        if (desired == 0) {
            return ExtensionState.STOPPED;
        }
        if (ready >= desired) {
            return ExtensionState.ACTIVE;
        }
        if (ready == 0) {
            return ExtensionState.PROVISIONING;
        }
        // partial readiness: a progressing rollout is UPDATING, otherwise DEGRADED
        if (health.updatedReplicas() < desired) {
            return ExtensionState.UPDATING;
        }
        return ExtensionState.DEGRADED;
    }

    public static String effectiveError(ExtensionState state, String liveReason, String storedError) {
        // ACTIVE carries no error at all — neither a stale stored one nor a live reason. derive() cannot
        // produce ACTIVE while a reason is present, but the status service can: it holds the last stable
        // state through a flickering signal, and an ACTIVE hold showing "CrashLoopBackOff" would just
        // move the blink from the state into the message.
        if (state == ExtensionState.ACTIVE) {
            return null;
        }
        if (liveReason != null && !liveReason.isBlank()) {
            return liveReason;
        }
        return storedError;
    }

}
