// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.scheduler;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import lombok.Data;
import org.thingsboard.server.common.data.scheduler.SchedulerEventDescriptor;

@Data
class SchedulerEventMetaData {

    private final SchedulerEventDescriptor descriptor;
    private volatile ListenableScheduledFuture<?> nextTaskFuture;

}
