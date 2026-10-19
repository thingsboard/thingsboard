// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;

@Data
@Builder
public class CalculatedFieldAlarmActionMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final Alarm alarm;
    private final ActionType action;
    private final TbCallback callback;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_ALARM_ACTION_MSG;
    }

}
