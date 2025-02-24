/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api_enabled=true",
        "queue.edqs.mode=local"
})
public class EdqsEntityServiceTest extends EntityServiceTest {

    @Autowired
    private EdqsService edqsService;

    @MockBean
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void beforeEach() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.isApiEnabled());
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, EntityDataQuery query, long expectedResultSize) {
        return await().atMost(15, TimeUnit.SECONDS).until(() -> findByQuery(customerId, query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), countQuery, expectedResult);
    }

    @Override
    protected long countByQueryAndCheck(CustomerId customerId, EntityCountQuery query, int expectedResult) {
        return await().atMost(15, TimeUnit.SECONDS).until(() -> countByQuery(customerId, query),
                result -> result == expectedResult);
    }

}
