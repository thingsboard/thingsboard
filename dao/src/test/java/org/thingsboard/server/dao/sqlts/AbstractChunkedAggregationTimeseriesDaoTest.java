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
    final int LIMIT = 0;
    final int END_TS = 3000;
    final String TEMP = "temp";
    final String DESC = "DESC";
    AbstractChunkedAggregationTimeseriesDao tsDao;

    @Before
    public void setUp() throws Exception {
        tsDao = mock(AbstractChunkedAggregationTimeseriesDao.class);
        ListenableFuture<Optional<TsKvEntry>> optionalListenableFuture = Futures.immediateFuture(Optional.of(mock(TsKvEntry.class)));
        willReturn(optionalListenableFuture).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 2000, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 1, 2000, 1000, Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 2001, END_TS, 2500, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriod() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, END_TS, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 1, END_TS, 1500, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriodMinusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 2999, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 0, 2999, 1499, Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, END_TS, END_TS, END_TS, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsPeriodPlusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, 3001, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, START_TS, END_TS, 1501, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsZero() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, 0, 1, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 0, 0, 0, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, 1, 1, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, 1, 1, 1, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsOneMillisecondAndStartTsIsLongMax() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Long.MAX_VALUE, Long.MAX_VALUE, 1, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanIntervalEqualsBigNumber() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, Long.MAX_VALUE, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, START_TS, END_TS, 1500, Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanCountIntervalEqualsPeriodSize() {
        long intervalTs = 3;
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, START_TS, END_TS, intervalTs, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        for (long i = START_TS; i < END_TS; i += intervalTs) {
            verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, i, i + intervalTs, i, Aggregation.COUNT);
        }
    }
}