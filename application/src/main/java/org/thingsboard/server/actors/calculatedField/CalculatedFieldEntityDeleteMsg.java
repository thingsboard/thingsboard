// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;

@Data
public class CalculatedFieldEntityDeleteMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final TbCallback callback;

    public CalculatedFieldEntityDeleteMsg(TenantId tenantId,
                                          EntityId entityId,
                                          TbCallback callback) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.callback = callback;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_ENTITY_DELETE_MSG;
    }
}
