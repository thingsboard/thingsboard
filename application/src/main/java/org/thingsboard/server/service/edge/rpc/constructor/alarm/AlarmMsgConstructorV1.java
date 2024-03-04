/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor.alarm;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class AlarmMsgConstructorV1 extends BaseAlarmMsgConstructor {

    @Override
    public AlarmUpdateMsg constructAlarmUpdatedMsg(UpdateMsgType msgType, Alarm alarm, String entityName) {
        return AlarmUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(alarm.getId().getId().getMostSignificantBits())
                .setIdLSB(alarm.getId().getId().getLeastSignificantBits())
                .setName(alarm.getName())
                .setType(alarm.getType())
                .setOriginatorName(entityName)
                .setOriginatorType(alarm.getOriginator().getEntityType().name())
                .setSeverity(alarm.getSeverity().name())
                .setStatus(alarm.getStatus().name())
                .setStartTs(alarm.getStartTs())
                .setEndTs(alarm.getEndTs())
                .setAckTs(alarm.getAckTs())
                .setClearTs(alarm.getClearTs())
                .setDetails(JacksonUtil.toString(alarm.getDetails()))
                .setPropagate(alarm.isPropagate())
                .setPropagateToOwner(alarm.isPropagateToOwner())
                .setPropagateToTenant(alarm.isPropagateToTenant()).build();
    }
}
