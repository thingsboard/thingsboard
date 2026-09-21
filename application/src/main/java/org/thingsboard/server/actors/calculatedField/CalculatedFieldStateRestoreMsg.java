// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;

@Data
public class CalculatedFieldStateRestoreMsg implements ToCalculatedFieldSystemMsg {

    private final CalculatedFieldEntityCtxId id;
    private final CalculatedFieldState state;
    private final TbCallback callback;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_STATE_RESTORE_MSG;
    }

    @Override
    public TenantId getTenantId() {
        return id.tenantId();
    }
}
