// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class TenantEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        TenantId tenantId = TenantId.fromUUID(edgeEvent.getEntityId());
        if (EdgeEventActionType.UPDATED.equals(edgeEvent.getAction())) {
            Tenant tenant = edgeCtx.getTenantService().findTenantById(tenantId);
            if (tenant != null) {
                UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                TenantUpdateMsg tenantUpdateMsg = EdgeMsgConstructorUtils.constructTenantUpdateMsg(msgType, tenant);
                TenantProfile tenantProfile = edgeCtx.getTenantProfileService().findTenantProfileById(tenantId, tenant.getTenantProfileId());
                TenantProfileUpdateMsg tenantProfileUpdateMsg = EdgeMsgConstructorUtils.constructTenantProfileUpdateMsg(msgType, tenantProfile);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addTenantUpdateMsg(tenantUpdateMsg)
                        .addTenantProfileUpdateMsg(tenantProfileUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.TENANT;
    }

}
