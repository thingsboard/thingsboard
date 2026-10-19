// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;

public interface JobManager {

    ListenableFuture<Job> submitJob(Job job); // TODO: rate limits

    void cancelJob(TenantId tenantId, JobId jobId);

    void reprocessJob(TenantId tenantId, JobId jobId);

    void onJobUpdate(Job job);

}
