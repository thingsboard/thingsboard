// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.subscription;

public enum SubscriptionEntry {

    DEVICE_COUNT(1),
    ASSET_COUNT(2),
    EDGE_COUNT(3),
    WHITE_LABELING(9),
    INTEGRATIONS(10),
    SCHEDULER(11),
    REPORTING(12),
    AGENT_COUNT(13);

    private int entryCode;

    SubscriptionEntry(int entryCode) {
        this.entryCode = entryCode;
    }
}
