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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.job.task.DummyTaskProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.tasks.stats.processing_interval_ms=0"
})
public class JobManagerTest extends AbstractControllerTest {

    @Autowired
    private JobManager jobManager;

    @SpyBean
    private DummyTaskProcessor taskProcessor;

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
            assertThat(job.getResult().getCancelledCount()).isBetween(1, tasksCount - 1);
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
        }).when(taskProcessor).addToCancelledJobs(any()); // ignoring cancellation event,
        jobManager.cancelJob(tenantId, jobId);

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            System.err.println(job);
            assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(job.getResult().getSuccessfulCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getCancelledCount()).isBetween(1, tasksCount - 1);
            assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getCompletedCount()).isEqualTo(tasksCount);
        });
    }

    private Job findJobById(JobId jobId) throws Exception {
        return doGet("/api/job/" + jobId, Job.class);
    }

    private List<Job> findJobs() throws Exception {
        return doGetTypedWithPageLink("/api/jobs?", new TypeReference<PageData<Job>>() {}, new PageLink(100, 0)).getData();
    }

}