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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.UserId;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Service
public class UserActiveSessionsLimitService {
    private final CacheManager cacheManager;
    private Cache loginLimitCache;

    private final Long maxLoginUsers;

    public UserActiveSessionsLimitService(CacheManager cacheManager,
                                          @Value("${security.max_logged_in_users}") Long maxActiveSessions) {
        this.cacheManager = cacheManager;
        this.maxLoginUsers = maxActiveSessions;
    }


    @PostConstruct
    protected void initCache() {
        loginLimitCache = cacheManager.getCache(CacheConstants.USER_ACTIVE_SESSIONS_CACHE);
    }

/*
    Every request must be checked by ip.
    Session will expire in specific amount of time stored in cache - then logout.
 */

    public boolean isOverLimit(UserId userId) {
        if (maxLoginUsers == 0)
            return false;
        long currentAmount = getCurrentAmount(userId);
        return currentAmount >= maxLoginUsers;
    }

    public void decreaseCurrentActiveSessions(UserId userId) {
        long currentAmount = getCurrentAmount(userId);
        if (currentAmount > 0) {
            loginLimitCache.put(toKey(userId), currentAmount - 1);
        } else {
            loginLimitCache.evict(toKey(userId));
        }
    }

    public void increaseCurrentActiveSessions(UserId userId) {
        long currentAmount = getCurrentAmount(userId);
        loginLimitCache.put(toKey(userId), currentAmount + 1);
    }

    public Long getCurrentAmount(UserId userId) {
        return Optional.ofNullable(loginLimitCache.get(toKey(userId), Long.class))
                .orElseGet(() -> {
                    loginLimitCache.put(toKey(userId), 0L);
                    return 0L;
                });
    }

    private String toKey(UserId userId) {
        return userId.getId().toString();
    }

}
