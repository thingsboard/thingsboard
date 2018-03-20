package org.thingsboard.server.common.msg.system;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 15.03.18.
 */
@Data
public final class ServiceToRuleEngineMsg implements TbActorMsg {

    private final TenantId tenantId;
    private final TbMsg tbMsg;

    @Override
    public MsgType getMsgType() {
        return MsgType.SERVICE_TO_RULE_ENGINE_MSG;
    }
}
