// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;

public interface TbIntegrationDownlinkService {

    void onRuleEngineDownlinkMsg(TenantId tenantId, IntegrationId integrationId, IntegrationDownlinkMsgProto downlinkMsgProto, TbCallback tbQueueCallback);

    void onDownlinkToRemoteIntegrationMsg(IntegrationDownlinkMsgProto integrationDownlinkMsg, TbCallback callback);
}
