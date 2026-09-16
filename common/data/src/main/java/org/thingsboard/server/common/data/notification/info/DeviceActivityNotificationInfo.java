// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceActivityNotificationInfo implements RuleOriginatedNotificationInfo {

    private String eventType;
    private UUID deviceId;
    private String deviceName;
    private String deviceLabel;
    private String deviceType;
    private CustomerId deviceCustomerId;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "eventType", eventType,
                "deviceId", deviceId.toString(),
                "deviceName", deviceName,
                "deviceLabel", deviceLabel,
                "deviceType", deviceType
        );
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return deviceCustomerId;
    }

    @Override
    public EntityId getStateEntityId() {
        return new DeviceId(deviceId);
    }

}
