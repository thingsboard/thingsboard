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

    AbstractChunkedAggregationTimeseriesDao tsDao;

    @Before
    public void setUp() throws Exception {
        tsDao = mock(AbstractChunkedAggregationTimeseriesDao.class);
        ListenableFuture<Optional<TsKvEntry>> optionalListenableFuture = Futures.immediateFuture(Optional.of(mock(TsKvEntry.class)));
        willReturn(optionalListenableFuture).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thanLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery("temp", 0, 3000, 2000, 0, Aggregation.COUNT, "DESC");
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, "temp", 0, 2000, 1000, Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, "temp", 2001, 3000, 2500, Aggregation.COUNT);
    }
}