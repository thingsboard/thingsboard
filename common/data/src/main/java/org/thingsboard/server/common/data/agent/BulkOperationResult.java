// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Internal accumulator used while a bulk operation is filtered and executed (single-threaded).
 * It is not serialized in any REST response — {@link BulkOperationPreview} and
 * {@link AgentBulkAction} expose the totals/skip-counts to clients.
 */
@Data
@NoArgsConstructor
public class BulkOperationResult {

    private int total;
    private int submitted;
    private int eligible;
    private Collection<SkippedApp> skipped = new ArrayList<>();
    private Map<SkipReason, Integer> skipCounts = new EnumMap<>(SkipReason.class);

    /**
     * Per-reason cap on how many {@link SkippedApp} records are kept. Counts in {@link #skipCounts} stay exact
     * regardless. The execute path leaves this unbounded because it turns every skip into a synthetic error
     * event; the preview path caps it at the sample size it returns, so a fleet-wide scan stays O(reasons).
     */
    private int maxRetainedSkippedPerReason = Integer.MAX_VALUE;

    public void incrementTotal() {
        total++;
    }

    public void incrementSubmitted() {
        submitted++;
    }

    public void incrementEligible() {
        eligible++;
    }

    public void addSkipped(SkippedApp app) {
        int count = skipCounts.merge(app.getReason(), 1, Integer::sum);
        if (count <= maxRetainedSkippedPerReason) {
            skipped.add(app);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema
    public static class SkippedApp {
        @Schema(description = "Agent Id owning the application")
        private AgentId agentId;
        @Schema(description = "Agent name")
        private String agentName;
        @Schema(description = "Application Id")
        private AgentApplicationId applicationId;
        @Schema(description = "Application name")
        private String applicationName;
        @Schema(description = "Reason for skipping")
        private SkipReason reason;
        @Schema(description = "Optional message in case of a failure")
        private String msg;

        public SkippedApp(AgentId agentId, String agentName, AgentApplicationId applicationId, String applicationName, SkipReason reason) {
            this(agentId, agentName, applicationId, applicationName, reason, null);
        }
    }

    public enum SkipReason {
        VERSION_MISMATCH,
        ACTIVE_EVENT,
        RATE_LIMIT_EXCEEDED,
        ERROR
    }
}
