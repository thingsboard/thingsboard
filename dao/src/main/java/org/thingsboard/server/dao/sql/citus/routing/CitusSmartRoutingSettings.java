// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Getter
public class CitusSmartRoutingSettings {

    /**
     * Hikari clamps {@code validationTimeout} to a 250ms minimum. The worker pool derives
     * {@code validationTimeout = workerConnectionTimeoutMs / 2}, so a connection timeout below this
     * floor would push validationTimeout up to (or past) the connect budget, defeating the
     * "validation strictly shorter than connect" invariant in {@code CitusWorkerRegistry}. Reject such
     * values at startup. 500ms keeps the derived validationTimeout at the 250ms Hikari floor.
     */
    static final long MIN_WORKER_CONNECTION_TIMEOUT_MS = 500L;

    @Value("${database.citus.smart_routing.enabled:${database.citus.enabled:false}}")
    private boolean enabled;

    @Value("${database.citus.smart_routing.worker_pool_size:8}")
    private int workerPoolSize;

    @Value("${database.citus.smart_routing.worker_connection_timeout_ms:10000}")
    private long workerConnectionTimeoutMs;

    /**
     * Debounce window for the error-triggered routing refresh ({@link CitusFailoverRefreshTrigger}): at
     * most one failover-triggered refresh runs per this many milliseconds.
     */
    @Value("${database.citus.smart_routing.failover_refresh_debounce_ms:10000}")
    private long failoverRefreshDebounceMs;

    @Value("${database.citus.smart_routing.worker_host_overrides:}")
    private String workerHostOverridesRaw;

    /**
     * Cached parse of {@link #workerHostOverridesRaw} (the raw value is immutable after binding), populated
     * lazily on first access by {@link #getWorkerHostOverrides()} so the per-call re-parse on every reconcile
     * is avoided.
     */
    private Map<String, String> workerHostOverrides;

    /**
     * Returns the immutable {@code nodename -> host:port} override map (see {@link #parseWorkerHostOverrides}),
     * parsing the raw value once on first access and caching it. Never null.
     */
    public Map<String, String> getWorkerHostOverrides() {
        if (workerHostOverrides == null) {
            workerHostOverrides = parseWorkerHostOverrides(workerHostOverridesRaw);
        }
        return workerHostOverrides;
    }

    /**
     * Fail-fast validation at bean init: reject a {@code worker_connection_timeout_ms} below
     * {@link #MIN_WORKER_CONNECTION_TIMEOUT_MS}. Below that floor the worker pool's derived
     * {@code validationTimeout} (half the connect budget) would collide with Hikari's hard 250ms
     * validation minimum and no longer stay strictly shorter than the connect timeout.
     */
    @PostConstruct
    public void validate() {
        if (workerConnectionTimeoutMs < MIN_WORKER_CONNECTION_TIMEOUT_MS) {
            throw new IllegalStateException("database.citus.smart_routing.worker_connection_timeout_ms must be >= " +
                    MIN_WORKER_CONNECTION_TIMEOUT_MS + "ms but was " + workerConnectionTimeoutMs +
                    "ms; below this the derived worker pool validationTimeout (half the connect budget) collides with " +
                    "Hikari's 250ms validation floor and stops being shorter than the connect timeout");
        }
    }

    /**
     * Parses the comma-separated {@code nodename=host:port} override string into a map.
     * Whitespace is trimmed, blank entries are skipped, a trailing comma is tolerated and
     * malformed entries (no '=' separator or blank key) are ignored. The value part is an
     * opaque {@code host:port} string and is kept as-is.
     */
    static Map<String, String> parseWorkerHostOverrides(String raw) {
        Map<String, String> overrides = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyMap();
        }
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String nodeName = trimmed.substring(0, separator).trim();
            String hostPort = trimmed.substring(separator + 1).trim();
            if (nodeName.isEmpty() || hostPort.isEmpty()) {
                continue;
            }
            overrides.put(nodeName, hostPort);
        }
        return Collections.unmodifiableMap(overrides);
    }

}
