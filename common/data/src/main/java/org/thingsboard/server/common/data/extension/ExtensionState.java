// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

public enum ExtensionState {

    PROVISIONING,
    ACTIVE,
    UPDATING,
    DEGRADED,
    FAILED,
    STOPPED,
    /**
     * Reserved for a two-phase delete. NOTE: no code path writes it today — {@code deleteExtension}
     * removes the row inside its transaction and lets the post-commit event tear the workload down, so
     * every read of this constant is a defensive guard that currently cannot fire. Keep that in mind
     * before relying on it as a quota grace period or as an in-progress-delete signal.
     */
    DELETING

}
