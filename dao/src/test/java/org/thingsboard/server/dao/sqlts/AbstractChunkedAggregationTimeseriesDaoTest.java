/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractChunkedAggregationTimeseriesDaoTest {

    final int START_TS = 1;
    final int LIMIT = 1;
    final int END_TS = 3000;
    final String TEMP = "temp";
    final String DESC = "DESC";
    AbstractChunkedAggregationTimeseriesDao tsDao;

    //SOME PRESENT: When we give data with period l-r, program return data in interval [l;r)

    @Before
    public void setUp() throws Exception {
        tsDao = mock(AbstractChunkedAggregationTimeseriesDao.class);
        ListenableFuture<Optional<TsKvEntry>> optionalListenableFuture = Futures.immediateFuture(Optional.of(mock(TsKvEntry.class)));
        willReturn(optionalListenableFuture).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        willReturn(Futures.immediateFuture(null)).given(tsDao).getTskvEntriesFuture(any());
        willCallRealMethod().given(tsDao).findAllAsync(any(), any(), any(ReadTsKvQuery.class));
        willCallRealMethod().given(tsDao).findIntervals(any(), any(), any(ReadTsKvQuery.class));
        willCallRealMethod().given(tsDao).getTsForReadTsKvQuery(anyLong(), anyLong());
        willCallRealMethod().given(tsDao).toPartitionTs(anyLong());
        willCallRealMethod().given(tsDao).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 2000, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, START_TS, 2001, START_TS + (2001 - START_TS) / 2, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, 2001, END_TS, 2001 + (END_TS + 1 - 2001) / 2, LIMIT, Aggregation.COUNT, DESC);

        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, START_TS, 2001, Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQuerySecond, 2001, END_TS + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriod() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, END_TS, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, START_TS + (END_TS + 1 - START_TS) / 2, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, START_TS, END_TS + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriodMinusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 2999, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, START_TS, END_TS - 2, START_TS + (END_TS - 1 - START_TS) / 2, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, END_TS - 1, END_TS, END_TS - 1 + (END_TS + 1 - (END_TS - 1)) / 2, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, START_TS, END_TS, Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQuerySecond, END_TS, END_TS + 1, Aggregation.COUNT);

    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriodPlusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 3001, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, START_TS + (3001 + 1 - START_TS) / 2, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, START_TS, END_TS + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsZero() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 0, 0, 1, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 0, 0, 0, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, 0, 0 + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, START_TS, 1, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query, START_TS, START_TS + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsIntegerMax() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Integer.MAX_VALUE + (Integer.MAX_VALUE + 1L - Integer.MAX_VALUE) / 2L, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsBigNumber() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, Integer.MAX_VALUE, LIMIT, Aggregation.COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, START_TS + (END_TS + 1 - START_TS) / 2, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, subQueryFirst, START_TS, END_TS + 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanCountIntervalEqualsPeriodSize() {
        long intervalTs = 3;
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, intervalTs, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), any(), any(), anyLong(), anyLong(), any());
        for (long i = START_TS; i <= END_TS; i += intervalTs) {
            ReadTsKvQuery querySub = new BaseReadTsKvQuery(TEMP, i, i + intervalTs, i + (i + intervalTs - i) / 2, LIMIT, Aggregation.COUNT, DESC);
            verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, querySub, i, i + intervalTs, Aggregation.COUNT);
        }
    }
}