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

import org.assertj.core.api.ThrowingConsumer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.EdqsState.EdqsApiMode;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;
import org.thingsboard.server.queue.discovery.DiscoveryService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
//        "queue.type=kafka", // uncomment to use Kafka
//        "queue.kafka.bootstrap.servers=10.7.2.107:9092",
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local",
        "queue.edqs.readiness_check_interval=500"
})
public class EdqsEntityQueryControllerTest extends EntityQueryControllerTest {

    @Autowired
    private EdqsService edqsService;

    @Autowired
    private DiscoveryService discoveryService;

    @MockBean // so that we don't do backup for tests
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void before() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.getState().isApiEnabled());
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, int expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findByQuery(query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected PageData<AlarmData> findAlarmsByQueryAndCheck(AlarmDataQuery query, int expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findAlarmsByQuery(query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected Long countByQueryAndCheck(EntityCountQuery query, long expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(query),
                result -> result == expectedResult);
    }

    @Override
    protected Long countAlarmsByQueryAndCheck(AlarmCountQuery query, long expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countAlarmsByQuery(query),
                result -> result == expectedResult);
    }

    @Test
    public void testEdqsState() throws Exception {
        loginSysAdmin();
        assertThat(getEdqsState().getApiMode()).isEqualTo(EdqsApiMode.AUTO_ENABLED);

        // notifying EDQS is not ready: API should be auto-disabled
        discoveryService.setReady(false);
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.AUTO_DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        // manually disabling API
        edqsService.processSystemRequest(ToCoreEdqsRequest.builder()
                .apiEnabled(false)
                .build());
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        // notifying EDQS is ready: API should not be enabled automatically because manually disabled previously
        discoveryService.setReady(true);
        verifyState(state -> {
            assertThat(state.getEdqsReady()).isTrue();
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.DISABLED);
            assertThat(state.isApiEnabled()).isFalse();
        });

        // manually enabling API
        edqsService.processSystemRequest(ToCoreEdqsRequest.builder()
                .apiEnabled(true)
                .build());
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.ENABLED);
            assertThat(state.getEdqsReady()).isTrue();
            assertThat(state.isApiEnabled()).isTrue();
        });

        // notifying EDQS is not ready: API should be auto-disabled
        discoveryService.setReady(false);
        verifyState(state -> {
            assertThat(state.getApiMode()).isEqualTo(EdqsApiMode.AUTO_DISABLED);
            assertThat(state.getEdqsReady()).isFalse();
            assertThat(state.isApiEnabled()).isFalse();
        });

        discoveryService.setReady(true);
    }

    private void verifyState(ThrowingConsumer<EdqsState> assertion) {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getEdqsState()).satisfies(assertion);
        });
    }

    private EdqsState getEdqsState() throws Exception {
        return doGet("/api/edqs/state", EdqsState.class);
    }

}
