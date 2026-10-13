// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.dao.timeseries.TsLatestCacheKey;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedRedisSqlTimeseriesLatestDaoCitusEvictTest {

    private final TenantId tenantId = TenantId.SYS_TENANT_ID;
    private final EntityId entityId = new DeviceId(UUID.randomUUID());
    private final String key = "temperature";
    private final Long version = 7L;

    @SuppressWarnings("unchecked")
    private final VersionedTbCache<TsLatestCacheKey, TsKvEntry> cache = mock(VersionedTbCache.class);
    private CacheExecutorService cacheExecutorService;
    private SqlTimeseriesLatestDao sqlDao;
    private DeleteTsKvQuery query;

    @Before
    public void setUp() {
        cacheExecutorService = mock(CacheExecutorService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheExecutorService).execute(any(Runnable.class));

        sqlDao = mock(SqlTimeseriesLatestDao.class);
        query = mock(DeleteTsKvQuery.class);
        when(query.getKey()).thenReturn(key);
        // No new latest entry remains => null data, removed == true
        TsKvLatestRemovingResult result = new TsKvLatestRemovingResult(key, true, version);
        when(sqlDao.removeLatest(tenantId, entityId, query))
                .thenReturn(Futures.immediateFuture(result));
    }

    private CachedRedisSqlTimeseriesLatestDao dao(CitusSettings citusSettings) {
        return new CachedRedisSqlTimeseriesLatestDao(cacheExecutorService, sqlDao, mock(StatsFactory.class), cache, citusSettings);
    }

    @Test
    public void givenCitusEnabled_whenRemoveLatest_thenUnversionedEvict() throws Exception {
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(true);

        dao(citusSettings).removeLatest(tenantId, entityId, query).get();

        TsLatestCacheKey cacheKey = new TsLatestCacheKey(entityId, key);
        verify(cache, times(1)).evict(cacheKey);
        verify(cache, never()).evict(eq(cacheKey), anyLong());
    }

    @Test
    public void givenCitusDisabled_whenRemoveLatest_thenVersionedEvict() throws Exception {
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(false);

        dao(citusSettings).removeLatest(tenantId, entityId, query).get();

        TsLatestCacheKey cacheKey = new TsLatestCacheKey(entityId, key);
        verify(cache, times(1)).evict(cacheKey, version);
        verify(cache, never()).evict(cacheKey);
    }

    @Test
    public void givenCitusEnabledAndRewrittenLatestRemains_whenRemoveLatest_thenEvictsAndNeverPuts() throws Exception {
        // A delete whose result carries a remaining rewritten latest entry must STILL evict under Citus:
        // the rewritten latest is a fresh INSERT whose per-row version resets to 1, so a version-guarded put
        // would be rejected by the cache still holding the deleted row's higher version.
        BasicTsKvEntry rewrittenLatest = new BasicTsKvEntry(123L, new LongDataEntry(key, 42L));
        when(sqlDao.removeLatest(tenantId, entityId, query))
                .thenReturn(Futures.immediateFuture(new TsKvLatestRemovingResult(rewrittenLatest, version)));
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(true);

        dao(citusSettings).removeLatest(tenantId, entityId, query).get();

        TsLatestCacheKey cacheKey = new TsLatestCacheKey(entityId, key);
        verify(cache, times(1)).evict(cacheKey);
        verify(cache, never()).put(any(), any());
        verify(cache, never()).evict(eq(cacheKey), anyLong());
    }

    @Test
    public void givenCitusDisabledAndRewrittenLatestRemains_whenRemoveLatest_thenPutsNewLatestWithVersion() throws Exception {
        BasicTsKvEntry rewrittenLatest = new BasicTsKvEntry(123L, new LongDataEntry(key, 42L));
        when(sqlDao.removeLatest(tenantId, entityId, query))
                .thenReturn(Futures.immediateFuture(new TsKvLatestRemovingResult(rewrittenLatest, version)));
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(false);

        dao(citusSettings).removeLatest(tenantId, entityId, query).get();

        TsLatestCacheKey cacheKey = new TsLatestCacheKey(entityId, key);
        ArgumentCaptor<TsKvEntry> putCaptor = ArgumentCaptor.forClass(TsKvEntry.class);
        verify(cache, times(1)).put(eq(cacheKey), putCaptor.capture());
        TsKvEntry cachedEntry = putCaptor.getValue();
        assertThat(cachedEntry.getTs()).isEqualTo(rewrittenLatest.getTs());
        assertThat(cachedEntry.getValue()).isEqualTo(rewrittenLatest.getValue());
        assertThat(cachedEntry.getVersion()).isEqualTo(version);
        verify(cache, never()).evict(any(TsLatestCacheKey.class));
        verify(cache, never()).evict(any(TsLatestCacheKey.class), anyLong());
    }

}
