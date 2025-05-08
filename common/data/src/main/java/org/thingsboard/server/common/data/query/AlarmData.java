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
package org.thingsboard.server.common.data.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
public class AlarmData extends AlarmInfo {

    private static final long serialVersionUID = -7042457913823369638L;

    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public AlarmData(AlarmInfo main, AlarmData prototype) {
        super(main);
        this.entityId = prototype.entityId;
        this.latest = new HashMap<>();
        this.latest.putAll(prototype.getLatest());
    }

    public AlarmData(Alarm alarm, EntityId entityId) {
        super(alarm);
        this.entityId = entityId;
        this.latest = new HashMap<>();
    }
}
