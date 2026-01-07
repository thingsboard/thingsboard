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
