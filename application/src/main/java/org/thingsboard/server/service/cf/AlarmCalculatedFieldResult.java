// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
public class AlarmCalculatedFieldResult implements CalculatedFieldResult {

    private final TbAlarmResult alarmResult;

    @Override
    public TbMsg toTbMsg(EntityId entityId, String cfName, List<CalculatedFieldId> cfIds) {
        TbMsgType msgType;
        TbMsgMetaData metaData = new TbMsgMetaData();
        if (alarmResult.isCreated()) {
            msgType = TbMsgType.ALARM_CREATED;
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated()) {
            msgType = TbMsgType.ALARM_UPDATED;
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isSeverityUpdated()) {
            msgType = TbMsgType.ALARM_SEVERITY_UPDATED;
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
            metaData.putValue(DataConstants.IS_SEVERITY_UPDATED_ALARM, Boolean.TRUE.toString());
        } else {
            msgType = TbMsgType.ALARM_CLEAR;
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        if (alarmResult.getConditionRepeats() != null) {
            metaData.putValue(DataConstants.ALARM_CONDITION_REPEATS, String.valueOf(alarmResult.getConditionRepeats()));
        }
        if (alarmResult.getConditionDuration() != null) {
            metaData.putValue(DataConstants.ALARM_CONDITION_DURATION, String.valueOf(alarmResult.getConditionDuration()));
        }

        return TbMsg.newMsg()
                .type(msgType)
                .originator(entityId)
                .data(JacksonUtil.toString(alarmResult.getAlarm()))
                .metaData(metaData)
                .build();
    }

    @Override
    public String stringValue() {
        return alarmResult != null ? JacksonUtil.toString(alarmResult) : null;
    }

    @Override
    public boolean isEmpty() {
        return alarmResult == null;
    }

}
