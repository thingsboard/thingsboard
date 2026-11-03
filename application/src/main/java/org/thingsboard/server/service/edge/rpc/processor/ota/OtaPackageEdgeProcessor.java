// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.ota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class OtaPackageEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        OtaPackageId otaPackageId = new OtaPackageId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                OtaPackage otaPackage = edgeCtx.getOtaPackageService().findOtaPackageById(edgeEvent.getTenantId(), otaPackageId);
                if (otaPackage != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OtaPackageUpdateMsg otaPackageUpdateMsg = EdgeMsgConstructorUtils.constructOtaPackageUpdatedMsg(msgType, otaPackage);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                OtaPackageUpdateMsg otaPackageUpdateMsg = EdgeMsgConstructorUtils.constructOtaPackageDeleteMsg(otaPackageId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.OTA_PACKAGE;
    }

}
