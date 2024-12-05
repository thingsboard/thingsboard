/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;

@Component
@Slf4j
@TbCoreComponent
public class WidgetBundleEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private WidgetMsgConstructorFactory widgetMsgConstructorFactory;

    public DownlinkMsg convertWidgetsBundleEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(edgeEvent.getEntityId());
        var msgConstructor = ((WidgetMsgConstructor) widgetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion));
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                WidgetsBundle widgetsBundle = edgeCtx.getWidgetsBundleService().findWidgetsBundleById(edgeEvent.getTenantId(), widgetsBundleId);
                if (widgetsBundle != null) {
                    List<String> widgets = edgeCtx.getWidgetTypeService().findWidgetFqnsByWidgetsBundleId(edgeEvent.getTenantId(), widgetsBundleId);
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    WidgetsBundleUpdateMsg widgetsBundleUpdateMsg = msgConstructor.constructWidgetsBundleUpdateMsg(msgType, widgetsBundle, widgets);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addWidgetsBundleUpdateMsg(widgetsBundleUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                WidgetsBundleUpdateMsg widgetsBundleUpdateMsg = msgConstructor.constructWidgetsBundleDeleteMsg(widgetsBundleId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addWidgetsBundleUpdateMsg(widgetsBundleUpdateMsg)
                        .build();
            }
        }
        return null;
    }

}
