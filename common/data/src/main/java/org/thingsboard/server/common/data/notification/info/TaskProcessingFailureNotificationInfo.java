/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProcessingFailureNotificationInfo implements RuleOriginatedNotificationInfo {

    private TenantId tenantId;
    private EntityId entityId;
    private HousekeeperTaskType taskType;
    private String taskDescription;
    private String error;
    private int attempt;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "tenantId", tenantId.toString(),
                "entityType", entityId.getEntityType().getNormalName(),
                "entityId", entityId.getId().toString(),
                "taskType", taskType.getDescription(),
                "taskDescription", taskDescription,
                "error", error,
                "attempt", String.valueOf(attempt)
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

}
