// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TraceProjection {
    private String traceId;
    private String serviceName;
    private long startTime;
    private Long endTime;
    private Long duration;
    private Integer spanCount;
    private Integer errorCount;
    private long createdTime;
    private String firstSpanName;
    private Long inQueueTime;
    private Long totalSpanTime;

    private String rootQueueId;
    private String messageType;
    private String rootRuleChainId;
    private Integer ruleNodeCount;
    private Boolean hasErrors;
    private Boolean hasQueueTimeouts;

    public TraceProjection(String traceId, String serviceName, Long startTime, Long endTime, Long duration,
                           Integer spanCount, Integer errorCount, Long createdTime, String firstSpanName,
                           Long totalSpanTime, Long inQueueTime,
                           String rootQueueId, String messageType, String rootRuleChainId,
                           Integer ruleNodeCount,
                           Boolean hasErrors, Boolean hasQueueTimeouts) {
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.startTime = startTime != null ? startTime : 0L;
        this.endTime = endTime;
        this.duration = duration;
        this.spanCount = spanCount;
        this.errorCount = errorCount;
        this.createdTime = createdTime != null ? createdTime : 0L;
        this.firstSpanName = firstSpanName;
        this.totalSpanTime = totalSpanTime;
        this.inQueueTime = inQueueTime;
        this.rootQueueId = rootQueueId;
        this.messageType = messageType;
        this.rootRuleChainId = rootRuleChainId;
        this.ruleNodeCount = ruleNodeCount;
        this.hasErrors = hasErrors;
        this.hasQueueTimeouts = hasQueueTimeouts;
    }
}
