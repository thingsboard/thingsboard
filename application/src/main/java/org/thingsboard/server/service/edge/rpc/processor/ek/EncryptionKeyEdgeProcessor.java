// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.ek;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EncryptionKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class EncryptionKeyEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        if (EdgeEventActionType.ADDED.equals(edgeEvent.getAction())) {
            EncryptionKey encryptionKey = edgeCtx.getEncryptionService().findByTenantId(edgeEvent.getTenantId());
            if (encryptionKey != null) {
                UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                EncryptionKeyUpdateMsg encryptionKeyUpdateMsg =
                        EdgeMsgConstructorUtils.constructEncryptionKeyUpdatedMsg(msgType, encryptionKey);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addEncryptionKeyUpdateMsg(encryptionKeyUpdateMsg)
                        .build();
            } else {
                log.warn("EncryptionKey not found for tenantId: {}", edgeEvent.getTenantId());
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ENCRYPTION_KEY;
    }

}
