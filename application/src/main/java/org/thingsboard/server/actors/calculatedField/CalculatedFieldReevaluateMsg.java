// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

@Data
public class CalculatedFieldReevaluateMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final CalculatedFieldCtx ctx;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_REEVALUATE_MSG;
    }

}
