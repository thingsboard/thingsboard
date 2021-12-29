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
package org.thingsboard.server.service.security.auth;

import lombok.NonNull;
import org.apache.commons.collections4.map.ReferenceIdentityMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserActiveSessionsLimitService {
    private final CacheManager cacheManager;
    private Cache userActiveSessionsCache;

    private final Long differentIpsLimit;
    private final Long oneIpLimit;
    private final JwtTokenFactory tokenFactory;

    public UserActiveSessionsLimitService(
            CacheManager cacheManager,
            @Value("${security.sessionsLimit.differentIpsLimit}") Long differentIpsLimit,
            @Value("${security.sessionsLimit.oneIpLimit}") Long oneIpLimit,
            JwtTokenFactory tokenFactory
    ) {
        this.cacheManager = cacheManager;
        this.differentIpsLimit = differentIpsLimit;
        this.oneIpLimit = oneIpLimit;
        this.tokenFactory = tokenFactory;
    }

    @PostConstruct
    protected void initCache() {
        userActiveSessionsCache = cacheManager.getCache(CacheConstants.USER_ACTIVE_SESSIONS_CACHE);
    }

    public boolean sessionIsValidFor(UserId userId, JwtToken token, String ip) {
        if (differentIpsLimit == 0 && oneIpLimit == 0)
            return true;

        String tokenId = getTokenId(token);
        List<String> validTokens = extractValidTokens(userId, ip);

        return validTokens.contains(tokenId);
    }

    public void validateSessionFor(@NonNull UserId userId, @NonNull JwtToken token, @NonNull String ip) {
        String tokenId = getTokenId(token);

        Map<String, List<String>> activeIps = userActiveSessionsCache.get(
                toKey(userId), ReferenceIdentityMap::new
        );

        if (activeIps == null) {
            activeIps = new FixedSizeHashMap<>(differentIpsLimit);
        }
        List<String> validTokens = activeIps.get(ip);
        if (validTokens == null) {
            validTokens = new FixedSizeArrayList<>(oneIpLimit);
        }

        validTokens.add(tokenId);
        activeIps.put(ip, validTokens);

        userActiveSessionsCache.put(toKey(userId), activeIps);
    }

    public void invalidateSessionFor(@NonNull UserId userId, @NonNull JwtToken token, @NonNull String ip) {
        String tokenId = getTokenId(token);

        var activeIps = getIpsWithActiveSession(userId);
        long activeIpsAmount = activeIps.size();
        long currentSessionsAmount = getCurrentSessionsAmountForIp(userId, ip);

        if (activeIpsAmount > 0) {
            if (currentSessionsAmount > 0) {
                List<String> validTokens = activeIps.get(ip);
                validTokens.remove(tokenId);
            } else {
                activeIps.remove(ip);
            }

            userActiveSessionsCache.put(toKey(userId), activeIps);
        } else {
            userActiveSessionsCache.evictIfPresent(toKey(userId));
        }
    }

    private String getTokenId(JwtToken token) {
        var extracted = tokenFactory
                .parseTokenClaims(token)
                .getBody()
                .get(JwtTokenFactory.TOKEN_ID);
        var tokenId = Optional.ofNullable(extracted).map(Object::toString);
        if (tokenId.isEmpty()) {
            throw new RuntimeException("No token id provided!");
        }
        return tokenId.get();
    }

    private Long getCurrentSessionsAmountForIp(UserId userId, String ip) {
        return (long) extractValidTokens(userId, ip).size();
    }

    private List<String> extractValidTokens(UserId userId, String ip) {
        var activeIps = getIpsWithActiveSession(userId);
        var validTokens = activeIps.get(ip);

        if (validTokens == null) {
            validTokens = new FixedSizeArrayList<>(oneIpLimit);
            activeIps.replace(ip, validTokens);
            userActiveSessionsCache.put(toKey(userId), activeIps);
        }

        return validTokens;
    }

    private Map<String, List<String>> getIpsWithActiveSession(UserId userId) {
        Map<String, List<String>> activeIps = userActiveSessionsCache.get(
                toKey(userId), ReferenceIdentityMap::new
        );

        if (activeIps == null) {
            activeIps = new FixedSizeHashMap<>(differentIpsLimit);
            userActiveSessionsCache.put(toKey(userId), activeIps);
        }

        return activeIps;
    }


    private String toKey(UserId userId) {
        return userId.getId().toString();
    }

    private static class FixedSizeArrayList<T> extends ArrayList<T> {
        private final Long fixedSize;

        public FixedSizeArrayList(@NonNull Long fixedSize) {
            super();
            this.fixedSize = fixedSize;
        }

        @Override
        public boolean add(@NonNull T t) {
            if (fixedSize == 0) return false;
            if (size() < fixedSize) {
                return super.add(t);
            }

            this.remove(0);
            return add(t);
        }
    }

    private static class FixedSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final Long fixedSize;

        public FixedSizeHashMap(@NonNull Long fixedSize) {
            super();
            this.fixedSize = fixedSize;
        }

        @Override
        public V put(@NonNull K key, @NonNull V value) {
            if (fixedSize == 0) return null;

            if (size() < fixedSize) {
                return super.put(key, value);
            }

            remove(
                    this.entrySet().iterator().next().getKey()
            );
            return put(key, value);
        }
    }
}