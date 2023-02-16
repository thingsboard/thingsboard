/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
public class AlarmData extends AlarmInfo {

    private static final long serialVersionUID = -7042457913823369638L;

    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public AlarmData update(Alarm alarm) {
        this.setEndTs(alarm.getEndTs());
        this.setSeverity(alarm.getSeverity());
        this.setStatus(alarm.getStatus());
        this.setDetails(alarm.getDetails());
        this.setPropagate(alarm.isPropagate());
        this.setPropagateToOwner(alarm.isPropagateToOwner());
        this.setPropagateToTenant(alarm.isPropagateToTenant());
        this.setPropagateRelationTypes(alarm.getPropagateRelationTypes());
        // This should be changed via separate message?
        this.setAckTs(alarm.getAckTs());
        this.setClearTs(alarm.getClearTs());
        return this;
    }

    public AlarmData(Alarm alarm, EntityId entityId) {
        super(alarm);
        this.entityId = entityId;
        this.latest = new HashMap<>();
    }
}
