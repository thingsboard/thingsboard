// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.secret;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class SecretEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        SecretId secretId = new SecretId(edgeEvent.getEntityId());

        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                Secret secret = edgeCtx.getSecretService().findSecretById(edgeEvent.getTenantId(), secretId);
                if (secret != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    SecretUpdateMsg secretUpdateMsg = EdgeMsgConstructorUtils.constructSecretUpdatedMsg(msgType, secret);

                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addSecretUpdateMsg(secretUpdateMsg)
                            .build();
                }
                break;

            case DELETED:
                SecretUpdateMsg deleteMsg = EdgeMsgConstructorUtils.constructSecretDeleteMsg(secretId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addSecretUpdateMsg(deleteMsg)
                        .build();
        }

        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.SECRET;
    }

}
