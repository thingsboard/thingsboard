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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class AlarmEdgeProcessorV1 extends AlarmEdgeProcessor {

    @Override
    protected Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg) {
        Alarm alarm = new Alarm();
        alarm.setId(alarmId);
        alarm.setTenantId(tenantId);
        alarm.setOriginator(originatorId);
        alarm.setType(alarmUpdateMsg.getName());
        alarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
        alarm.setStartTs(alarmUpdateMsg.getStartTs());
        AlarmStatus alarmStatus = AlarmStatus.valueOf(alarmUpdateMsg.getStatus());
        alarm.setClearTs(alarmUpdateMsg.getClearTs());
        alarm.setPropagate(alarmUpdateMsg.getPropagate());
        alarm.setCleared(alarmStatus.isCleared());
        alarm.setAcknowledged(alarmStatus.isAck());
        alarm.setAckTs(alarmUpdateMsg.getAckTs());
        alarm.setEndTs(alarmUpdateMsg.getEndTs());
        alarm.setDetails(JacksonUtil.toJsonNode(alarmUpdateMsg.getDetails()));
        return alarm;
    }

    @Override
    protected EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        return getAlarmOriginator(tenantId, alarmUpdateMsg.getOriginatorName(),
                EntityType.valueOf(alarmUpdateMsg.getOriginatorType()));
    }

    private EntityId getAlarmOriginator(TenantId tenantId, String entityName, EntityType entityType) {
        return switch (entityType) {
            case DEVICE -> edgeCtx.getDeviceService().findDeviceByTenantIdAndName(tenantId, entityName).getId();
            case ASSET -> edgeCtx.getAssetService().findAssetByTenantIdAndName(tenantId, entityName).getId();
            case ENTITY_VIEW -> edgeCtx.getEntityViewService().findEntityViewByTenantIdAndName(tenantId, entityName).getId();
            default -> null;
        };
    }

}
