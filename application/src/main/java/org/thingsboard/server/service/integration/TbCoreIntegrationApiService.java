// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Collection;

public interface TbCoreIntegrationApiService extends TbQueueHandler<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> {

    void handle(Collection<TbProtoQueueMsg<ToCoreIntegrationMsg>> msg, TbCallback callback);

}
