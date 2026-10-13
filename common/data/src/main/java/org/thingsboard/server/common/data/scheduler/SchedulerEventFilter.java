// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;

@Data
@RequiredArgsConstructor
@SuperBuilder
public class SchedulerEventFilter {

    private final CustomerId customerId;
    private final String type;

    private final EdgeId edgeId;

}
