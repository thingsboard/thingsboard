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
package org.thingsboard.server.extensions.api.device;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * @author Andrew Shvayka
 */
@ToString
public class DeviceNameOrTypeUpdateMsg implements ToDeviceActorNotificationMsg {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final DeviceId deviceId;
    @Getter
    private final String deviceName;
    @Getter
    private final String deviceType;

    public DeviceNameOrTypeUpdateMsg(TenantId tenantId, DeviceId deviceId, String deviceName, String deviceType) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }
}
