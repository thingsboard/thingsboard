// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record TraceSummaryInfo(String traceId, UUID tenantId, List<String> serviceNames, long startTime,
                               long endTime, long duration, int spanCount, int errorCount, long createdTime,
                               String firstSpanName, long totalSpanTime, long inQueueTime, UUID rootQueueId,
                               String rootMessageType, String rootRuleChainId, List<String> originatorIds,
                               List<String> queueNames, List<String> ruleChainIds, List<String> ruleNodeIds,
                               List<String> messageTypes, List<String> messageIds, boolean hasErrors,
                               boolean hasQueueTimeouts) {
    public TraceSummaryInfo {
        serviceNames = safeList(serviceNames);
        rootMessageType = safeString(rootMessageType);
        rootRuleChainId = safeString(rootRuleChainId);
        originatorIds = safeList(originatorIds);
        queueNames = safeList(queueNames);
        ruleChainIds = safeList(ruleChainIds);
        ruleNodeIds = safeList(ruleNodeIds);
        messageTypes = safeList(messageTypes);
        messageIds = safeList(messageIds);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : List.copyOf(values);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
