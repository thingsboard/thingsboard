package org.thingsboard.server.service.state;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsgMetaData;

/**
 * Created by ashvayka on 01.05.18.
 */
@Data
@Builder
class DeviceStateData {

    private final TenantId tenantId;
    private final DeviceId deviceId;

    private TbMsgMetaData metaData;
    private final DeviceState state;

}
