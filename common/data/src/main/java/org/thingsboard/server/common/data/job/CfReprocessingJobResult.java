// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

public class CfReprocessingJobResult extends JobResult {

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
    }

}
