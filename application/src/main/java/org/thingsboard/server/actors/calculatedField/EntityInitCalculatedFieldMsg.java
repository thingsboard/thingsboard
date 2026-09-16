// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

@Data
public class EntityInitCalculatedFieldMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final CalculatedFieldCtx ctx;
    private final StateAction stateAction;
    private final CalculatedFieldEventType eventType;
    private final TbCallback callback;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_ENTITY_INIT_CF_MSG;
    }

    public enum StateAction {
        INIT,
        REINIT,
        RECREATE,
        REPROCESS,
        REFRESH_CTX
    }
}
