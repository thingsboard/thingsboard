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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class WidgetTypeMsgConstructor {

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
                if (widgetType.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
                   builder.setIsSystem(true);
                }
        return builder.build();
    }

    public WidgetTypeUpdateMsg constructWidgetTypeDeleteMsg(WidgetTypeId widgetTypeId) {
        return WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetTypeId.getId().getMostSignificantBits())
                .setIdLSB(widgetTypeId.getId().getLeastSignificantBits())
                .build();
    }
}
