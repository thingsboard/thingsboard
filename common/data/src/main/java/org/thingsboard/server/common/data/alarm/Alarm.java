/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

/**
 * Created by ashvayka on 11.05.17.
 */
@Data
@Builder
@AllArgsConstructor
public class Alarm extends BaseData<AlarmId> implements HasName, HasTenantId {

    private TenantId tenantId;
    private String type;
    private EntityId originator;
    private AlarmSeverity severity;
    private AlarmStatus status;
    private long startTs;
    private long endTs;
    private long ackTs;
    private long clearTs;
    private transient JsonNode details;
    private boolean propagate;
    private List<String> propagateRelationTypes;

    public Alarm() {
        super();
    }

    public Alarm(AlarmId id) {
        super(id);
    }

    public Alarm(Alarm alarm) {
        super(alarm.getId());
        this.createdTime = alarm.getCreatedTime();
        this.tenantId = alarm.getTenantId();
        this.type = alarm.getType();
        this.originator = alarm.getOriginator();
        this.severity = alarm.getSeverity();
        this.status = alarm.getStatus();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.details = alarm.getDetails();
        this.propagate = alarm.isPropagate();
        this.propagateRelationTypes = alarm.getPropagateRelationTypes();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return type;
    }
}
