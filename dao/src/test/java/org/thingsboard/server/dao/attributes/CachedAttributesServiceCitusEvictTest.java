// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.citus.CitusSettings;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedAttributesServiceCitusEvictTest {

    private final TenantId tenantId = TenantId.SYS_TENANT_ID;
    private final EntityId entityId = new DeviceId(UUID.randomUUID());
    private final AttributeScope scope = AttributeScope.SERVER_SCOPE;
    private final String key = "temperature";
    private final Long version = 5L;

    @SuppressWarnings("unchecked")
    private final VersionedTbCache<AttributeCacheKey, AttributeKvEntry> cache = mock(VersionedTbCache.class);
    private AttributesDao attributesDao;
    private EdqsService edqsService;

    @Before
    public void setUp() {
        attributesDao = mock(AttributesDao.class);
        edqsService = mock(EdqsService.class);
        when(attributesDao.removeAllWithVersions(tenantId, entityId, scope, List.of(key)))
                .thenReturn(List.of(Futures.immediateFuture(TbPair.of(key, version))));
    }

    private CachedAttributesService service(CitusSettings citusSettings) {
        CachedAttributesService service = new CachedAttributesService(attributesDao, null, edqsService,
                mock(StatsFactory.class), null, cache, citusSettings);
        ReflectionTestUtils.setField(service, "cacheExecutor", MoreExecutors.newDirectExecutorService());
        return service;
    }

    @Test
    public void givenCitusEnabled_whenRemoveAll_thenUnversionedEvict() throws Exception {
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(true);

        service(citusSettings).removeAll(tenantId, entityId, scope, List.of(key)).get();

        AttributeCacheKey cacheKey = new AttributeCacheKey(scope, entityId, key);
        verify(cache, times(1)).evict(cacheKey);
        verify(cache, never()).evict(eq(cacheKey), anyLong());
    }

    @Test
    public void givenCitusDisabled_whenRemoveAll_thenVersionedEvict() throws Exception {
        CitusSettings citusSettings = mock(CitusSettings.class);
        when(citusSettings.isEnabled()).thenReturn(false);

        service(citusSettings).removeAll(tenantId, entityId, scope, List.of(key)).get();

        AttributeCacheKey cacheKey = new AttributeCacheKey(scope, entityId, key);
        verify(cache, times(1)).evict(cacheKey, version);
        verify(cache, never()).evict(cacheKey);
    }

}
