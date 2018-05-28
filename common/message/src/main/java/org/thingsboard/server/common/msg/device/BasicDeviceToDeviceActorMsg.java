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
package org.thingsboard.server.common.msg.device;

import lombok.ToString;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.TransportToDeviceSessionActorMsg;

import java.util.Optional;

@ToString
public class BasicDeviceToDeviceActorMsg implements DeviceToDeviceActorMsg {

    private static final long serialVersionUID = -1866795134993115408L;

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final SessionId sessionId;
    private final SessionType sessionType;
    private final ServerAddress serverAddress;
    private final FromDeviceMsg msg;

    public BasicDeviceToDeviceActorMsg(DeviceToDeviceActorMsg other, FromDeviceMsg msg) {
        this(null, other.getTenantId(), other.getCustomerId(), other.getDeviceId(), other.getSessionId(), other.getSessionType(), msg);
    }

    public BasicDeviceToDeviceActorMsg(TransportToDeviceSessionActorMsg msg, SessionType sessionType) {
        this(null, msg.getTenantId(), msg.getCustomerId(), msg.getDeviceId(), msg.getSessionId(), sessionType, msg.getSessionMsg().getMsg());
    }

    private BasicDeviceToDeviceActorMsg(ServerAddress serverAddress, TenantId tenantId, CustomerId customerId, DeviceId deviceId, SessionId sessionId, SessionType sessionType,
                                        FromDeviceMsg msg) {
        super();
        this.serverAddress = serverAddress;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.msg = msg;
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
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return sessionType;
    }

    @Override
    public Optional<ServerAddress> getServerAddress() {
        return Optional.ofNullable(serverAddress);
    }

    @Override
    public FromDeviceMsg getPayload() {
        return msg;
    }

    @Override
    public DeviceToDeviceActorMsg toOtherAddress(ServerAddress otherAddress) {
        return new BasicDeviceToDeviceActorMsg(otherAddress, tenantId, customerId, deviceId, sessionId, sessionType, msg);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_SESSION_TO_DEVICE_ACTOR_MSG;
    }
}
