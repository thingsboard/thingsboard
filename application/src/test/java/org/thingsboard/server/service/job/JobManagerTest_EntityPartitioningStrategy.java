// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.job;

import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;

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

}
