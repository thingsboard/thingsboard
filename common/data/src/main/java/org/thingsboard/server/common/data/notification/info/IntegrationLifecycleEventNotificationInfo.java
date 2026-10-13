// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationLifecycleEventNotificationInfo implements RuleOriginatedNotificationInfo {

    private IntegrationId integrationId;
    private String integrationType;
    private String integrationName;
    private String action;
    private ComponentLifecycleEvent eventType;
    private String error;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "integrationId", integrationId.toString(),
                "integrationType", integrationType,
                "integrationName", integrationName,
                "action", action,
                "eventType", eventType.name().toLowerCase(),
                "error", error
        );
    }

    @Override
    public EntityId getStateEntityId() {
        return integrationId;
    }

}
