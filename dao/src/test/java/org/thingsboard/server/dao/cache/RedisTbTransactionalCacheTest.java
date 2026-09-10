// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisSslCredentials;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationCacheKey;
import org.thingsboard.server.dao.relation.RelationRedisCache;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RelationRedisCache.class, CacheSpecsMap.class, TBRedisCacheConfiguration.class})
@TestPropertySource(properties = {
        "cache.type=redis",
        "cache.specs.relations.timeToLiveInMinutes=1440",
        "cache.specs.relations.maxSize=0",
})
@Slf4j
public class RedisTbTransactionalCacheTest {

    @MockitoBean
    private RelationRedisCache relationRedisCache;
    @MockitoBean
    private RedisConnectionFactory connectionFactory;
    @MockitoBean
    private RedisConnection redisConnection;
    @MockitoBean
    private RedisSslCredentials redisSslCredentials;

    @Test
    public void testNoOpWhenCacheDisabled() {
        when(connectionFactory.getConnection()).thenReturn(redisConnection);

        relationRedisCache.put(createRelationCacheKey(), null);
        relationRedisCache.putIfAbsent(createRelationCacheKey(), null);
        relationRedisCache.evict(createRelationCacheKey());
        relationRedisCache.evict(List.of(createRelationCacheKey()));
        relationRedisCache.getAndPutInTransaction(createRelationCacheKey(), null, false);
        relationRedisCache.getAndPutInTransaction(createRelationCacheKey(), null, null, null, false);
        relationRedisCache.getOrFetchFromDB(createRelationCacheKey(), null, false, false);

        verify(connectionFactory, never()).getConnection();
        verifyNoInteractions(redisConnection);
    }

    private RelationCacheKey createRelationCacheKey() {
        return new RelationCacheKey(new DeviceId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), null, RelationTypeGroup.COMMON);
    }

}
