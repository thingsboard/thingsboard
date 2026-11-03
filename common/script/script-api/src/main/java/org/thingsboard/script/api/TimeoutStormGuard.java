// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Detects pod-wide script timeout storms: many distinct scripts timing out within a short window.
 * <p>
 * A single slow script (infinite loop, heavy converter) times out on its own. When several different scripts time out
 * at the same moment, the cause is the infrastructure (Kafka or JS executor stall, network blip, CPU starvation), so
 * those timeouts should not push healthy scripts into the blocklist.
 * <p>
 * A storm starts when {@code minScripts} distinct scripts time out within {@code windowMs}, and ends after
 * {@code windowMs} without any timeout. Timeouts are suppressed for at most {@code maxDurationMs} from the storm start;
 * after that they are counted again, so a sustained overload still sheds load.
 */
@Slf4j
public class TimeoutStormGuard {

    private final String name;
    private final long windowMs;
    private final int minScripts;
    private final long maxDurationMs;

    private final Deque<ScriptTimeout> recentTimeouts = new ArrayDeque<>();
    private final Map<UUID, Integer> recentTimeoutsPerScript = new HashMap<>();
    private long stormStartTs = -1;
    private long lastTimeoutTs = -1;
    private long suppressedInStorm;

    public TimeoutStormGuard(String name, long windowMs, int minScripts, long maxDurationMs) {
        this.name = name;
        this.windowMs = windowMs;
        this.minScripts = Math.max(2, minScripts);
        this.maxDurationMs = maxDurationMs;
    }

    /**
     * Registers a script timeout.
     *
     * @return the decision: whether this timeout must not be counted towards the blocklist, and, if this timeout
     * started a storm, the scripts that timed out within the window (their timeouts-only blocklist entries should be
     * dropped)
     */
    public synchronized Decision onTimeout(UUID scriptId, long now) {
        if (stormStartTs >= 0 && now - lastTimeoutTs > windowMs) {
            log.warn("[{}] Timeout storm ended: {} timeouts were not counted towards the blocklist, storm lasted {} ms",
                    name, suppressedInStorm, lastTimeoutTs - stormStartTs);
            stormStartTs = -1;
        }
        lastTimeoutTs = now;
        evictOlderThan(now - windowMs);
        recentTimeouts.addLast(new ScriptTimeout(now, scriptId));
        recentTimeoutsPerScript.merge(scriptId, 1, Integer::sum);

        if (stormStartTs >= 0) {
            if (now - stormStartTs > maxDurationMs) {
                return Decision.COUNT;
            }
            suppressedInStorm++;
            return Decision.SUPPRESS;
        }
        if (recentTimeoutsPerScript.size() >= minScripts) {
            stormStartTs = now;
            suppressedInStorm = 1;
            log.warn("[{}] Timeout storm detected: {} distinct scripts timed out within {} ms. " +
                            "Script timeouts are not counted towards the blocklist for up to {} ms",
                    name, recentTimeoutsPerScript.size(), windowMs, maxDurationMs);
            return new Decision(true, Set.copyOf(recentTimeoutsPerScript.keySet()));
        }
        return Decision.COUNT;
    }

    public synchronized boolean isInStorm(long now) {
        return stormStartTs >= 0 && now - lastTimeoutTs <= windowMs && now - stormStartTs <= maxDurationMs;
    }

    private void evictOlderThan(long minTs) {
        while (!recentTimeouts.isEmpty() && recentTimeouts.peekFirst().ts() < minTs) {
            UUID scriptId = recentTimeouts.pollFirst().scriptId();
            recentTimeoutsPerScript.computeIfPresent(scriptId, (id, count) -> count > 1 ? count - 1 : null);
        }
    }

    private record ScriptTimeout(long ts, UUID scriptId) {}

    public record Decision(boolean suppress, Set<UUID> stormScripts) {
        static final Decision COUNT = new Decision(false, Set.of());
        static final Decision SUPPRESS = new Decision(true, Set.of());
    }

}
