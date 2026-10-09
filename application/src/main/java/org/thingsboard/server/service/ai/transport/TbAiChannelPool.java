// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Channels to TB AI on this tb-core node, one per user and client origin in the common case.
 * <ul>
 * <li>Calls of one user share a channel; concurrent first calls share one handshake.</li>
 * <li>At most {@code maxConnections} pooled channels per node. When full, the least recently used idle channel is
 * closed; if none is idle the call gets a transient channel that closes after it, so a full pool never delays or
 * refuses a call.</li>
 * <li>One sweeper closes channels idle for {@code idleTimeout} and retires channels older than {@code maxAge}, which
 * bounds how long a revoked token or changed subscription keeps working on an open channel.</li>
 * <li>Background calls (housekeeper deletions) always use transient channels.</li>
 * </ul>
 */
@Slf4j
@Component
@TbCoreComponent
class TbAiChannelPool {

    record PoolKey(TenantId tenantId, UserId userId) {}

    private static final Duration SWEEP_INTERVAL = Duration.ofSeconds(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_ACQUIRE_ATTEMPTS = 3;

    private final TbAiChannelClient channelClient;
    private final int maxConnections;
    private final int maxOperationsPerConnection;
    private final long idleTimeoutNanos;
    private final long maxAgeNanos;
    private final LongSupplier nanoClock;
    private final ConcurrentMap<PoolKey, Group> groups = new ConcurrentHashMap<>();
    private final AtomicInteger pooledConnections = new AtomicInteger();
    private final ScheduledExecutorService sweeper;

    @Autowired
    TbAiChannelPool(TbAiChannelClient channelClient,
                    @Value("${ai.channel.pool.max_connections:2000}") int maxConnections,
                    @Value("${ai.channel.pool.max_operations_per_connection:20}") int maxOperationsPerConnection,
                    @Value("${ai.channel.pool.idle_timeout_seconds:60}") long idleTimeoutSeconds,
                    @Value("${ai.channel.pool.max_age_minutes:10}") long maxAgeMinutes) {
        this(channelClient, maxConnections, maxOperationsPerConnection, Duration.ofSeconds(idleTimeoutSeconds),
                Duration.ofMinutes(maxAgeMinutes), System::nanoTime, true);
    }

    TbAiChannelPool(TbAiChannelClient channelClient, int maxConnections, int maxOperationsPerConnection,
                    Duration idleTimeout, Duration maxAge, LongSupplier nanoClock, boolean startSweeper) {
        this.channelClient = channelClient;
        this.maxConnections = maxConnections;
        this.maxOperationsPerConnection = maxOperationsPerConnection;
        this.idleTimeoutNanos = idleTimeout.toNanos();
        this.maxAgeNanos = maxAge.toNanos();
        this.nanoClock = nanoClock;
        if (startSweeper) {
            this.sweeper = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-ai-channel-pool"));
            this.sweeper.scheduleWithFixedDelay(this::sweep, SWEEP_INTERVAL.toMillis(), SWEEP_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            this.sweeper = null;
        }
    }

    /**
     * A channel with one slot reserved for the caller, who must give it back through
     * {@link TbAiPooledConnection#finish}. {@code fresh} skips the open channels, for a retry after a channel refused
     * the call. Fails with {@link TbAiChannelUnavailableException} or {@link TbAiChannelRejectedException}.
     */
    CompletableFuture<TbAiPooledConnection> acquire(TbAiTurnContext context, boolean fresh) {
        return acquire(context, fresh, 1);
    }

    private CompletableFuture<TbAiPooledConnection> acquire(TbAiTurnContext context, boolean fresh, int attempt) {
        if (context.isBackground()) {
            return connect(context, null, false);
        }
        PoolKey key = new PoolKey(context.tenantId(), context.userId());
        String origin = context.clientRequest().clientOrigin();
        boolean exactOrigin = context.clientRequest().exactOrigin();
        CompletableFuture<TbAiPooledConnection> pending = null;
        if (!fresh) {
            Group group = groups.computeIfAbsent(key, k -> new Group());
            synchronized (group) {
                long now = nanoClock.getAsLong();
                for (int i = group.connections.size() - 1; i >= 0; i--) {
                    TbAiPooledConnection connection = group.connections.get(i);
                    if ((!exactOrigin || origin.equals(connection.origin())) && connection.tryReserve(now)) {
                        return CompletableFuture.completedFuture(connection);
                    }
                }
                pending = exactOrigin ? group.connecting.get(origin) : group.connecting.values().stream().findFirst().orElse(null);
            }
        }
        if (pending != null) {
            return pending.thenCompose(connection -> connection.tryReserve(nanoClock.getAsLong())
                    ? CompletableFuture.completedFuture(connection)
                    : retryAcquire(context, attempt));
        }
        boolean pooled = tryTakePoolSlot() || (evictIdleConnection() && tryTakePoolSlot());
        if (!pooled) {
            log.debug("[{}] AI channel pool is full ({}), using a transient channel", key, maxConnections);
            return connect(context, key, false);
        }
        Group group = groups.computeIfAbsent(key, k -> new Group());
        synchronized (group) {
            if (!fresh) {
                CompletableFuture<TbAiPooledConnection> started = group.connecting.get(origin);
                if (started != null) {
                    pooledConnections.decrementAndGet();
                    return started.thenCompose(connection -> connection.tryReserve(nanoClock.getAsLong())
                            ? CompletableFuture.completedFuture(connection)
                            : retryAcquire(context, attempt));
                }
            }
            CompletableFuture<TbAiPooledConnection> ready = connect(context, key, true);
            group.connecting.put(origin, ready);
            ready.whenComplete((connection, error) -> {
                boolean added = false;
                synchronized (group) {
                    group.connecting.remove(origin, ready);
                    // A channel closed right after hello has already left the pool (removed() then found nothing).
                    if (connection != null && connection.isMultiplexed() && !connection.isRemoved()) {
                        group.connections.add(connection);
                        added = true;
                    }
                }
                if (connection != null && !added) {
                    // An older TB AI closes the channel after one call: nothing to pool.
                    release(connection);
                }
            });
            return ready;
        }
    }

    private CompletableFuture<TbAiPooledConnection> retryAcquire(TbAiTurnContext context, int attempt) {
        if (attempt >= MAX_ACQUIRE_ATTEMPTS) {
            return CompletableFuture.failedFuture(new TbAiChannelUnavailableException("No AI channel with free capacity", null));
        }
        return acquire(context, true, attempt + 1);
    }

    private CompletableFuture<TbAiPooledConnection> connect(TbAiTurnContext context, PoolKey key, boolean pooled) {
        var connection = new TbAiPooledConnection(key, context.clientRequest().clientOrigin(), pooled,
                maxOperationsPerConnection, channelClient.codec(), this, nanoClock.getAsLong());
        channelClient.connect(handshakeTokenProvider(context), connection)
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(CONNECT_TIMEOUT)
                .subscribe(connection::opened, error -> connection.failed(toConnectError(error)));
        return connection.ready()
                .orTimeout(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((ready, error) -> {
                    if (error != null) {
                        connection.close(ChannelProtocol.CLOSE_NORMAL, "Handshake failed");
                    }
                })
                .exceptionallyCompose(error -> CompletableFuture.failedFuture(unwrap(error)));
    }

    void release(TbAiPooledConnection connection) {
        if (connection.releasePoolSlot()) {
            pooledConnections.decrementAndGet();
        }
    }

    /**
     * Called once when a channel closes or fails to open.
     */
    void removed(TbAiPooledConnection connection) {
        Group group = connection.key() != null ? groups.get(connection.key()) : null;
        if (group != null) {
            synchronized (group) {
                group.connections.remove(connection);
            }
        }
        release(connection);
    }

    int pooledConnections() {
        return pooledConnections.get();
    }

    void sweep() {
        try {
            long now = nanoClock.getAsLong();
            int closed = 0;
            for (Map.Entry<PoolKey, Group> entry : groups.entrySet()) {
                List<TbAiPooledConnection> snapshot;
                synchronized (entry.getValue()) {
                    snapshot = new ArrayList<>(entry.getValue().connections);
                }
                for (TbAiPooledConnection connection : snapshot) {
                    if (connection.sweep(now, idleTimeoutNanos, maxAgeNanos)) {
                        closed++;
                    }
                }
                groups.computeIfPresent(entry.getKey(), (key, group) -> {
                    synchronized (group) {
                        return group.connections.isEmpty() && group.connecting.isEmpty() ? null : group;
                    }
                });
            }
            if (closed > 0 || log.isTraceEnabled()) {
                log.debug("AI channel pool: {} pooled channel(s), {} user(s), {} closed as idle or aged",
                        pooledConnections.get(), groups.size(), closed);
            }
        } catch (Exception e) {
            log.warn("Failed to sweep the AI channel pool", e);
        }
    }

    @PreDestroy
    void shutdown() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
        groups.values().forEach(group -> {
            List<TbAiPooledConnection> snapshot;
            synchronized (group) {
                snapshot = new ArrayList<>(group.connections);
            }
            snapshot.forEach(connection -> connection.close(ChannelProtocol.CLOSE_GOING_AWAY, "Server is shutting down"));
        });
    }

    private boolean tryTakePoolSlot() {
        while (true) {
            int current = pooledConnections.get();
            if (current >= maxConnections) {
                return false;
            }
            if (pooledConnections.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private boolean evictIdleConnection() {
        TbAiPooledConnection oldest = null;
        long oldestUse = Long.MAX_VALUE;
        for (Group group : groups.values()) {
            synchronized (group) {
                for (TbAiPooledConnection connection : group.connections) {
                    if (connection.isIdle() && connection.lastUsedNanos() < oldestUse) {
                        oldest = connection;
                        oldestUse = connection.lastUsedNanos();
                    }
                }
            }
        }
        if (oldest == null || !oldest.evictIfIdle()) {
            return false;
        }
        // Free the evicted channel's slot now rather than when its close is reported, so the caller can take it.
        release(oldest);
        return true;
    }

    private static TbAiClient.TokenProvider handshakeTokenProvider(TbAiTurnContext context) {
        TbAiClient.TokenProvider delegate = context.tokenProvider();
        String acceptLanguage = context.acceptLanguage();
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return delegate;
        }
        return new TbAiClient.TokenProvider() {
            @Override
            public String getToken() {
                return delegate.getToken();
            }

            @Override
            public Map<String, String> getAdditionalInfo() {
                var headers = new HashMap<>(delegate.getAdditionalInfo());
                headers.put(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
                return headers;
            }
        };
    }

    private static Throwable toConnectError(Throwable error) {
        for (Throwable cause : ExceptionUtils.getThrowableList(error)) {
            if (cause instanceof WebSocketClientHandshakeException handshake && handshake.response() != null) {
                int status = handshake.response().status().code();
                if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                    return new TbAiChannelRejectedException(HttpStatus.valueOf(status).getReasonPhrase());
                }
            }
            if (cause instanceof TbAiChannelRejectedException || cause instanceof TbAiChannelUnavailableException) {
                return cause;
            }
        }
        return new TbAiChannelUnavailableException("Cannot open the AI channel: " + ExceptionUtils.getRootCauseMessage(error), error);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof TbAiChannelRejectedException || cause instanceof TbAiChannelUnavailableException) {
            return cause;
        }
        if (cause instanceof java.util.concurrent.TimeoutException) {
            return new TbAiChannelUnavailableException("AI channel handshake timed out", cause);
        }
        return new TbAiChannelUnavailableException("Cannot open the AI channel: " + ExceptionUtils.getRootCauseMessage(cause), cause);
    }

    private static final class Group {

        private final List<TbAiPooledConnection> connections = new ArrayList<>();
        private final Map<String, CompletableFuture<TbAiPooledConnection>> connecting = new HashMap<>();

    }

}
