/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cf;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmRuleState;

import java.util.List;

@Data
@Builder
public class AlarmCalculatedFieldResult implements CalculatedFieldResult {

    private final TbAlarmResult alarmResult;
    private final AlarmRuleState alarmRuleState;

    @Override
    public TbMsg toTbMsg(EntityId entityId, List<CalculatedFieldId> cfIds) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        if (alarmResult.isCreated()) {
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated()) {
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isSeverityUpdated()) {
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
            metaData.putValue(DataConstants.IS_SEVERITY_UPDATED_ALARM, Boolean.TRUE.toString());
        } else {
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        switch (alarmRuleState.getCondition().getType()) {
            case REPEATING -> {
                metaData.putValue(DataConstants.ALARM_CONDITION_REPEATS, String.valueOf(alarmRuleState.getEventCount()));
            }
            case DURATION -> {
                // TODO: schedule instead of duration
                metaData.putValue(DataConstants.ALARM_CONDITION_DURATION, String.valueOf(alarmRuleState.getDuration()));
            }
        }

        return TbMsg.newMsg()
                .type(TbMsgType.ALARM)
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
