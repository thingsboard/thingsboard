/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.job.TaskResult.TaskFailure;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJobService implements JobService {

    private final JobDao jobDao;

    @Override
    public Job createJob(TenantId tenantId, Job job) {
        return jobDao.save(tenantId, job);
    }

    @Override
    public void reportTaskResults(JobId jobId, List<TaskResult> results) {
        Job job = jobDao.findById(TenantId.SYS_TENANT_ID, jobId.getId());
        switch (job.getStatus()) {
            case PENDING -> {
                job.setStatus(JobStatus.RUNNING);
            }
            case CANCELLED, COMPLETED, FAILED -> {
                // got some stale stats
                return;
            }
        }

        JobResult jobResult = job.getResult();
        for (TaskResult taskResult : results) {
            if (taskResult.isSuccess()) {
                jobResult.setSuccessfulCount(jobResult.getSuccessfulCount() + 1);
            } else {
                TaskFailure failure = taskResult.getFailure();
                String key = failure.getTask().getKey();
                jobResult.setFailedCount(jobResult.getFailedCount() + 1);
                jobResult.getFailures().put(key, failure.getError());
            }
        }

        if (jobResult.getSuccessfulCount() + jobResult.getFailedCount() >= jobResult.getTotalCount()) {
            if (jobResult.getFailures().isEmpty()) {
                job.setStatus(JobStatus.COMPLETED);
            } else {
                job.setStatus(JobStatus.FAILED);
            }
        }
        log.info("Saving job {}", job);
        jobDao.save(TenantId.SYS_TENANT_ID, job);
    }

    @Override
    public PageData<Job> findJobsByTenantId(TenantId tenantId, PageLink pageLink) {
        return jobDao.findByTenantId(tenantId, pageLink);
    }

}
