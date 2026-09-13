// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Data;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

@Data
public class CalculatedFieldRelationActionMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final EntityId relatedEntityId;
    private final ActionType action;
    private final CalculatedFieldCtx calculatedField;
    private final TbCallback callback;

    public CalculatedFieldRelationActionMsg(TenantId tenantId,
                                            EntityId relatedEntityId, ActionType action,
                                            CalculatedFieldCtx calculatedField,
                                            TbCallback callback) {
        this.tenantId = tenantId;
        this.relatedEntityId = relatedEntityId;
        this.action = action;
        this.calculatedField = calculatedField;
        this.callback = callback;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_RELATION_ACTION_MSG;
    }

}
