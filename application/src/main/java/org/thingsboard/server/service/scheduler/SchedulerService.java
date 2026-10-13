// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.scheduler;

import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.SchedulerServiceMsgProto;

public interface SchedulerService {

    void onSchedulerEventAdded(SchedulerEventInfo event);

    void onSchedulerEventUpdated(SchedulerEventInfo event);

    void onSchedulerEventDeleted(SchedulerEventInfo event);

    void onQueueMsg(SchedulerServiceMsgProto msg, TbCallback callback);

}
