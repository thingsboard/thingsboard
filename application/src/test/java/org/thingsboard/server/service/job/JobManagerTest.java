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
package org.thingsboard.server.service.job;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.queue.task.JobStatsService;
import org.thingsboard.server.service.job.task.DummyTaskProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.tasks.stats.processing_interval_ms=0"
})
public class JobManagerTest extends AbstractControllerTest {

    @Autowired
    private JobManager jobManager;

    @Autowired
    private JobService jobService;

    @SpyBean
    private DummyTaskProcessor taskProcessor;

    @SpyBean
    private JobStatsService jobStatsService;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSubmitJob_allTasksSuccessful() {
        int tasksCount = 5;
        JobId jobId = jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .successfulTasksCount(tasksCount)
                        .taskProcessingTimeMs(1000)
                        .build())
                .build()).getId();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
            assertThat(job.getResult().getSuccessfulCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
        });
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(job.getResult().getSuccessfulCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getFailures()).isEmpty();
            assertThat(job.getResult().getCompletedCount()).isEqualTo(tasksCount);
        });
    }

    @Test
    public void testSubmitJob_someTasksPermanentlyFailed() {
        int successfulTasks = 3;
        int failedTasks = 2;
        JobId jobId = jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .successfulTasksCount(successfulTasks)
                        .failedTasksCount(failedTasks)
                        .errors(List.of("error1", "error2", "error3"))
                        .retries(2)
                        .taskProcessingTimeMs(100)
                        .build())
                .build()).getId();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
            JobResult jobResult = job.getResult();
            assertThat(jobResult.getSuccessfulCount()).isEqualTo(successfulTasks);
            assertThat(jobResult.getFailedCount()).isEqualTo(failedTasks);
            assertThat(jobResult.getTotalCount()).isEqualTo(successfulTasks + failedTasks);
            assertThat(jobResult.getFailures().get("Task 1")).isEqualTo("error3"); // last error
            assertThat(jobResult.getFailures().get("Task 2")).isEqualTo("error3"); // last error
            assertThat(jobResult.getCompletedCount()).isEqualTo(jobResult.getTotalCount());
        });
    }

    @Test
    public void testCancelJob_whileRunning() throws Exception {
        int tasksCount = 100;
        JobId jobId = jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .successfulTasksCount(tasksCount)
                        .taskProcessingTimeMs(100)
                        .build())
                .build()).getId();

        Thread.sleep(500);
        jobManager.cancelJob(tenantId, jobId);
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(job.getResult().getSuccessfulCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getDiscardedCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getCompletedCount()).isEqualTo(tasksCount);
        });
    }

    @Test
    public void testCancelJob_simulateTaskProcessorRestart() {
        int tasksCount = 10;
        JobId jobId = jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .successfulTasksCount(tasksCount)
                        .taskProcessingTimeMs(100)
                        .build())
                .build()).getId();

        // simulate cancelled jobs are forgotten
        AtomicInteger cancellationRenotifyAttempt = new AtomicInteger(0);
        doAnswer(inv -> {
            if (cancellationRenotifyAttempt.incrementAndGet() >= 5) {
                inv.callRealMethod();
            }
            return null;
        }).when(taskProcessor).addToDiscardedJobs(any()); // ignoring cancellation event,
        jobManager.cancelJob(tenantId, jobId);

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(job.getResult().getSuccessfulCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getDiscardedCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getCompletedCount()).isEqualTo(tasksCount);
        });
    }

    @Test
    public void whenTenantIsDeleted_thenCancelAllTheJobs() throws Exception {
        loginSysAdmin();
        createDifferentTenant();

        TenantId tenantId = this.differentTenantId;
        jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .successfulTasksCount(1000)
                        .taskProcessingTimeMs(500)
                        .build())
                .build());

        Thread.sleep(2000);
        deleteDifferentTenant();
        Mockito.reset(jobStatsService);

        Thread.sleep(3000);
        verify(jobStatsService, never()).reportTaskResult(any(), any(), any());
        Assertions.assertThat(jobService.findJobsByTenantId(tenantId, new PageLink(100, 0)).getData()).isEmpty();
    }

    @Test
    public void testSubmitMultipleJobs() {
        int tasksCount = 3;
        int jobsCount = 3;
        for (int i = 1; i <= jobsCount; i++) {
            Job job = Job.builder()
                    .tenantId(tenantId)
                    .type(JobType.DUMMY)
                    .key("test-job-" + i)
                    .description("test job")
                    .configuration(DummyJobConfiguration.builder()
                            .successfulTasksCount(tasksCount)
                            .taskProcessingTimeMs(1000)
                            .build())
                    .build();
            jobManager.submitJob(job);
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Job> jobs = findJobs();
            assertThat(jobs).hasSize(jobsCount);
            Job firstJob = jobs.get(2); // ordered by createdTime descending
            assertThat(firstJob.getStatus()).isEqualTo(JobStatus.RUNNING);
            Job secondJob = jobs.get(1);
            assertThat(secondJob.getStatus()).isEqualTo(JobStatus.QUEUED);
            Job thirdJob = jobs.get(0);
            assertThat(thirdJob.getStatus()).isEqualTo(JobStatus.QUEUED);
        });

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Job> jobs = findJobs();
            for (Job job : jobs) {
                assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
                assertThat(job.getResult().getSuccessfulCount()).isEqualTo(tasksCount);
                assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
            }
        });
    }

    @Test
    public void testCancelQueuedJob() {
        int tasksCount = 3;
        int jobsCount = 3;
        List<JobId> jobIds = new ArrayList<>();
        for (int i = 1; i <= jobsCount; i++) {
            Job job = Job.builder()
                    .tenantId(tenantId)
                    .type(JobType.DUMMY)
                    .key("test-job-" + i)
                    .description("test job")
                    .configuration(DummyJobConfiguration.builder()
                            .successfulTasksCount(tasksCount)
                            .taskProcessingTimeMs(1000)
                            .build())
                    .build();
            jobIds.add(jobManager.submitJob(job).getId());
        }

        for (int i = 1; i < jobIds.size(); i++) {
            jobManager.cancelJob(tenantId, jobIds.get(i));
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Job> jobs = findJobs();

            Job firstJob = jobs.get(2);
            assertThat(firstJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(firstJob.getResult().getSuccessfulCount()).isEqualTo(tasksCount);
            assertThat(firstJob.getResult().getTotalCount()).isEqualTo(tasksCount);

            Job secondJob = jobs.get(1);
            assertThat(secondJob.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(secondJob.getResult().getCompletedCount()).isZero();

            Job thirdJob = jobs.get(0);
            assertThat(thirdJob.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(thirdJob.getResult().getCompletedCount()).isZero();
        });
    }

    @Test
    public void testGeneralJobError() {
        int submittedTasks = 100;
        JobId jobId = jobManager.submitJob(Job.builder()
                .tenantId(tenantId)
                .type(JobType.DUMMY)
                .key("test-job")
                .description("test job")
                .configuration(DummyJobConfiguration.builder()
                        .generalError("Some error while submitting tasks")
                        .submittedTasksBeforeGeneralError(submittedTasks)
                        .taskProcessingTimeMs(10)
                        .build())
                .build()).getId();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(job.getResult().getSuccessfulCount()).isBetween(1, submittedTasks);
            assertThat(job.getResult().getDiscardedCount()).isBetween(1, submittedTasks);
            assertThat(job.getResult().getTotalCount()).isNull();
        });
    }

    // todo: job with zero tasks, reprocessing

    private Job findJobById(JobId jobId) throws Exception {
        return doGet("/api/job/" + jobId, Job.class);
    }

    private List<Job> findJobs() throws Exception {
        return doGetTypedWithPageLink("/api/jobs?", new TypeReference<PageData<Job>>() {}, new PageLink(100, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC))).getData();
    }

}