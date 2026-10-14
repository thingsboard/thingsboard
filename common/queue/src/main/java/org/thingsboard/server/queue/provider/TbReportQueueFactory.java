// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import org.thingsboard.server.gen.transport.TransportProtos.ToTbReportNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface TbReportQueueFactory {

    TbQueueConsumer<TbProtoQueueMsg<ToTbReportNotificationMsg>> createTbReportNotificationsConsumer();

}
