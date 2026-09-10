// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@TbTransportComponent
@Slf4j
public class DefaultEntityLimitsCache implements EntityLimitsCache {

    private static final int DEVIATION = 10;
    private final Cache<EntityLimitKey, Boolean> cache;

    public DefaultEntityLimitsCache(@Value("${cache.entityLimits.timeToLiveInMinutes:5}") int ttl,
                                    @Value("${cache.entityLimits.maxSize:100000}") int maxSize) {
        // We use the 'random' expiration time to avoid peak loads.
        long mainPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * (100 - DEVIATION);
        long randomPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * DEVIATION;
        cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<EntityLimitKey, Boolean>() {
                    @Override
                    public long expireAfterCreate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime) {
                        return mainPart + (long) (randomPart * ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
                    public long expireAfterUpdate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public boolean get(EntityLimitKey key) {
        var result = cache.getIfPresent(key);
        return result != null ? result : false;
    }

    @Override
    public void put(EntityLimitKey key, boolean value) {
        cache.put(key, value);
    }
}
