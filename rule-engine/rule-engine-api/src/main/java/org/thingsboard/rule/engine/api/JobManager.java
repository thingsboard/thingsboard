// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.msg.queue.TbCallback;

public interface JobManager {

    ListenableFuture<Job> submitJob(Job job);

    ListenableFuture<Job> submitJob(Job job, TbCallback finishCallback);

    void cancelJob(TenantId tenantId, JobId jobId);

    void reprocessJob(TenantId tenantId, JobId jobId);

    void onJobUpdate(Job job);

}
