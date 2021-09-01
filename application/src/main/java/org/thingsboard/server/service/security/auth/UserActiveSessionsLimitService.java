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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.jetbrains.annotations.NotNull;
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

    private final Long machinesLimit;
    private final Long oneMachineLimit;
    private JwtTokenFactory tokenFactory;

    public UserActiveSessionsLimitService(
            CacheManager cacheManager,
            @Value("${security.sessionsLimit.machinesLimit}") Long machinesLimit,
            @Value("${security.sessionsLimit.oneMachineLimit}") Long oneMachineLimit,
            JwtTokenFactory tokenFactory
    ) {
        this.cacheManager = cacheManager;
        this.machinesLimit = machinesLimit;
        this.oneMachineLimit = oneMachineLimit;
        this.tokenFactory = tokenFactory;
    }

    // TODO: 10.12.21 REVIEW names

    @PostConstruct
    protected void initCache() {
        userActiveSessionsCache = cacheManager.getCache(CacheConstants.USER_ACTIVE_SESSIONS_CACHE);
    }

    public boolean sessionIsValidFor(UserId userId, JwtToken token, String ip) {
        if (machinesLimit == 0 && oneMachineLimit == 0)
            return true;

        Optional<String> tokenId = getTokenId(token);
        List<String> validTokens = extractValidTokens(userId, ip);

        // TODO: 10.12.21 REVIEW
        return validTokens.contains(tokenId.orElseThrow(() -> new RuntimeException("No token id provided!")));
    }

    public void validateSessionFor(UserId userId, JwtToken token, String ip) {
        Jws<Claims> claimsJws = tokenFactory.parseTokenClaims(token);
        Optional<String> tokenId = Optional.ofNullable(claimsJws.getBody().get(JwtTokenFactory.TOKEN_ID)).map(Object::toString);

        Map<String, List<String>> loggedMachines = userActiveSessionsCache.get(
                toKey(userId), Map.class
        );

        if (loggedMachines == null) {
            loggedMachines = new FixedSizeHashMap<>(machinesLimit);
        }
        List<String> validTokens = loggedMachines.get(ip);
        if (validTokens == null) {
            validTokens = new FixedSizeArrayList<>(oneMachineLimit);
        }

        validTokens.add(tokenId.get());
        loggedMachines.put(ip, validTokens);

        userActiveSessionsCache.put(toKey(userId), loggedMachines);
    }

    public void invalidateSessionFor(UserId userId, JwtToken token, String ip) {
        Optional<String> tokenId = getTokenId(token);

        var loggedMachines = getMachinesBy(userId);
        long currentMachinesAmount = loggedMachines.size();
        long currentSessionsAmount = getCurrentSessionsAmountOnMachine(userId, ip);

        if (currentMachinesAmount > 0) {
            if (currentSessionsAmount > 0) {
                List<String> validTokens = loggedMachines.get(ip);
                validTokens.remove(tokenId.get());
            } else {
                loggedMachines.remove(ip);
            }

            userActiveSessionsCache.put(toKey(userId), loggedMachines);
        } else {
            userActiveSessionsCache.evictIfPresent(toKey(userId));
        }
    }

    @NotNull
    private Optional<String> getTokenId(JwtToken token) {
        return Optional.ofNullable(tokenFactory.parseTokenClaims(token).getBody().get(JwtTokenFactory.TOKEN_ID))
                .map(Object::toString);
    }

    private Long getCurrentSessionsAmountOnMachine(UserId userId, String ip) {
        extractValidTokens(userId, ip);
        List<String> tokens = extractValidTokens(userId, ip);

        return (long) tokens.size();
    }

    private List<String> extractValidTokens(UserId userId, String ip) {
        var loggedMachines = getMachinesBy(userId);
        var validTokens = loggedMachines.get(ip);

        if (validTokens == null) {
            validTokens = new FixedSizeArrayList<>(oneMachineLimit);
            loggedMachines.replace(ip, validTokens);
            userActiveSessionsCache.put(toKey(userId), loggedMachines);
        }

        return validTokens;
    }

    @NotNull
    private Map<String, List<String>> getMachinesBy(UserId userId) {
        Map<String, List<String>> loggedMachines = userActiveSessionsCache.get(
                toKey(userId), Map.class
        );

        if (loggedMachines == null) {
            loggedMachines = new FixedSizeHashMap<>(machinesLimit);
            userActiveSessionsCache.put(toKey(userId), loggedMachines);
        }

        return loggedMachines;
    }


    private String toKey(UserId userId) {
        return userId.getId().toString();
    }

    private static class FixedSizeArrayList<T> extends ArrayList<T> {
        private final Long fixedSize;

        public FixedSizeArrayList(Long fixedSize) {
            super();
            this.fixedSize = fixedSize;
        }

        @Override
        public boolean add(T t) {
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

        public FixedSizeHashMap(Long fixedSize) {
            super();
            this.fixedSize = fixedSize;
        }

        @Override
        public V put(@NotNull K key, @NotNull V value) {
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