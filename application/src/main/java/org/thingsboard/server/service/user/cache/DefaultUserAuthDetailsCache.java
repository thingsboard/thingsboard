/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.user.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultUserAuthDetailsCache implements UserAuthDetailsCache {

    private final UserService userService;

    @Value("${cache.userAuthDetails.maxSize:1000}")
    private int cacheMaxSize;
    @Value("${cache.userAuthDetails.timeToLiveInMinutes:30}")
    private int cacheValueTtl;
    private Cache<UserId, UserAuthDetails> cache;

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .build();
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId() != null) {
            if (event.getEntityId().getEntityType() == EntityType.USER) {
                evict(new UserId(event.getEntityId().getId()));
            }
        }
    }

    @Override
    public UserAuthDetails getUserAuthDetails(TenantId tenantId, UserId userId) {
        log.trace("Retrieving user with enabled credentials status for id {} for tenant {} from cache", userId, tenantId);
        return cache.get(userId, id -> userService.findUserAuthDetailsByUserId(tenantId, id));
    }

    public void evict(UserId userId) {
        cache.invalidate(userId);
        log.trace("Evicted record for user {} from cache", userId);
    }

}
