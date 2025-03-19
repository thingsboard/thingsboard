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
package org.thingsboard.server.controller;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.state.EdqsStateService;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
//        "queue.type=kafka", // uncomment to use Kafka
//        "queue.kafka.bootstrap.servers=10.7.2.107:9092",
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local"
})
public class EdqsEntityQueryControllerTest extends EntityQueryControllerTest {

    @Autowired
    private EdqsApiService edqsApiService;

    @Autowired
    private EdqsStateService edqsStateService;

    @MockBean // so that we don't do backup for tests
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void before() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsApiService.isEnabled() && edqsStateService.isReady());
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, int expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findByQuery(query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected Long countByQueryAndCheck(EntityCountQuery query, long expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(query),
                result -> result == expectedResult);
    }

}
