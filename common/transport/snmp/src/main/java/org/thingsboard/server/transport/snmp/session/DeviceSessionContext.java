/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.snmp.session;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DeviceSessionContext extends DeviceAwareSessionContext implements SessionMsgListener, ResponseListener {
    @Getter
    private Target target;
    private final String token;
    @Getter
    @Setter
    private SnmpDeviceProfileTransportConfiguration profileTransportConfiguration;
    @Getter
    @Setter
    private SnmpDeviceTransportConfiguration deviceTransportConfiguration;
    @Getter
    private final Device device;

    private final SnmpTransportContext snmpTransportContext;
    private final SnmpTransportService snmpTransportService;

    @Getter
    @Setter
    private long previousRequestExecutedAt = 0;
    private final AtomicInteger msgIdSeq = new AtomicInteger(0);
    private boolean isActive = true;
    private final String snmpUnderlyingProtocol;

    public DeviceSessionContext(Device device, DeviceProfile deviceProfile, String token,
                                SnmpDeviceTransportConfiguration deviceTransportConfiguration,
                                SnmpTransportContext snmpTransportContext, SnmpTransportService snmpTransportService,
                                String snmpUnderlyingProtocol) {
        super(UUID.randomUUID());
        super.setDeviceId(device.getId());
        super.setDeviceProfile(deviceProfile);
        this.device = device;

        this.token = token;
        this.snmpTransportContext = snmpTransportContext;
        this.snmpTransportService = snmpTransportService;

        this.profileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        this.deviceTransportConfiguration = deviceTransportConfiguration;

        this.snmpUnderlyingProtocol = snmpUnderlyingProtocol;
        initTarget(this.profileTransportConfiguration, this.deviceTransportConfiguration);
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(newSessionInfo, deviceProfile);
        if (isActive) {
            snmpTransportContext.onDeviceProfileUpdated(deviceProfile, this);
        }
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        snmpTransportContext.onDeviceDeleted(this);
    }

    @Override
    public void onResponse(ResponseEvent event) {
        if (isActive) {
            snmpTransportService.onNewDeviceResponse(this, event);
        }
    }

    public void initTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) {
        log.trace("Initializing target for SNMP session of device {}", device);
        CommunityTarget communityTarget = new CommunityTarget();
        communityTarget.setAddress(GenericAddress.parse(snmpUnderlyingProtocol + ":" + deviceTransportConfig.getAddress() + "/" + deviceTransportConfig.getPort()));
        communityTarget.setVersion(deviceTransportConfig.getProtocolVersion().getCode());
        communityTarget.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
        communityTarget.setTimeout(profileTransportConfig.getTimeoutMs());
        communityTarget.setRetries(profileTransportConfig.getRetries());
        this.target = communityTarget;
        log.info("SNMP target initialized: {}", this.target);
    }

    public void close() {
        isActive = false;
    }

    public String getToken() {
        return token;
    }

    @Override
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
    }

    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg attributeUpdateNotification) {
    }

    @Override
    public void onRemoteSessionCloseCommand(SessionCloseNotificationProto sessionCloseNotification) {
    }

    @Override
    public void onToDeviceRpcRequest(ToDeviceRpcRequestMsg toDeviceRequest) {
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
    }
}
