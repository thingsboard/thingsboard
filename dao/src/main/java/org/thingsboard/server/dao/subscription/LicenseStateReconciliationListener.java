// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

/**
 * Declared here rather than beside its implementation because the node-local state it drops lives in a module
 * that depends on this one, and the drop originates here.
 */
public interface LicenseStateReconciliationListener {

    /**
     * This node's licence state was reconverged from the cluster rather than through a local request, so the
     * node-local latches this service keeps beside it have to be dropped too. {@code licenseSecretStored} is a
     * one-way latch: left set, {@code pollClaim()} answers ACTIVATED on a node whose {@code getState()} answers
     * LICENSE_REQUIRED, and the setup wizard alternates between them until the node restarts.
     */
    void onLicenseStateReconciled();

}
