package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;

@Slf4j
@Component
public class AdminSettingsUpdateMsgConstructor {

    public AdminSettingsUpdateMsg constructAdminSettingsUpdateMsg(AdminSettings adminSettings) {
        AdminSettingsUpdateMsg.Builder builder = AdminSettingsUpdateMsg.newBuilder()
                .setKey(adminSettings.getKey())
                .setJsonValue(JacksonUtil.toString(adminSettings.getJsonValue()));
        AdminSettingsId adminSettingsId = adminSettings.getId();
        if (adminSettingsId != null) {
            builder.setIdMSB(adminSettingsId.getId().getMostSignificantBits());
            builder.setIdLSB(adminSettingsId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

}
