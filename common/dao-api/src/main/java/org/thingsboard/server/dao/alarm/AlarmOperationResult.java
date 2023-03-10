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
package org.thingsboard.server.dao.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmAssigneeUpdate;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@Deprecated
public class AlarmOperationResult {
    private final Alarm alarm;
    private final boolean successful;
    private final boolean created;
    private final AlarmSeverity oldSeverity;
    private final List<EntityId> propagatedEntitiesList;

    public AlarmOperationResult(Alarm alarm, boolean successful) {
        this(alarm, successful, Collections.emptyList());
    }

    public AlarmOperationResult(Alarm alarm, boolean successful, List<EntityId> propagatedEntitiesList) {
        this(alarm, successful, false, null, propagatedEntitiesList);
    }

    public AlarmOperationResult(Alarm alarm, boolean successful, boolean created, List<EntityId> propagatedEntitiesList) {
        this.alarm = alarm;
        this.successful = successful;
        this.created = created;
        this.propagatedEntitiesList = propagatedEntitiesList;
        this.oldSeverity = null;
    }

    //Temporary while we have not removed the AlarmOperationResult.
    public AlarmOperationResult(AlarmApiCallResult result) {
        this.alarm = result.getAlarm() != null ? new Alarm(result.getAlarm()) : null;
        this.successful = result.isSuccessful() && (result.isCreated() || result.isModified());
        this.created = result.isCreated();
        this.oldSeverity = result.getOldSeverity();
        this.propagatedEntitiesList = result.getPropagatedEntitiesList();
    }
}
