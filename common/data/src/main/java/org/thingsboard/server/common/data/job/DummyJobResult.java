// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

public class DummyJobResult extends JobResult {

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
