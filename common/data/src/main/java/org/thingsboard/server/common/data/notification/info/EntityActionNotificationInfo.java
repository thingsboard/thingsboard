// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    private CustomerId customerId;

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
        return customerId;
    }

    @Override
    public EntityId getStateEntityId() {
        return entityId;
    }

}
