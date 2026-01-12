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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityActionNotificationInfo implements RuleOriginatedNotificationInfo {

    private EntityId entityId;
    private String entityName;
    private ActionType actionType;
    private CustomerId entityCustomerId;

    private UUID userId;
    private String userTitle;
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "entityType", entityId.getEntityType().getNormalName(),
                "entityId", entityId.toString(),
                "entityName", entityName,
                "actionType", actionType.name().toLowerCase(),
                "userId", userId.toString(),
                "userTitle", userTitle,
                "userEmail", userEmail,
                "userFirstName", userFirstName,
                "userLastName", userLastName
        );
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return entityCustomerId;
    }

    @Override
    public EntityId getStateEntityId() {
        return entityId;
    }

}
