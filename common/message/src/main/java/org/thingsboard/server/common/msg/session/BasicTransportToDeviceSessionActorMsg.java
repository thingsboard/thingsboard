/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.util.Set;

public class BasicTransportToDeviceSessionActorMsg implements TransportToDeviceSessionActorMsg {

    private final TenantId tenantId;
    private final Set<ShortCustomerInfo> assignedCustomers;
    private final DeviceId deviceId;
    private final AdaptorToSessionActorMsg msg;

    public BasicTransportToDeviceSessionActorMsg(Device device, AdaptorToSessionActorMsg msg) {
        super();
        this.tenantId = device.getTenantId();
        this.assignedCustomers = device.getAssignedCustomers();
        this.deviceId = device.getId();
        this.msg = msg;
    }

    @Override
    public DeviceId getDeviceId() {
        return deviceId;
    }

    @Override
    public Set<ShortCustomerInfo> getAssignedCustomers() {
        return assignedCustomers;
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
        return "BasicTransportToDeviceSessionActorMsg [tenantId=" + tenantId + ", deviceId=" + deviceId + ", msg=" + msg
                + "]";
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.TRANSPORT_TO_DEVICE_SESSION_ACTOR_MSG;
    }
}
