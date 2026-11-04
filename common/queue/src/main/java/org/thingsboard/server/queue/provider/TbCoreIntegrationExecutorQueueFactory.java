// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

/**
 * Shared Producers and Consumers used by both TB Core and Integration Executor
 */
public interface TbCoreIntegrationExecutorQueueFactory extends TbUsageStatsClientQueueFactory {

    /**
     * Used to consume high priority messages by TB Integration Executor
     *
     * @return
     */
    TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createToIntegrationExecutorNotificationsMsgConsumer();

    /**
     * Used to consume high priority messages by TB Integration Executor
     *
     * @return
     * @param integrationType
     */
    TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType integrationType);

    TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate();

}
