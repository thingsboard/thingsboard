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
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    @Getter
    private boolean isActive = true;
    private final String snmpUnderlyingProtocol;

    @Getter
    @Setter
    private List<ScheduledFuture<?>> queryingTasks = new LinkedList<>();

    public DeviceSessionContext(Device device, DeviceProfile deviceProfile, String token,
                                SnmpDeviceProfileTransportConfiguration profileTransportConfiguration,
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

        this.profileTransportConfiguration = profileTransportConfiguration;
        this.deviceTransportConfiguration = deviceTransportConfiguration;

        this.snmpUnderlyingProtocol = snmpUnderlyingProtocol;
        initializeTarget(profileTransportConfiguration, deviceTransportConfiguration);
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
            snmpTransportService.processResponseEvent(this, event);
        }
    }

    public void initializeTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) {
        log.trace("Initializing target for SNMP session of device {}", device);

        AbstractTarget target;

        SnmpProtocolVersion protocolVersion = deviceTransportConfig.getProtocolVersion();
        switch (protocolVersion) {
            case V1:
                CommunityTarget communityTargetV1 = new CommunityTarget();
                communityTargetV1.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv1);
                communityTargetV1.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV1.setCommunity(new OctetString(deviceTransportConfig.getSecurityName()));
                target = communityTargetV1;
                break;
            case V2C:
                CommunityTarget communityTargetV2 = new CommunityTarget();
                communityTargetV2.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
                communityTargetV2.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                communityTargetV2.setCommunity(new OctetString(deviceTransportConfig.getSecurityName()));
                target = communityTargetV2;
                break;
            case V3:
                USM usm = new USM();
                SecurityModels.getInstance().addSecurityModel(usm);

                OctetString securityName = new OctetString(deviceTransportConfig.getSecurityName());
                OctetString authenticationPassphrase = new OctetString(deviceTransportConfig.getAuthenticationPassphrase());
                OctetString privacyPassphrase = new OctetString(deviceTransportConfig.getPrivacyPassphrase());

                OID authenticationProtocol = AuthSHA.ID;
                OID privacyProtocol = PrivDES.ID; // FIXME: to config

                UsmUser user = new UsmUser(securityName, authenticationProtocol, authenticationPassphrase, privacyProtocol, privacyPassphrase);
                snmpTransportService.getSnmp().getUSM().addUser(user);

                UserTarget userTarget = new UserTarget();
                userTarget.setSecurityName(securityName);
                userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);

                target = userTarget;
                break;
            default:
                throw new UnsupportedOperationException("SNMP protocol version " + protocolVersion + " is not supported");
        }

        target.setAddress(GenericAddress.parse(snmpUnderlyingProtocol + ":" + deviceTransportConfig.getAddress() + "/" + deviceTransportConfig.getPort()));
        target.setTimeout(profileTransportConfig.getTimeoutMs());
        target.setRetries(profileTransportConfig.getRetries());
        target.setVersion(protocolVersion.getCode());

        this.target = target;
        log.info("SNMP target initialized: {}", target);
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
        profileTransportConfiguration.getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING)
                .findFirst()
                .ifPresent(communicationConfig -> {
                    Map<String, String> sharedAttributes = JsonConverter.toJson(attributeUpdateNotification).entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString()
                            ));
                    try {
                        snmpTransportService.sendRequest(this, communicationConfig, sharedAttributes);
                    } catch (Exception e) {
                        log.error("Failed to send request with shared attributes to SNMP device {}: {}", getDeviceId(), e.getMessage());
                    }
                });
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
