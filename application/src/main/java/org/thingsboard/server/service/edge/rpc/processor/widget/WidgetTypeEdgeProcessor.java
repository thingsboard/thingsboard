// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class WidgetTypeEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                WidgetTypeDetails widgetTypeDetails = edgeCtx.getWidgetTypeService().findWidgetTypeDetailsById(edgeEvent.getTenantId(), widgetTypeId);
                if (widgetTypeDetails != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    WidgetTypeUpdateMsg widgetTypeUpdateMsg = EdgeMsgConstructorUtils.constructWidgetTypeUpdateMsg(msgType, widgetTypeDetails);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addWidgetTypeUpdateMsg(widgetTypeUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                WidgetTypeUpdateMsg widgetTypeUpdateMsg = EdgeMsgConstructorUtils.constructWidgetTypeDeleteMsg(widgetTypeId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addWidgetTypeUpdateMsg(widgetTypeUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.WIDGET_TYPE;
    }

}
