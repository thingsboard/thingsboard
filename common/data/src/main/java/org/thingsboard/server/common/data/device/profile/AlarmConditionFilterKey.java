/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

@Data
public class AlarmConditionFilterKey {

    private final AlarmConditionKeyType type;
    private final String key;

    public static AlarmConditionFilterKey fromEntityKey(EntityKey entityKey) {
        String key = entityKey.getKey();
        EntityKeyType entityKeyType = entityKey.getType();

        AlarmConditionKeyType alarmConditionKeyType;
        switch (entityKeyType) {
            case ATTRIBUTE:
            case CLIENT_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case SERVER_ATTRIBUTE:
                alarmConditionKeyType = AlarmConditionKeyType.ATTRIBUTE;
                break;
            case TIME_SERIES:
                alarmConditionKeyType = AlarmConditionKeyType.TIME_SERIES;
                break;
            case ENTITY_FIELD:
            case ALARM_FIELD:
                alarmConditionKeyType = AlarmConditionKeyType.ENTITY_FIELD;
                break;
            default:
                return null;
        }

        return new AlarmConditionFilterKey(alarmConditionKeyType, key);
    }

}
