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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;
import static org.thingsboard.server.common.data.kv.Aggregation.COUNT;

public class AbstractChunkedAggregationTimeseriesDaoTest {

    final int LIMIT = 1;
    final String TEMP = "temp";
    final String DESC = "DESC";
    private AbstractChunkedAggregationTimeseriesDao tsDao;

    @Before
    public void setUp() throws Exception {
        tsDao = spy(AbstractChunkedAggregationTimeseriesDao.class);
        Optional<TsKvEntry> optionalListenableFuture = Optional.of(mock(TsKvEntry.class));
        willReturn(Futures.immediateFuture(optionalListenableFuture)).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        willReturn(Futures.immediateFuture(mock(ReadTsKvQueryResult.class))).given(tsDao).getReadTsKvQueryResultFuture(any(), any());
        willReturn(mock(ReadTsKvQueryResult.class)).given(tsDao).findAllAsyncWithLimit(any(), any());
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 2001, 1001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, 2001, 3000, 2501, LIMIT, COUNT, DESC);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 2001, getTsForReadTsKvQuery(1, 2001), COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuerySecond.getKey(), 2001, 3000, getTsForReadTsKvQuery(2001, 3000), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        assertThat(tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query)).isNotNull();
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2999, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3000, 1500, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);

    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 0, 0, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 0, 1, 0, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 0, 1, getTsForReadTsKvQuery(0, 1), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 1, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuery = new BaseReadTsKvQuery(TEMP, 1, 2, 1, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuery.getKey(), 1, 2, getTsForReadTsKvQuery(1, 2), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), Integer.MAX_VALUE, 1L + Integer.MAX_VALUE, getTsForReadTsKvQuery(Integer.MAX_VALUE, 1L + Integer.MAX_VALUE), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        for (long i = 1; i <= 3000; i += 3) {
            verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, TEMP, i, Math.min(i + 3, 3000), getTsForReadTsKvQuery(i, i + 3), COUNT);
        }
    }

    @Test
    public void givenZeroInterval_whenAggregateCount_thenFindAllWithoutAggregation() {
        givenInterval_whenAggregateCount_thenFindAllWithoutAggregation(0);
    }

    @Test
    public void givenNegativeInterval_whenAggregateCount_thenFindAllWithoutAggregation() {
        givenInterval_whenAggregateCount_thenFindAllWithoutAggregation(-1);
    }

    public void givenInterval_whenAggregateCount_thenFindAllWithoutAggregation(int interval) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, interval, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAllAsyncWithLimit(any(), any());
        verify(tsDao, times(0)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
    }

    long getTsForReadTsKvQuery(long startTs, long endTs) {
        return startTs + (endTs - startTs) / 2L;
    }

}
