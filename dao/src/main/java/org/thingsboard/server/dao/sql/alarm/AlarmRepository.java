/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.dao.model.sql.AlarmEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public interface AlarmRepository extends CrudRepository<AlarmEntity, UUID> {

    @Query(nativeQuery = true, value = "SELECT * FROM ALARM WHERE TENANT_ID = ?1 AND ORIGINATOR_ID = ?2 " +
            "AND ?3 = ?3 AND TYPE = ?4 ORDER BY ID DESC LIMIT 1")
    AlarmEntity findLatestByOriginatorAndType(UUID tenantId, UUID originatorId, int entityType, String alarmType);
}
