// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

public enum JobStatus {

    QUEUED,
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isOneOf(JobStatus... statuses) {
        if (statuses == null) {
            return false;
        }
        for (JobStatus status : statuses) {
            if (this == status) {
                return true;
            }
        }
        return false;
    }

}
