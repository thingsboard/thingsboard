// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.settings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class AdminSettingsEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AdminSettings adminSettings = null;
        if (edgeEvent.getEntityId() != null) {
            AdminSettingsId adminSettingsId = new AdminSettingsId(edgeEvent.getEntityId());
            adminSettings = edgeCtx.getAdminSettingsService().findAdminSettingsById(edgeEvent.getTenantId(), adminSettingsId);
        } else if (edgeEvent.getBody() != null && !edgeEvent.getBody().isEmpty()) {
            // legacy
            adminSettings = JacksonUtil.convertValue(edgeEvent.getBody(), AdminSettings.class);
        }
        if (adminSettings == null) {
            return null;
        }
        AdminSettingsUpdateMsg msg = AdminSettingsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(adminSettings)).build();
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(msg)
                .build();
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ADMIN_SETTINGS;
    }

}
