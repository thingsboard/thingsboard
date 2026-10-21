package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceGroup {
    // Groups aggregate per UTC day; bucketTime is the day start.
    public static final long DAY_BUCKET_MS = 86_400_000L;

    private long bucketTime;
    private UUID tenantId;
    private String pathId;

    private UUID rootQueueId;
    private String rootMessageType;
    private UUID rootRuleChainId;

    private List<VisitedRuleNode> visitedRuleNodes;

    private List<String> relatedTraceIds;
    private long lastRelatedTraceAddedTime;

    private long lastObserved;

    private long processedTraces;
    private long processedTracesWithTimeout;
    private long processedTracesWithError;

    private long storedTraces;
    private long totalTime;
    private long maxTime;
    private long totalInQueueTime;
    private long totalRuleNodeProcessingTime;

    private Map<String, RuleNodePathMetric> ruleNodePathMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitedRuleNode {
        private UUID queueId;
        private UUID ruleChainId;
        private UUID ruleNodeId;

        private List<UUID> fromNodeConnections;

        private List<UUID> toNodeConnections;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleNodePathMetric {
        private long totalTime;
        private long totalCount;
        private long errors;
        private UUID ruleChainId;
    }

    public static long floorToDay(long timestampMs) {
        return (timestampMs / DAY_BUCKET_MS) * DAY_BUCKET_MS;
    }
}
