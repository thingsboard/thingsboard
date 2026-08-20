/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.monitoring.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.data.MonitoredServiceKey;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
@Slf4j
public class ProbeMetricsRecorder {

    public static final String PROBE_SUCCESS_METRIC = "probe_success";
    public static final String PROBE_DURATION_METRIC = "probe_duration_ms";
    public static final String MONITORING_HEARTBEAT_METRIC = "tb_monitoring_last_run_timestamp_seconds";
    private static final String KIND_PROBE = "probe";
    private static final String KIND_ACCEPTED = "accepted";

    // action names shared with BaseHealthChecker/BaseMonitoringService/WsClientFactory, so a
    // recordActionDuration call and its matching removeActionDuration can't drift via a typo
    public static final String ACTION_REQUEST = "request";
    public static final String ACTION_WS_UPDATE = "ws_update";
    public static final String ACTION_CONNECT = "connect";
    public static final String ACTION_SUBSCRIBE = "subscribe";

    private final MeterRegistry meterRegistry;
    private final boolean enabled;
    private final String domain;
    private final String label;
    private final String loginEndpoint;
    private final String wsEndpoint;

    private final Map<GaugeKey, AtomicReference<Double>> gaugeValues = new ConcurrentHashMap<>();
    // Optional-valued so an unresolvable target's negative result is cached too (a plain
    // computeIfAbsent never stores a null return, so it would otherwise re-resolve every call)
    private final Map<TransportTagKey, Optional<Tags>> transportTagsCache = new ConcurrentHashMap<>();
    private final Map<TransportTagKey, Optional<Tags>> acceptedTagsCache = new ConcurrentHashMap<>();
    // detects two probes resolving to identical labels (e.g. same host:port, different queue) so
    // the collision is logged instead of one gauge silently overwriting the other
    private final Map<Tags, Object> tagsOwners = new ConcurrentHashMap<>();
    private final Set<Tags> warnedCollisions = ConcurrentHashMap.newKeySet();
    // dedupes the "can't resolve this transport's endpoint" warning so a permanently-misconfigured
    // target logs it once instead of every probe cycle forever (computeIfAbsent doesn't cache nulls)
    private final Set<TransportTagKey> warnedUnresolvable = ConcurrentHashMap.newKeySet();
    // action gauges recorded per probe, so removeProbe can clean them up without knowing them upfront
    private final Map<Tags, Set<String>> actionsByBaseTags = new ConcurrentHashMap<>();
    // tags that already got fresh data this cycle - so a colliding sibling's removeProbe/removeAcceptedProbe
    // (e.g. crashing right after this one succeeded) can't wipe what was just correctly recorded.
    // Safe only because every BaseMonitoringService's runChecks() cycle runs on one shared
    // single-threaded executor (see ThingsboardMonitoringApplication) - a second thread/pool
    // touching this set concurrently would break the protection with no compile-time signal.
    private final Set<Tags> freshThisCycle = ConcurrentHashMap.newKeySet();

    public ProbeMetricsRecorder(MeterRegistry meterRegistry,
                                 @Value("${monitoring.metrics.otlp.enabled:false}") boolean otlpEnabled,
                                 @Value("${monitoring.metrics.prometheus.enabled:false}") boolean prometheusEnabled,
                                 @Value("${monitoring.domain}") String domain,
                                 @Value("${monitoring.rest.base_url}") String restBaseUrl,
                                 @Value("${monitoring.ws.base_url}") String wsBaseUrl,
                                 @Value("${monitoring.label:}") String label) {
        this.meterRegistry = meterRegistry;
        this.enabled = otlpEnabled || prometheusEnabled;
        this.domain = domain;
        this.label = label;
        this.loginEndpoint = this.enabled ? ProbeLabelResolver.resolveLoginEndpoint(restBaseUrl) : null;
        this.wsEndpoint = this.enabled ? ProbeLabelResolver.resolveWsEndpoint(wsBaseUrl) : null;
    }

    // resets which Tags have fresh data this cycle - called once at the start of each runChecks()
    public void startCycle() {
        freshThisCycle.clear();
    }

    public void recordProbe(Object serviceKey, boolean success) {
        withTags(serviceKey, "record", tags -> {
            warnIfLabelCollision(serviceKey, tags);
            setGauge(PROBE_SUCCESS_METRIC, tags, success ? 1d : 0d);
            freshThisCycle.add(tags);
        });
    }

    public void recordActionDuration(Object serviceKey, String action, long durationMs) {
        withTags(serviceKey, "record action duration", tags -> {
            actionsByBaseTags.computeIfAbsent(tags, k -> ConcurrentHashMap.newKeySet()).add(action);
            setGauge(PROBE_DURATION_METRIC, tags.and("action", action), (double) durationMs);
        });
    }

    // removes just this one action's duration gauge (the specific probe that didn't respond),
    // without touching probe_success or any other action recorded for this target. Skips removal
    // if these tags already have fresh data this cycle (from a colliding sibling target that
    // recorded successfully first) - same protection removeStaleProbe already gives probe_success.
    public void removeActionDuration(Object serviceKey, String action) {
        withTags(serviceKey, "remove action duration", tags -> {
            if (freshThisCycle.contains(tags)) {
                log.debug("Skipping removal of action duration for [{}] action {} - tags {} already have fresh data this cycle from a colliding target", serviceKey, action, tags);
                return;
            }
            removeGauge(PROBE_DURATION_METRIC, tags.and("action", action));
            Set<String> actions = actionsByBaseTags.get(tags);
            if (actions != null) {
                actions.remove(action);
            }
        });
    }

    // true once the transport itself acknowledges a message, independent of the login/WS session
    public void recordAcceptedProbe(Object serviceKey, boolean success) {
        if (!enabled || !(serviceKey instanceof TransportInfo transportInfo)) {
            return;
        }
        try {
            Tags tags = acceptedTags(transportInfo);
            if (tags == null) {
                return;
            }
            setGauge(PROBE_SUCCESS_METRIC, tags, success ? 1d : 0d);
            freshThisCycle.add(tags);
        } catch (Exception e) {
            log.warn("Failed to record accepted probe metric for [{}]", serviceKey, e);
        }
    }

    // permanent removal (e.g. decommissioning a domain-IP associate) - always removes the gauge,
    // even if its tags got fresh data this cycle, since the target is gone for good either way
    public void removeProbe(Object serviceKey) {
        removeProbe(serviceKey, false);
    }

    // for a probe no longer checked THIS cycle only - skips removal if its tags already have fresh
    // data this cycle (from a colliding sibling target sharing the same resolved tags), so that a
    // crashing sibling's stale-clearing can't wipe another target's just-recorded gauge
    public void removeStaleProbe(Object serviceKey) {
        removeProbe(serviceKey, true);
    }

    private void removeProbe(Object serviceKey, boolean skipIfFresh) {
        withTags(serviceKey, "remove", tags -> {
            if (skipIfFresh && freshThisCycle.contains(tags)) {
                log.debug("Skipping removal of probe metric for [{}] - tags {} already have fresh data this cycle from a colliding target", serviceKey, tags);
                return;
            }
            removeGauge(PROBE_SUCCESS_METRIC, tags);
            Set<String> actions = actionsByBaseTags.remove(tags);
            if (actions != null) {
                actions.forEach(action -> removeGauge(PROBE_DURATION_METRIC, tags.and("action", action)));
            }
            tagsOwners.remove(tags);
            warnedCollisions.remove(tags);
            freshThisCycle.remove(tags); // a permanent removal must not leave a stale fresh-flag behind
        });
        // only on permanent removal - a stale (per-cycle) removal runs every unhealthy cycle (e.g.
        // for the whole duration of a login/WS outage), so evicting here would defeat the cache and,
        // for warnedUnresolvable, make an unresolvable target's warning re-fire every such cycle
        // instead of once - exactly what that dedup set exists to prevent
        if (!skipIfFresh && serviceKey instanceof TransportInfo transportInfo) {
            // otherwise these caches leak the same way the gauges just did
            TransportTagKey key = TransportTagKey.of(transportInfo);
            transportTagsCache.remove(key);
            warnedUnresolvable.remove(key);
        }
    }

    // permanent removal - see removeProbe(Object) for why this never skips
    public void removeAcceptedProbe(Object serviceKey) {
        removeAcceptedProbe(serviceKey, false);
    }

    // for an accepted-fallback probe no longer checked THIS cycle only - see removeStaleProbe(Object)
    public void removeStaleAcceptedProbe(Object serviceKey) {
        removeAcceptedProbe(serviceKey, true);
    }

    // only evicts acceptedTagsCache on permanent removal (see below) - the stale path runs every
    // healthy cycle, which would defeat the cache
    private void removeAcceptedProbe(Object serviceKey, boolean skipIfFresh) {
        if (!enabled || !(serviceKey instanceof TransportInfo transportInfo)) {
            return;
        }
        try {
            Tags tags = acceptedTags(transportInfo);
            if (tags == null) {
                return;
            }
            if (skipIfFresh && freshThisCycle.contains(tags)) {
                log.debug("Skipping removal of accepted probe metric for [{}] - tags {} already have fresh data this cycle from a colliding target", transportInfo, tags);
                return;
            }
            removeGauge(PROBE_SUCCESS_METRIC, tags);
            freshThisCycle.remove(tags);
            if (!skipIfFresh) {
                // permanent removal (decommissioning) - safe to evict here, unlike the stale path which
                // runs every healthy cycle and would otherwise defeat the cache
                acceptedTagsCache.remove(TransportTagKey.of(transportInfo));
            }
        } catch (Exception e) {
            log.warn("Failed to remove accepted probe metric for [{}]", transportInfo, e);
        }
    }

    // distinguishes "the prober is dead" (this goes stale) from "a target is down" (probe_success does)
    public void recordHeartbeat() {
        if (!enabled) {
            return;
        }
        try {
            setGauge(MONITORING_HEARTBEAT_METRIC, Tags.of("domain", domain, "label", label), (double) (System.currentTimeMillis() / 1000));
        } catch (Exception e) {
            log.warn("Failed to record monitoring heartbeat metric", e);
        }
    }

    private void warnIfLabelCollision(Object serviceKey, Tags tags) {
        Object previousOwner = tagsOwners.put(tags, serviceKey);
        if (previousOwner != null && !previousOwner.equals(serviceKey) && warnedCollisions.add(tags)) {
            log.warn("Probe metrics collision: [{}] and [{}] both resolve to labels {} - " +
                    "one will silently overwrite the other's gauge every cycle", previousOwner, serviceKey, tags);
        }
    }

    private void withTags(Object serviceKey, String verb, Consumer<Tags> body) {
        if (!enabled) {
            return;
        }
        try {
            Tags tags = resolveTags(serviceKey);
            if (tags == null) {
                return; // GENERAL, EDQS, an unresolvable transport endpoint, or anything outside the documented label taxonomy
            }
            body.accept(tags);
        } catch (Exception e) {
            log.warn("Failed to {} probe metric for [{}]", verb, serviceKey, e);
        }
    }

    private Tags resolveTags(Object serviceKey) {
        if (serviceKey instanceof TransportInfo transportInfo) {
            return transportTags(transportInfo);
        } else if (loginEndpoint != null && MonitoredServiceKey.LOGIN.equals(serviceKey)) {
            return baseTags("login", loginEndpoint, KIND_PROBE);
        } else if (wsEndpoint != null && MonitoredServiceKey.WS.equals(serviceKey)) {
            return baseTags("ws", wsEndpoint, KIND_PROBE);
        }
        return null;
    }

    private Tags transportTags(TransportInfo info) {
        return cachedTags(transportTagsCache, info, KIND_PROBE);
    }

    private Tags acceptedTags(TransportInfo info) {
        return cachedTags(acceptedTagsCache, info, KIND_ACCEPTED);
    }

    // keyed on type+baseUrl, not the whole TransportInfo (its equals/hashCode reach mutable state)
    private Tags cachedTags(Map<TransportTagKey, Optional<Tags>> cache, TransportInfo info, String kind) {
        return cache.computeIfAbsent(TransportTagKey.of(info), key -> {
            ProbeLabelResolver.ProbeLabels labels = ProbeLabelResolver.resolveTransportLabels(key.type(), key.baseUrl());
            if (labels == null) {
                if (warnedUnresolvable.add(key)) {
                    log.warn("Failed to resolve host:port from transport base URL \"{}\" (missing scheme?) - its probe metrics will not be recorded", key.baseUrl());
                }
                return Optional.empty();
            }
            return Optional.of(baseTags(labels.check(), labels.endpoint(), kind));
        }).orElse(null);
    }

    private Tags baseTags(String check, String endpoint, String kind) {
        return Tags.of("domain", domain, "check", check, "endpoint", endpoint, "kind", kind, "label", label);
    }

    private void setGauge(String metricName, Tags tags, double value) {
        gaugeValues.computeIfAbsent(new GaugeKey(metricName, tags), k -> {
            AtomicReference<Double> ref = new AtomicReference<>(value);
            Gauge.builder(metricName, ref, r -> r.get())
                    .tags(tags)
                    .register(meterRegistry);
            return ref;
        }).set(value);
    }

    private void removeGauge(String metricName, Tags tags) {
        // skip find()'s full-registry scan when gaugeValues shows there's nothing to remove
        if (gaugeValues.remove(new GaugeKey(metricName, tags)) != null) {
            meterRegistry.find(metricName).tags(tags).meters().forEach(meterRegistry::remove);
        }
    }

    private record GaugeKey(String metricName, Tags tags) {
    }

    private record TransportTagKey(TransportType type, String baseUrl) {
        static TransportTagKey of(TransportInfo info) {
            return new TransportTagKey(info.getType(), info.getTarget().getBaseUrl());
        }
    }

}
