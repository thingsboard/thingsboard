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
package org.thingsboard.server.dao.service.timeseries.sql;

import org.junit.Test;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
public class TimeseriesServiceSqlTest extends BaseTimeseriesServiceTest {

    @Test
    public void testRemoveLatestAndNoValuePresentInDB() throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry tsKvEntry = toTsEntry(TS, stringKvEntry);
        tsService.save(tenantId, deviceId, tsKvEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        Optional<TsKvEntry> tsKvEntryOpt = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        assertThat(tsKvEntryOpt).isPresent();
        equalsIgnoreVersion(tsKvEntry, tsKvEntryOpt.get());
        assertThat(tsKvEntryOpt.get().getVersion()).isNotNull();

        tsService.removeLatest(tenantId, deviceId, List.of(STRING_KEY));

        await().alias("Wait until ts last is removed from the cache").atMost(MAX_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<TsKvEntry> tsKvEntryAfterRemoval = tsService.findLatest(tenantId, deviceId, STRING_KEY).get(MAX_TIMEOUT, TimeUnit.SECONDS);
                    assertThat(tsKvEntryAfterRemoval).isNotPresent();
                });
    }

}
