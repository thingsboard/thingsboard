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
package org.thingsboard.monitoring.notification.incident;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.monitoring.data.notification.AffectedService;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safety: all public entry points and scheduled callbacks are {@code synchronized} on the
 * manager instance. Transport I/O is performed while the monitor is held. This is safe under the
 * assumptions that (a) the transport enforces a short per-call timeout, and (b) notification
 * producers are single-threaded; see the Slack client (default 5s) for the Slack-based transport.
 */
@Slf4j
public class IncidentManager {

    private final IncidentTransport transport;
    private final long resolutionTimeoutSeconds;
    private final String messagePrefix;
    private final boolean tagChannel;
    private final ScheduledExecutorService scheduler;

    private String activeIncidentThreadId;
    private ScheduledFuture<?> resolutionTask;
    private ScheduledFuture<?> durationUpdateTask;
    private Instant incidentStartTime;
    private Instant lastAlertTime;
    private final Map<String, Integer> failingServices = new LinkedHashMap<>();
    private final Map<String, Integer> recoveredServices = new LinkedHashMap<>();
    private final Set<String> highLatencyServices = new LinkedHashSet<>();

    public IncidentManager(IncidentTransport transport, long resolutionTimeoutSeconds,
                           String messagePrefix, boolean tagChannel) {
        this.transport = transport;
        this.resolutionTimeoutSeconds = resolutionTimeoutSeconds;
        this.messagePrefix = messagePrefix;
        this.tagChannel = tagChannel;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "incident-manager");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void sendAlert(String message, List<AffectedService> affectedServices) {
        try {
            if (activeIncidentThreadId == null) {
                if (affectedServices.stream().allMatch(s -> s.status() == AffectedService.Status.RECOVERED)) {
                    return;
                }
                incidentStartTime = Instant.now();
                failingServices.clear();
                recoveredServices.clear();
                highLatencyServices.clear();
                applyAffectedServices(affectedServices);
                activeIncidentThreadId = transport.postIncident(buildOngoingMessageText());
                startDurationUpdater();
                log.info("New incident created, thread id: {}", activeIncidentThreadId);
            } else if (applyAffectedServices(affectedServices)) {
                safeUpdateHeader();
            }

            try {
                transport.postThreadReply(activeIncidentThreadId, message);
                log.debug("Alert added to incident thread {}", activeIncidentThreadId);
            } catch (Exception e) {
                log.error("Failed to post alert to incident thread {}", activeIncidentThreadId, e);
            }
        } finally {
            if (activeIncidentThreadId != null) {
                lastAlertTime = Instant.now();
                // High latency is a warning only — it has no explicit recovery signal
                // (HighLatencyNotification fires only when something is above threshold),
                // so resolution hinges on failing services alone.
                if (failingServices.isEmpty()) {
                    resetResolutionTimer();
                } else {
                    cancelResolutionTimer();
                }
            }
        }
    }

    private boolean applyAffectedServices(List<AffectedService> affectedServices) {
        boolean changed = false;
        Set<String> latencySnapshot = null;
        for (AffectedService service : affectedServices) {
            String name = service.name();
            switch (service.status()) {
                case FAILING -> {
                    Integer prev = failingServices.put(name, service.failureCount());
                    if (prev == null || prev.intValue() != service.failureCount()) {
                        changed = true;
                    }
                    if (recoveredServices.remove(name) != null) {
                        changed = true;
                    }
                }
                case RECOVERED -> {
                    Integer lastFailureCount = failingServices.remove(name);
                    if (lastFailureCount != null) {
                        recoveredServices.put(name, lastFailureCount);
                        changed = true;
                    }
                }
                case HIGH_LATENCY -> {
                    if (latencySnapshot == null) {
                        latencySnapshot = new LinkedHashSet<>();
                    }
                    latencySnapshot.add(name);
                }
            }
        }
        // HighLatencyNotification carries the full current set of high latencies, so treat it as a
        // snapshot: replace highLatencyServices entirely. Without this, a brief spike would stay
        // yellow in the header until the incident resolves.
        if (latencySnapshot != null && !latencySnapshot.equals(highLatencyServices)) {
            highLatencyServices.clear();
            highLatencyServices.addAll(latencySnapshot);
            changed = true;
        }
        return changed;
    }

    private String buildOngoingMessageText() {
        StringBuilder sb = new StringBuilder();
        if (tagChannel) {
            sb.append("<!channel> ");
        }
        if (messagePrefix != null && !messagePrefix.isEmpty()) {
            sb.append("*").append(messagePrefix).append("*");
        }
        sb.append(" :rotating_light:");
        Duration elapsed = Duration.between(incidentStartTime, Instant.now());
        if (elapsed.toMinutes() >= 1) {
            sb.append(" (").append(formatDuration(elapsed)).append(")");
        }
        if (hasAffected()) {
            sb.append(" | ").append(formatAffectedServices());
        }
        return sb.toString();
    }

    private boolean hasAffected() {
        return !failingServices.isEmpty() || !recoveredServices.isEmpty() || !highLatencyServices.isEmpty();
    }

    private void safeUpdateHeader() {
        try {
            transport.updateIncident(activeIncidentThreadId, buildOngoingMessageText());
        } catch (Exception e) {
            log.error("Failed to update incident message", e);
        }
    }

    private void resetResolutionTimer() {
        cancelResolutionTimer();
        resolutionTask = scheduler.schedule(this::resolveIncident, resolutionTimeoutSeconds, TimeUnit.SECONDS);
    }

    private void cancelResolutionTimer() {
        if (resolutionTask != null) {
            resolutionTask.cancel(false);
            resolutionTask = null;
        }
    }

    private void startDurationUpdater() {
        if (durationUpdateTask != null) {
            durationUpdateTask.cancel(false);
        }
        durationUpdateTask = scheduler.scheduleAtFixedRate(this::updateDuration, 60, 60, TimeUnit.SECONDS);
    }

    private synchronized void updateDuration() {
        if (activeIncidentThreadId == null) {
            return;
        }
        safeUpdateHeader();
    }

    private void stopDurationUpdater() {
        if (durationUpdateTask != null) {
            durationUpdateTask.cancel(false);
            durationUpdateTask = null;
        }
    }

    static String formatDuration(Duration duration) {
        long totalMinutes = duration.toMinutes();
        if (totalMinutes < 60) {
            return totalMinutes + "m";
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return minutes > 0 ? hours + "h" + minutes + "m" : hours + "h";
    }

    synchronized void resolveIncident() {
        if (activeIncidentThreadId == null) {
            return;
        }
        String threadId = activeIncidentThreadId;
        stopDurationUpdater();
        String resolutionMessage = buildResolutionMessage();
        activeIncidentThreadId = null;
        resolutionTask = null;
        failingServices.clear();
        recoveredServices.clear();
        highLatencyServices.clear();
        try {
            transport.updateIncident(threadId, resolutionMessage);
            log.info("Incident resolved (thread was {})", threadId);
        } catch (Exception e) {
            log.error("Failed to send incident resolution message", e);
        }
    }

    private String buildResolutionMessage() {
        Duration totalDuration = lastAlertTime != null
                ? Duration.between(incidentStartTime, lastAlertTime)
                : Duration.between(incidentStartTime, Instant.now());
        StringBuilder sb = new StringBuilder();
        if (messagePrefix != null && !messagePrefix.isEmpty()) {
            sb.append("*").append(messagePrefix).append("*");
        }
        sb.append(" :white_check_mark:");
        sb.append(" (").append(formatDuration(totalDuration)).append(")");
        if (hasAffected()) {
            sb.append(" | ").append(formatAffectedServices()).append("\n");
        }
        return sb.toString();
    }

    private String formatAffectedServices() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : failingServices.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(":red_circle: ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
            first = false;
        }
        for (String name : highLatencyServices) {
            if (!first) sb.append(", ");
            sb.append(":large_yellow_circle: ").append(name);
            first = false;
        }
        for (Map.Entry<String, Integer> entry : recoveredServices.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(":large_green_circle: ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
            first = false;
        }
        return sb.toString();
    }

    public void shutdown() {
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Incident scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
