// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsValidationResult;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Transport-side cache of device credentials validation results.
 * L1 is an in-process Caffeine cache, L2 is the shared {@code TokenValidationCache}.
 * All cache failures are swallowed (fail-open): the caller falls back to the regular core validation.
 */
@Slf4j
@Component
@TbTransportComponent
public class TokenValidationCacheService {

    private static final long MAX_L1_SIZE = 100_000;
    private static final long WARN_INTERVAL_MS = 60_000;

    private final TbTransactionalCache<String, TokenValidationCacheEntryProto> l2;
    private final boolean enabled;
    private final long negativeTtlMs;
    private final Cache<String, TokenValidationCacheEntryProto> l1;

    // settable in tests
    Supplier<Long> clock = System::currentTimeMillis;
    private volatile long lastWarnTime;

    @Autowired
    public TokenValidationCacheService(
            @Autowired(required = false) @Qualifier("TokenValidationCache") Optional<TbTransactionalCache<String, TokenValidationCacheEntryProto>> l2,
            @Value("${transport.validation_cache.enabled:false}") boolean enabled,
            @Value("${transport.validation_cache.negative_ttl_sec:30}") int negativeTtlSec,
            @Value("${transport.validation_cache.local_ttl_sec:20}") int localTtlSec) {
        this.l2 = l2 != null ? l2.orElse(null) : null;
        this.enabled = enabled && this.l2 != null;
        this.negativeTtlMs = TimeUnit.SECONDS.toMillis(Math.max(negativeTtlSec, 0));
        this.l1 = localTtlSec > 0 ?
                Caffeine.newBuilder().expireAfterWrite(localTtlSec, TimeUnit.SECONDS).maximumSize(MAX_L1_SIZE).build() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<TokenValidationCacheEntryProto> lookup(String token) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            String key = cacheKey(token);
            TokenValidationCacheEntryProto entry = l1 != null ? l1.getIfPresent(key) : null;
            if (entry == null) {
                TbCacheValueWrapper<TokenValidationCacheEntryProto> wrapper = l2.get(key);
                entry = wrapper != null ? wrapper.get() : null;
                if (entry != null && l1 != null) {
                    l1.put(key, entry);
                }
            }
            if (entry == null || isExpiredNegative(entry)) {
                return Optional.empty();
            }
            return Optional.of(entry);
        } catch (Exception e) {
            warnFailOpen("lookup", e);
            return Optional.empty();
        }
    }

    public void store(String token, ValidateDeviceCredentialsResponseMsg response) {
        if (!enabled) {
            return;
        }
        try {
            ValidateDeviceCredentialsResponseMsg toCache;
            CredentialsValidationResult result = response.getResult();
            if (response.hasDeviceInfo() && result != CredentialsValidationResult.VALIDATION_INTERNAL_ERROR) {
                toCache = response.toBuilder().clearDeviceProfile().build();
            } else if (!response.hasDeviceInfo() && result == CredentialsValidationResult.CREDENTIALS_NOT_FOUND && negativeTtlMs > 0) {
                toCache = response;
            } else {
                return;
            }
            String key = cacheKey(token);
            TokenValidationCacheEntryProto entry = TokenValidationCacheEntryProto.newBuilder()
                    .setCreatedTime(clock.get())
                    .setResponse(toCache)
                    .build();
            l2.put(key, entry);
            if (l1 != null) {
                l1.put(key, entry);
            }
        } catch (Exception e) {
            warnFailOpen("store", e);
        }
    }

    public static String cacheKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "vt:" + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static TokenValidationCacheEntryProto revokedMarker(long now) {
        return TokenValidationCacheEntryProto.newBuilder()
                .setCreatedTime(now)
                .setRevoked(true)
                .setResponse(ValidateDeviceCredentialsResponseMsg.newBuilder()
                        .setResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND)
                        .build())
                .build();
    }

    private boolean isExpiredNegative(TokenValidationCacheEntryProto entry) {
        if (entry.getRevoked() || entry.getResponse().hasDeviceInfo()) {
            return false;
        }
        return clock.get() - entry.getCreatedTime() > negativeTtlMs;
    }

    private void warnFailOpen(String op, Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastWarnTime >= WARN_INTERVAL_MS) {
            lastWarnTime = now;
            log.warn("Token validation cache {} failed, falling back to core validation: {}", op, e.toString());
        } else {
            log.debug("Token validation cache {} failed, falling back to core validation", op, e);
        }
    }

}
