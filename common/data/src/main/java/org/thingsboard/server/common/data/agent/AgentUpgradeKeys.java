// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

public final class AgentUpgradeKeys {

    public static final String IMAGE_REF = "imageRef";
    public static final String OLD_CONTAINER_ID = "oldContainerId";
    public static final String NEW_CONTAINER_ID = "newContainerId";
    public static final String FINALIZE_DEADLINE_TS = "finalizeDeadlineTs";
    public static final String RECONCILED_ACTIVITY = "confirmed applied after timeout";

    private AgentUpgradeKeys() {
    }
}
