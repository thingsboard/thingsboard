// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
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
