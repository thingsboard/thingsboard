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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.UserId;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginAmountLimitService {
    private final CacheManager cacheManager;
    private Cache loginLimitCache;

    @Value("${security.max_login_users}")
    private Long maxLoginUsers;

    @PostConstruct
    protected void initCache() {
        loginLimitCache = cacheManager.getCache(CacheConstants.LOGIN_AMOUNT_LIMIT_CACHE);
    }

    public boolean isOverLimit(UserId userId) {
        Boolean isOverLimit = getCurrentAmount(userId).map(currentAmount -> currentAmount >= maxLoginUsers).orElse(false);
        if (!isOverLimit)
            increaseCurrentLoginAmount(userId);
        return isOverLimit;
    }

    public void decreaseCurrentLoginAmount(UserId userId) {
        getCurrentAmount(userId).ifPresent(currentAmount -> loginLimitCache.put(toKey(userId), currentAmount - 1));
    }

    public void increaseCurrentLoginAmount(UserId userId) {
        getCurrentAmount(userId).ifPresent(currentAmount -> loginLimitCache.put(toKey(userId), currentAmount + 1));
    }

    private Optional<Long> getCurrentAmount(UserId userId) {
        return Optional.ofNullable(loginLimitCache.get(toKey(userId), Long.class));
    }

    private String toKey(UserId userId) {
        return userId.getId().toString();
    }

}
