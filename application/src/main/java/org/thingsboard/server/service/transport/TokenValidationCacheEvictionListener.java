// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.dao.device.DeviceCredentialsEvictEvent;
import org.thingsboard.server.dao.device.DeviceCredentialsEvictionHook;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

import java.util.Optional;

/**
 * Keeps the transport token-validation cache consistent with device credentials changes:
 * new credentials id is evicted (so stale negative entries disappear), rotated or deleted ids get a "revoked" marker.
 * Fail-open: cache outages never break credentials saves.
 */
@Slf4j
@Component
public class TokenValidationCacheEvictionListener implements DeviceCredentialsEvictionHook {

    private final TbTransactionalCache<String, TokenValidationCacheEntryProto> cache;

    @Autowired
    public TokenValidationCacheEvictionListener(
            @Autowired(required = false) @Qualifier("TokenValidationCache") Optional<TbTransactionalCache<String, TokenValidationCacheEntryProto>> cache) {
        this.cache = cache != null ? cache.orElse(null) : null;
    }

    @Override
    public void onDeviceCredentialsEvict(DeviceCredentialsEvictEvent event) {
        if (cache == null) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (event.isDeleted()) {
                cache.put(TokenValidationCacheService.cacheKey(event.getNewCredentialsId()), TokenValidationCacheService.revokedMarker(now));
                return;
            }
            cache.evict(TokenValidationCacheService.cacheKey(event.getNewCredentialsId()));
            String oldId = event.getOldCredentialsId();
            if (oldId != null && !oldId.isEmpty() && !oldId.equals(event.getNewCredentialsId())) {
                cache.put(TokenValidationCacheService.cacheKey(oldId), TokenValidationCacheService.revokedMarker(now));
            }
        } catch (Exception e) {
            log.warn("Failed to update token validation cache on credentials change [{}]", event, e);
        }
    }

}
