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

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.ConstraintValidator;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
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
    private final EntityService entityService;

    @Transactional
    @Override
    public Job saveJob(TenantId tenantId, Job job) {
        if (jobDao.existsByTenantAndKeyAndStatusOneOf(tenantId, job.getKey(), QUEUED, PENDING, RUNNING)) {
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
        long lastFinishTs = 0;
        for (TaskResult taskResult : jobStats.getTaskResults()) {
            if (!taskResult.getKey().equals(job.getConfiguration().getTasksKey())) {
                log.debug("Ignoring task result {} with outdated key {}", taskResult, job.getConfiguration().getTasksKey());
                continue;
            }

            result.processTaskResult(taskResult);

            if (result.getCancellationTs() > 0) {
                if (!taskResult.isDiscarded() && System.currentTimeMillis() > result.getCancellationTs()) {
                    log.info("Got task result for cancelled job {}: {}, re-notifying processors about cancellation", jobId, taskResult);
                    // task processor forgot the task is cancelled
                    publishEvent = true;
                }
            }
            if (taskResult.getFinishTs() > lastFinishTs) {
                lastFinishTs = taskResult.getFinishTs();
            }
        }

        if (job.getStatus() == RUNNING) {
            if (result.getTotalCount() != null && result.getCompletedCount() >= result.getTotalCount()) {
                if (result.getCancellationTs() > 0) {
                    job.setStatus(CANCELLED);
                } else if (result.getFailedCount() > 0) {
                    job.setStatus(FAILED);
                    publishEvent = true;
                } else {
                    job.setStatus(COMPLETED);
                    publishEvent = true;
                }
                result.setFinishTs(lastFinishTs);
                job.getConfiguration().setToReprocess(null);
            }
        }

        saveJob(tenantId, job, publishEvent, prevStatus);
    }

    private Job saveJob(TenantId tenantId, Job job, boolean publishEvent, JobStatus prevStatus) {
        ConstraintValidator.validateFields(job);
        if (!Job.SUPPORTED_ENTITY_TYPES.contains(job.getEntityId().getEntityType())) {
            throw new IllegalArgumentException("Unsupported entity type " + job.getEntityId().getEntityType());
        }
        if (job.getStatus() == PENDING) {
            job.getResult().setStartTs(System.currentTimeMillis());
        }

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
    public PageData<Job> findJobsByFilter(TenantId tenantId, JobFilter filter, PageLink pageLink) {
        PageData<Job> jobs = jobDao.findByTenantIdAndFilter(tenantId, filter, pageLink);

        Set<EntityId> entityIds = jobs.getData().stream()
                .map(Job::getEntityId)
                .collect(Collectors.toSet());
        Map<EntityId, EntityInfo> entityInfos = entityService.fetchEntityInfos(tenantId, null, entityIds);
        jobs.getData().forEach(job -> {
            EntityInfo entityInfo = entityInfos.get(job.getEntityId());
            if (entityInfo != null) {
                job.setEntityName(entityInfo.getName());
            }
        });
        return jobs;
    }

    @Override
    public Job findLatestJobByKey(TenantId tenantId, String key) {
        return jobDao.findLatestByTenantIdAndKey(tenantId, key);
    }

    @Override
    public void deleteJob(TenantId tenantId, JobId jobId) {
        Job job = findJobById(tenantId, jobId);
        if (!job.getStatus().isOneOf(CANCELLED, COMPLETED, FAILED)) {
            throw new IllegalArgumentException("Job must be cancelled, completed or failed");
        }
        jobDao.removeById(tenantId, jobId.getId());
    }

    @Override
    public int deleteJobsByEntityId(TenantId tenantId, EntityId entityId) { // TODO: cancel all jobs for this entity
        return jobDao.removeByEntityId(tenantId, entityId);
    }

    private Job findForUpdate(TenantId tenantId, JobId jobId) {
        return jobDao.findByIdForUpdate(tenantId, jobId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findJobById(tenantId, (JobId) entityId));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(jobDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        jobDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        jobDao.removeByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
