// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import lombok.Data;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.util.ArrayList;
import java.util.List;

@Data
public class JobStats {

    private final TenantId tenantId;
    private final JobId jobId;
    private final List<TaskResult> taskResults = new ArrayList<>();
    private Integer totalTasksCount;

}
