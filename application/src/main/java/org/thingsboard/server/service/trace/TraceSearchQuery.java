// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.sql.trace.SortOrder;
import org.thingsboard.server.dao.sql.trace.TraceSortField;

@Data
@Builder
public class TraceSearchQuery {
    private String serviceName;
    private long startTime;
    private long endTime;
    private String spanName;
    private String queueName;
    private String originatorId;
    private String messageType;
    private String messageId;
    private String messageData;
    private String messageMetadata;
    private String traceId;
    private boolean errorsOnly;
    private boolean hasTimeOutsByQueue;
    private String ruleChainId;
    private String ruleNodeId;
    private String pathId;
    private int page;
    private int pageSize;
    private TraceSortField sortField;
    private SortOrder sortOrder;

    public boolean hasTraceId() {
        return !StringUtils.isEmpty(traceId);
    }
}
