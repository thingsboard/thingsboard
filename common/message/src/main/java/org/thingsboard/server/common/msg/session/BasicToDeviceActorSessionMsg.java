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
package org.thingsboard.server.common.msg.session;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.id.TenantId;

public class BasicToDeviceActorSessionMsg implements ToDeviceActorSessionMsg {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final AdaptorToSessionActorMsg msg;

    public BasicToDeviceActorSessionMsg(Device device, AdaptorToSessionActorMsg msg) {
        super();
        this.tenantId = device.getTenantId();
        this.customerId = device.getCustomerId();
        this.deviceId = device.getId();
        this.msg = msg;
    }

    public BasicToDeviceActorSessionMsg(ToDeviceActorSessionMsg deviceMsg) {
        this.tenantId = deviceMsg.getTenantId();
        this.customerId = deviceMsg.getCustomerId();
        this.deviceId = deviceMsg.getDeviceId();
        this.msg = deviceMsg.getSessionMsg();
    }

    @Override
    public DeviceId getDeviceId() {
        return deviceId;
    }

    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public SessionId getSessionId() {
        return msg.getSessionId();
    }

    @Override
    public AdaptorToSessionActorMsg getSessionMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "BasicToDeviceActorSessionMsg [tenantId=" + tenantId + ", customerId=" + customerId + ", deviceId=" + deviceId + ", msg=" + msg
                + "]";
    }

}
