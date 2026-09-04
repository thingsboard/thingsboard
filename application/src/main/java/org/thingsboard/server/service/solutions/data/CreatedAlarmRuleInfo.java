// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

public record CreatedAlarmRuleInfo(EntityId entityId, String entityName, String alarmType, String severities) {

    public String getCfPageLink(UUID cfId) {
        return "/alarms/alarm-rules/" + cfId;
    }

    public String getEntityPageLink() {
        if (entityId == null) {
            return null;
        }
        return switch (entityId.getEntityType()) {
            case DEVICE_PROFILE -> "/profiles/deviceProfiles/" + entityId.getId();
            case ASSET_PROFILE -> "/profiles/assetProfiles/" + entityId.getId();
            default -> null;
        };
    }

}
