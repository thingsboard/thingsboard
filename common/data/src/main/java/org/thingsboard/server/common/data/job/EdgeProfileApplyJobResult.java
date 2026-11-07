// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job;

public class EdgeProfileApplyJobResult extends JobResult {

    @Override
    public JobType getJobType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

}
