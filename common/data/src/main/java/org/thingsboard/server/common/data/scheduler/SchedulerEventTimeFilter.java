// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@SuperBuilder
public class SchedulerEventTimeFilter extends SchedulerEventFilter {

    private final long startTime;
    private final long endTime;

    SchedulerEventTimeFilter(CustomerId customerId, String type, long startTime, long endTime, EdgeId edgeId) {
        super(customerId, type, edgeId);
        this.startTime = startTime;
        this.endTime = endTime;
    }

}
