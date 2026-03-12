/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.tasks.stats.processing_interval=0",
        "queue.tasks.partitioning_strategy=entity",
        "queue.tasks.partitions_per_type=DUMMY:100;DUMMY:50"
})
public class JobManagerTest_EntityPartitioningStrategy extends JobManagerTest {

    /*
     * Some tests are overridden because they are based on
     * tenant partitioning strategy (subsequent tasks processing within a tenant)
     * */

    @Override
    public void testCancelJob_simulateTaskProcessorRestart() {
    }

    @Override
    public void testSubmitJob_generalError() {
    }

    /*
     * Overridden because DummyTask.getEntityId() returns a random UUID per task,
     * so with entity partitioning all tasks are spread across partitions and
     * processed concurrently — they all finish at the same time. Using very long
     * taskProcessingTimeMs ensures tasks are reliably in-flight when cancel fires,
     * regardless of GC pauses or CI load. No successfulCount check is needed here:
     * we just wait for the job to be RUNNING, then cancel while tasks are sleeping.
     */
    @Override
    @Test
    public void testCancelJob_whileRunning() throws Exception {
        int tasksCount = 200;
        JobId jobId = submitJob(DummyJobConfiguration.builder()
                .successfulTasksCount(tasksCount)
                .taskProcessingTimeMs(TimeUnit.SECONDS.toMillis(30))
                .taskProcessingTimeoutMs(TimeUnit.SECONDS.toMillis(60))
                .build()).getId();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        });
        cancelJob(jobId);
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job job = findJobById(jobId);
            assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(job.getResult().getDiscardedCount()).isBetween(1, tasksCount);
            assertThat(job.getResult().getTotalCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getCompletedCount()).isEqualTo(tasksCount);
            assertThat(job.getResult().getStartTs()).isPositive();
            assertThat(job.getResult().getFinishTs()).isPositive();
            assertThat(job.getResult().getCancellationTs()).isPositive();
        });
    }

}
