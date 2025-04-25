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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.job.TaskResult.TaskFailure;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJobService implements JobService {

    private final JobDao jobDao;
    private final JobValidator validator = new JobValidator();

    @Override
    public Job createJob(TenantId tenantId, Job job) {
        validator.validate(job, Job::getTenantId);
        return jobDao.save(tenantId, job);
    }

    @Override
    public Job findJobById(TenantId tenantId, JobId jobId) {
        return jobDao.findById(tenantId, jobId.getId());
    }

    @Override
    public void processStats(JobId jobId, JobStats jobStats) {
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
        if (jobStats.getTotalTasksCount() != null) {
            jobResult.setTotalCount(jobStats.getTotalTasksCount());
        }

        for (TaskResult taskResult : jobStats.getTaskResults()) {
            if (taskResult.isSuccess()) {
                jobResult.setSuccessfulCount(jobResult.getSuccessfulCount() + 1);
            } else {
                TaskFailure failure = taskResult.getFailure();
                String key = failure.getTask().getKey();
                jobResult.setFailedCount(jobResult.getFailedCount() + 1);
                jobResult.getFailures().put(key, failure.getError());
            }
        }

        if (jobResult.getTotalCount() != null && jobResult.getSuccessfulCount() + jobResult.getFailedCount() >= jobResult.getTotalCount()) {
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

    // todo: cancellation, reprocessing

    public class JobValidator extends DataValidator<Job> {

        @Override
        protected void validateCreate(TenantId tenantId, Job job) {
            if (jobDao.existsByTenantIdAndTypeAndStatusOneOf(tenantId, job.getType(), JobStatus.PENDING, JobStatus.RUNNING)) {
                throw new DataValidationException("Job of this type is already running");
            }
        }

        @Override
        protected Job validateUpdate(TenantId tenantId, Job job) {
            throw new IllegalArgumentException("Job can't be updated externally");
        }

    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findJobById(tenantId, new JobId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
