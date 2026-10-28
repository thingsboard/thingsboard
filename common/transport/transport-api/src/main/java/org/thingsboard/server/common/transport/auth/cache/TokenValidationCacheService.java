// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Transport-side cache of device credentials validation results.
 * L1 is an in-process Caffeine cache in front of a remote (Redis) L2, the shared {@code TokenValidationCache}.
 * L1 reads are pure in-memory and run on the calling thread; all L2 I/O runs on a small dedicated bounded executor,
 * so a slow L2 never occupies the transport callback executor or the queue request template thread, and L1 hits
 * keep working while the executor is saturated; a saturated executor degrades to a cache miss.
 * All cache failures are swallowed (fail-open): the caller falls back to the regular core validation.
 */
@Slf4j
@Component
@TbTransportComponent
public class TokenValidationCacheService {

    static final int IO_THREADS = 4;
    static final int IO_QUEUE_CAPACITY = 1000;
    /**
     * Lifetime of the "tombstone" entries the core writes when cached validation results become stale without the
     * credentials ceasing to exist (credentials created, tenant reassigned). A tombstone is a miss for lookups and
     * blocks the {@code putIfAbsent} of any validation response produced before the invalidation, so it only needs
     * to outlive an in-flight transport-api round trip (10 s request timeout by default).
     */
    public static final long TOMBSTONE_TTL_SEC = 30;
    private static final long MAX_L1_SIZE = 100_000;
    private static final long WARN_INTERVAL_MS = 60_000;

    private final TbTransactionalCache<String, TokenValidationCacheEntryProto> l2;
    private final boolean enabled;
    private final long negativeTtlSec;
    private final long negativeTtlMs;
    private final Cache<String, TokenValidationCacheEntryProto> l1;
    private final ExecutorService ioExecutorService;

    // settable in tests
    Supplier<Long> clock = System::currentTimeMillis;
    Executor ioExecutor;
    private volatile long lastWarnTime;

    @Autowired
    public TokenValidationCacheService(
            @Autowired(required = false) @Qualifier("TokenValidationCache") Optional<TbTransactionalCache<String, TokenValidationCacheEntryProto>> l2,
            @Value("${cache.type:caffeine}") String cacheType,
            @Value("${transport.validation_cache.enabled:false}") boolean enabled,
            @Value("${transport.validation_cache.negative_ttl_sec:30}") int negativeTtlSec,
            @Value("${transport.validation_cache.local_ttl_sec:20}") int localTtlSec) {
        this.l2 = l2 != null ? l2.orElse(null) : null;
        this.enabled = enabled && this.l2 != null;
        this.negativeTtlSec = Math.max(negativeTtlSec, 0);
        this.negativeTtlMs = TimeUnit.SECONDS.toMillis(this.negativeTtlSec);
        // L1 only makes sense in front of a remote cache; in front of a local Caffeine L2 it would only add staleness
        this.l1 = this.enabled && "redis".equalsIgnoreCase(cacheType) && localTtlSec > 0 ?
                Caffeine.newBuilder().expireAfterWrite(localTtlSec, TimeUnit.SECONDS).maximumSize(MAX_L1_SIZE).build() : null;
        this.ioExecutorService = this.enabled ? new ThreadPoolExecutor(IO_THREADS, IO_THREADS, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(IO_QUEUE_CAPACITY), ThingsBoardThreadFactory.forName("transport-token-validation-cache"),
                new ThreadPoolExecutor.AbortPolicy()) : null;
        this.ioExecutor = ioExecutorService;
    }

    @PreDestroy
    public void destroy() {
        if (ioExecutorService != null) {
            ioExecutorService.shutdownNow();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Looks the token up: L1 is consulted on the calling thread (pure in-memory, so it keeps working even when the
     * cache I/O executor is saturated by a slow L2); only an L1 miss goes to the cache I/O executor for the L2 read.
     * Never fails: errors, a disabled cache and a saturated executor all complete with {@code Optional.empty()}
     * (a miss), the latter immediately on the calling thread.
     */
    public ListenableFuture<Optional<TokenValidationCacheEntryProto>> lookupAsync(String token) {
        if (!enabled) {
            return Futures.immediateFuture(Optional.empty());
        }
        try {
            if (l1 != null) {
                String key = cacheKey(token);
                TokenValidationCacheEntryProto entry = l1.getIfPresent(key);
                if (entry != null && isUsable(entry)) {
                    log.debug("[{}] Token validation cache hit in L1: {}", key, describe(entry));
                    return Futures.immediateFuture(Optional.of(entry));
                }
            }
            return Futures.submit(() -> lookup(token), ioExecutor);
        } catch (RejectedExecutionException e) {
            warnThrottled("Token validation cache lookup rejected (cache executor saturated), falling back to core validation");
            return Futures.immediateFuture(Optional.empty());
        } catch (Exception e) {
            warnFailOpen("lookup", null, e);
            return Futures.immediateFuture(Optional.empty());
        }
    }

    Optional<TokenValidationCacheEntryProto> lookup(String token) {
        if (!enabled) {
            return Optional.empty();
        }
        String key = null;
        try {
            key = cacheKey(token);
            TokenValidationCacheEntryProto entry = l1 != null ? l1.getIfPresent(key) : null;
            boolean l1Hit = entry != null;
            if (entry == null) {
                TbCacheValueWrapper<TokenValidationCacheEntryProto> wrapper = l2.get(key);
                entry = wrapper != null ? wrapper.get() : null;
                if (entry != null && l1 != null && isL1Cacheable(entry)) {
                    l1.put(key, entry);
                }
            }
            if (entry == null) {
                log.debug("[{}] Token validation cache miss", key);
                return Optional.empty();
            }
            if (entry.getTombstone()) {
                log.debug("[{}] Token validation cache miss (tombstone)", key);
                return Optional.empty();
            }
            if (isExpiredNegative(entry)) {
                log.debug("[{}] Token validation cache miss (expired negative entry)", key);
                return Optional.empty();
            }
            log.debug("[{}] Token validation cache hit in {}: {}", key, l1Hit ? "L1" : "L2", describe(entry));
            return Optional.of(entry);
        } catch (Exception e) {
            warnFailOpen("lookup", key, e);
            return Optional.empty();
        }
    }

    /**
     * Stores a core validation response asynchronously (fire-and-forget) on the cache I/O executor;
     * dropped silently when the executor is saturated.
     */
    public void store(String token, ValidateDeviceCredentialsResponseMsg response) {
        if (!enabled) {
            return;
        }
        ValidateDeviceCredentialsResponseMsg toCache;
        CredentialsValidationResult result = response.getResult();
        boolean positive;
        if (response.hasDeviceInfo() && result != CredentialsValidationResult.VALIDATION_INTERNAL_ERROR) {
            toCache = response.toBuilder().clearDeviceProfile().build();
            positive = true;
        } else if (!response.hasDeviceInfo() && result == CredentialsValidationResult.CREDENTIALS_NOT_FOUND && negativeTtlMs > 0) {
            toCache = response;
            positive = false;
        } else {
            return;
        }
        TokenValidationCacheEntryProto entry = TokenValidationCacheEntryProto.newBuilder()
                .setCreatedTime(clock.get())
                .setResponse(toCache)
                .build();
        try {
            ioExecutor.execute(() -> doStore(token, entry, positive));
        } catch (RejectedExecutionException e) {
            log.debug("Token validation cache store dropped (cache executor saturated)");
        }
    }

    /**
     * Drops the token's entry from L1 (immediately, on the calling thread) and L2 (fire-and-forget on the cache I/O
     * executor). Used when a cached entry turned out unusable (e.g. its device profile no longer resolves), so the
     * next request revalidates through the core instead of re-hitting the same broken entry until the TTL.
     */
    public void evictAsync(String token) {
        if (!enabled) {
            return;
        }
        try {
            String key = cacheKey(token);
            if (l1 != null) {
                l1.invalidate(key);
            }
            ioExecutor.execute(() -> {
                try {
                    l2.evict(key);
                    log.debug("[{}] Token validation cache entry evicted", key);
                } catch (Exception e) {
                    warnFailOpen("evict", key, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.debug("Token validation cache evict dropped (cache executor saturated)");
        } catch (Exception e) {
            warnFailOpen("evict", null, e);
        }
    }

    private void doStore(String token, TokenValidationCacheEntryProto entry, boolean positive) {
        String key = null;
        try {
            key = cacheKey(token);
            if (positive) {
                // never overwrite a revoked marker or tombstone the core may have written after this response was
                // produced; L1 is not populated here, so the next lookup reads (and caches) whatever won in L2
                l2.putIfAbsent(key, entry);
            } else {
                // negatives carry their own short expiry (enforced in Redis, not only logically), so unauthenticated
                // junk-token traffic cannot keep ~rate x TTL keys alive in the shared cache; putIfAbsent so a late
                // negative cannot overwrite a tombstone written by the core right after credentials were created
                l2.putIfAbsent(key, entry, negativeTtlSec);
            }
            log.debug("[{}] Token validation cache store: {}", key, describe(entry));
        } catch (Exception e) {
            warnFailOpen("store", key, e);
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

    /**
     * Written by the core (with {@link #TOMBSTONE_TTL_SEC}) when cached validation results for a token became stale
     * while the credentials stayed valid: credentials created (clears a stale negative) or the device was reassigned
     * to another tenant (clears a stale positive). Lookups treat it as a miss; it exists to make late
     * {@code putIfAbsent} stores of pre-invalidation responses fail instead of repopulating the key.
     */
    public static TokenValidationCacheEntryProto tombstoneMarker(long now) {
        return TokenValidationCacheEntryProto.newBuilder()
                .setCreatedTime(now)
                .setTombstone(true)
                .build();
    }

    /**
     * Only positive entries go to L1. Negatives and revoked markers stay out: the core clears or replaces them in L2
     * when the credentials get (re-)created, and that must take effect within seconds, not after the L1 TTL —
     * otherwise this pod would keep rejecting a token that is valid again. An L1 miss on them costs one L2 read,
     * never a core round trip.
     */
    private static boolean isL1Cacheable(TokenValidationCacheEntryProto entry) {
        return !entry.getRevoked() && !entry.getTombstone() && entry.getResponse().hasDeviceInfo();
    }

    private boolean isUsable(TokenValidationCacheEntryProto entry) {
        return !entry.getTombstone() && !isExpiredNegative(entry);
    }

    private boolean isExpiredNegative(TokenValidationCacheEntryProto entry) {
        if (entry.getRevoked() || entry.getResponse().hasDeviceInfo()) {
            return false;
        }
        return clock.get() - entry.getCreatedTime() > negativeTtlMs;
    }

    private static String describe(TokenValidationCacheEntryProto entry) {
        if (entry.getRevoked()) {
            return "revoked";
        }
        if (entry.getTombstone()) {
            return "tombstone";
        }
        return entry.getResponse().hasDeviceInfo() ? "positive" : "negative";
    }

    private void warnFailOpen(String op, String key, Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastWarnTime >= WARN_INTERVAL_MS) {
            lastWarnTime = now;
            log.warn("[{}] Token validation cache {} failed, falling back to core validation: {}", key, op, e.toString());
        } else {
            log.debug("[{}] Token validation cache {} failed, falling back to core validation", key, op, e);
        }
    }

    private void warnThrottled(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarnTime >= WARN_INTERVAL_MS) {
            lastWarnTime = now;
            log.warn(message);
        } else {
            log.debug(message);
        }
    }

}
