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
package org.thingsboard.server.service.edge.rpc.constructor.widget;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Component
@TbCoreComponent
public class WidgetMsgConstructorV2 extends BaseWidgetMsgConstructor {

    @Override
    public WidgetsBundleUpdateMsg constructWidgetsBundleUpdateMsg(UpdateMsgType msgType, WidgetsBundle widgetsBundle, List<String> widgets) {
        return WidgetsBundleUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(widgetsBundle))
                .setWidgets(JacksonUtil.toString(widgets))
                .setIdMSB(widgetsBundle.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetsBundle.getId().getId().getLeastSignificantBits()).build();
    }

    @Override
    public WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, WidgetTypeDetails widgetTypeDetails, EdgeVersion edgeVersion) {
        return WidgetTypeUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(widgetTypeDetails))
                .setIdMSB(widgetTypeDetails.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetTypeDetails.getId().getId().getLeastSignificantBits()).build();
    }
}
