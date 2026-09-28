// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

public enum AgentProcessingStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    FINISHED,
    ERROR,

    START_FAILED;

    public boolean isTerminated() {
        return this == FINISHED || this == ERROR || this == START_FAILED;
    }
}
