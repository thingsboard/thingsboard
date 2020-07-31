/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;

@Component
@Slf4j
public class WidgetTypeUpdateMsgConstructor {

    public WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, WidgetType widgetType) {
        WidgetTypeUpdateMsg.Builder builder = WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(widgetType.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetType.getId().getId().getLeastSignificantBits());
                if (widgetType.getBundleAlias() != null) {
                    builder.setBundleAlias(widgetType.getBundleAlias());
                }
                if (widgetType.getAlias() != null) {
                    builder.setAlias(widgetType.getAlias());
                }
                if (widgetType.getName() != null) {
                    builder.setName(widgetType.getName());
                }
                if (widgetType.getDescriptor() != null) {
                    builder.setDescriptorJson(JacksonUtil.toString(widgetType.getDescriptor()));
                }
                return builder.build();
    }

    public WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(WidgetTypeId widgetTypeId) {
        return WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetTypeId.getId().getMostSignificantBits())
                .setIdLSB(widgetTypeId.getId().getLeastSignificantBits())
                .build();
    }
}
