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
 * Keeps the transport token-validation cache consistent with device credentials changes: rotated or deleted ids get
 * a "revoked" marker; a (re-)created id, or an id whose cached data went stale without a credentials change (tenant
 * reassignment), gets a short-lived "tombstone" — a miss for lookups that also blocks in-flight stores of
 * pre-invalidation validation responses from repopulating the key (a bare delete would not).
 * Fail-open: cache outages never break credentials saves. The security-relevant write (the revoked marker)
 * runs first, so a failure on the lower-stakes tombstone write can never skip it.
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
        long now = System.currentTimeMillis();
        if (event.isDeleted()) {
            putMarker(event.getNewCredentialsId(), TokenValidationCacheService.revokedMarker(now), 0, "revoked");
            return;
        }
        // the revoked marker first: if the tombstone write below fails, the old token must already be dead
        String oldId = event.getOldCredentialsId();
        if (oldId != null && !oldId.isEmpty() && !oldId.equals(event.getNewCredentialsId())) {
            putMarker(oldId, TokenValidationCacheService.revokedMarker(now), 0, "revoked");
        }
        putMarker(event.getNewCredentialsId(), TokenValidationCacheService.tombstoneMarker(now),
                TokenValidationCacheService.TOMBSTONE_TTL_SEC, "tombstone");
    }

    private void putMarker(String credentialsId, TokenValidationCacheEntryProto marker, long ttlSec, String kind) {
        String key = null;
        try {
            key = TokenValidationCacheService.cacheKey(credentialsId);
            cache.put(key, marker, ttlSec);
        } catch (Exception e) {
            // never log the credentials id: it is a secret (access token); the hashed cache key is safe
            log.warn("[{}] Failed to write {} marker to the token validation cache", key, kind, e);
        }
    }

}
