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
package org.thingsboard.server.dao.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.job.TaskFailure;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Optional;

import static org.thingsboard.server.common.data.job.JobStatus.CANCELLED;
import static org.thingsboard.server.common.data.job.JobStatus.COMPLETED;
import static org.thingsboard.server.common.data.job.JobStatus.FAILED;
import static org.thingsboard.server.common.data.job.JobStatus.PENDING;
import static org.thingsboard.server.common.data.job.JobStatus.QUEUED;
import static org.thingsboard.server.common.data.job.JobStatus.RUNNING;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultJobService extends AbstractEntityService implements JobService {

    private final JobDao jobDao;

    @Transactional
    @Override
    public Job submitJob(TenantId tenantId, Job job) {
        if (jobDao.existsByKeyAndStatusOneOf(job.getKey(), QUEUED, PENDING, RUNNING)) {
            throw new IllegalArgumentException("The same job is already queued or running");
        }
        if (jobDao.existsByTenantIdAndTypeAndStatusOneOf(tenantId, job.getType(), PENDING, RUNNING)) {
            job.setStatus(QUEUED);
        } else {
            job.setStatus(PENDING);
        }
        return saveJob(tenantId, job, true, null);
    }

    @Override
    public Job findJobById(TenantId tenantId, JobId jobId) {
        return jobDao.findById(tenantId, jobId.getId());
    }

    @Transactional
    @Override
    public void cancelJob(TenantId tenantId, JobId jobId) {
        Job job = findForUpdate(tenantId, jobId);
        if (!job.getStatus().isOneOf(QUEUED, PENDING, RUNNING)) {
            throw new IllegalArgumentException("Job already " + job.getStatus().name().toLowerCase());
        }
        job.getResult().setCancellationTs(System.currentTimeMillis());
        JobStatus prevStatus = job.getStatus();
        if (job.getStatus() == QUEUED) {
            job.setStatus(CANCELLED); // setting cancelled status right away, because we don't expect stats for cancelled tasks
        } else if (job.getStatus() == PENDING) {
            job.setStatus(RUNNING);
        }
        saveJob(tenantId, job, true, prevStatus);
    }

    @Transactional
    @Override
    public void markAsFailed(TenantId tenantId, JobId jobId, String error) {
        Job job = findForUpdate(tenantId, jobId);
        job.getResult().setGeneralError(error);
        JobStatus prevStatus = job.getStatus();
        job.setStatus(FAILED);
        saveJob(tenantId, job, true, prevStatus);
    }

    @Transactional
    @Override
    public void processStats(TenantId tenantId, JobId jobId, JobStats jobStats) {
        Job job = findForUpdate(tenantId, jobId);
        if (job == null) {
            log.debug("[{}][{}] Got stale stats: {}", tenantId, jobId, jobStats);
            return;
        }
        JobStatus prevStatus = job.getStatus();
        if (job.getStatus() == PENDING) {
            job.setStatus(RUNNING);
        }

        JobResult result = job.getResult();
        if (jobStats.getTotalTasksCount() != null) {
            result.setTotalCount(jobStats.getTotalTasksCount());
        }

        boolean publishEvent = false;
        for (TaskResult taskResult : jobStats.getTaskResults()) {
            if (taskResult.isSuccess()) {
                result.setSuccessfulCount(result.getSuccessfulCount() + 1);
            } else if (taskResult.isDiscarded()) {
                result.setDiscardedCount(result.getDiscardedCount() + 1);
            } else {
                TaskFailure failure = taskResult.getFailure();
                result.setFailedCount(result.getFailedCount() + 1);
                if (result.getFailures().size() < 1000) { // preserving only first 1000 errors, not reprocessing if there are more failures
                    result.getFailures().add(failure);
                }
            }

            if (result.getCancellationTs() > 0) {
                if (!taskResult.isDiscarded() && System.currentTimeMillis() > result.getCancellationTs()) {
                    log.info("Got task result for cancelled job {}: {}, re-notifying processors about cancellation", jobId, taskResult);
                    // task processor forgot the task is cancelled
                    publishEvent = true;
                }
            }
        }

        if (job.getStatus() == RUNNING) {
            if (result.getTotalCount() != null && result.getCompletedCount() >= result.getTotalCount()) {
                if (result.getCancellationTs() > 0) {
                    job.setStatus(CANCELLED);
                } else if (result.getFailedCount() > 0) {
                    job.setStatus(FAILED);
                } else {
                    job.setStatus(COMPLETED);
                }
            }
        }

        saveJob(tenantId, job, publishEvent, prevStatus);
    }

    private Job saveJob(TenantId tenantId, Job job, boolean publishEvent, JobStatus prevStatus) {
        job = jobDao.save(tenantId, job);
        if (publishEvent) {
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(job.getId())
                    .entity(job)
                    .build());
        }
        log.info("[{}] Saved job: {}", tenantId, job);
        if (prevStatus != null && job.getStatus() != prevStatus) {
            log.info("[{}][{}][{}] New job status: {} -> {}", tenantId, job.getId(), job.getType(), prevStatus, job.getStatus());
            if (job.getStatus().isOneOf(CANCELLED, COMPLETED, FAILED) && prevStatus != QUEUED) { // if prev status is QUEUED - means there are already running jobs with this type, no need to check for waiting job
                checkWaitingJobs(tenantId, job.getType());
            }
        }
        return job;
    }

    private void checkWaitingJobs(TenantId tenantId, JobType jobType) {
        Job queuedJob = jobDao.findOldestByTenantIdAndTypeAndStatusForUpdate(tenantId, jobType, QUEUED);
        if (queuedJob == null) {
            return;
        }
        queuedJob.setStatus(PENDING);
        saveJob(tenantId, queuedJob, true, QUEUED);
    }

    @Override
    public PageData<Job> findJobsByTenantId(TenantId tenantId, PageLink pageLink) {
        return jobDao.findByTenantId(tenantId, pageLink);
    }

    private Job findForUpdate(TenantId tenantId, JobId jobId) {
        return jobDao.findByIdForUpdate(tenantId, jobId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findJobById(tenantId, (JobId) entityId));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        jobDao.removeById(tenantId, id.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
