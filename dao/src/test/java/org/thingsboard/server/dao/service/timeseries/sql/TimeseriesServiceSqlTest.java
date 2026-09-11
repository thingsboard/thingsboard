// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
