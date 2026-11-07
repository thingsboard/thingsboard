// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.dao.sql.trace.SortOrder;
import org.thingsboard.server.dao.sql.trace.TracePathSortField;

@Data
@Builder
public class TracePathListQuery {
    private long startTime;
    private long endTime;

    private String rootQueueId;
    private String rootMessageType;
    private String rootRuleChainId;
    private String ruleNodeId;
    private boolean errorsOnly;

    private int page;
    private int pageSize;
    private TracePathSortField sortField;
    private SortOrder sortOrder;
}
