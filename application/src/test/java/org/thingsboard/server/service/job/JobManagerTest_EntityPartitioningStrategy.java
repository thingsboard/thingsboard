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
    public void testCancelJob_simulateTaskProcessorRestart() throws Exception {
    }

    @Override
    public void testSubmitJob_generalError() {

    }

}
