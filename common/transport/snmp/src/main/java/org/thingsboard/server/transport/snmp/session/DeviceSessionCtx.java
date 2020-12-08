/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class DeviceSessionCtx extends DeviceAwareSessionContext implements SessionMsgListener {
    private final AtomicInteger msgIdSeq = new AtomicInteger(0);

    @Getter
    @Setter
    private SnmpDeviceTransportConfiguration transportConfiguration;
    @Getter
    @Setter
    private SnmpSessionListener snmpSessionListener;
    @Getter
    @Setter
    private Target target;
    @Getter
    @Setter
    private volatile TransportProtos.SessionInfoProto sessionInfo;

    protected volatile ScheduledFuture<?> poolingTask;

    private Snmp snmp;

    public DeviceSessionCtx(SnmpTransportContext transportContext, String token, SnmpDeviceTransportConfiguration transportConfiguration, Snmp snmp, DeviceId deviceId, DeviceProfile deviceProfile) {
        super(UUID.randomUUID());
        this.snmpSessionListener = new SnmpSessionListener(transportContext, token);
        this.transportConfiguration = transportConfiguration;
        super.setDeviceId(deviceId);
        super.setDeviceProfile(deviceProfile);
        if (snmp != null) {
            this.snmp = snmp;
        }
        SnmpDeviceProfileTransportConfiguration config = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        initPoolingTask(config);
        initTarget(config);
    }

    private void initPoolingTask(SnmpDeviceProfileTransportConfiguration config) {
        poolingTask = snmpSessionListener.getSnmpTransportContext().getScheduler().scheduleWithFixedDelay(
                this::sendSnmpRequest,
                config.getPoolPeriodMs(), config.getPoolPeriodMs(), TimeUnit.MILLISECONDS);
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

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(sessionInfo, deviceProfile);
        poolingTask.cancel(true);
        if (DeviceTransportType.SNMP.equals(deviceProfile.getTransportType())) {
            SnmpDeviceProfileTransportConfiguration snmpDeviceProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
            snmpSessionListener.getSnmpTransportContext().getDeviceProfileTransportConfig().put(
                    deviceProfile.getId(),
                    snmpDeviceProfileTransportConfiguration);
            snmpSessionListener.getSnmpTransportContext().updatePduListPerProfile(deviceProfile.getId(), snmpDeviceProfileTransportConfiguration);
            initPoolingTask(snmpDeviceProfileTransportConfiguration);
        } else {
            //TODO: should the context be removed from the map?
        }
    }

    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        super.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
        poolingTask.cancel(true);
        if (super.getDeviceProfile() != null && DeviceTransportType.SNMP.equals(super.getDeviceProfile().getTransportType())) {
            snmpSessionListener.getSnmpTransportContext().updateDeviceSessionCtx(device, deviceProfile, null);
            initPoolingTask((SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration());
        } else {
            //TODO: should the context be removed from the map?
        }
    }

    public void createSessionInfo(Consumer<TransportProtos.SessionInfoProto> registerSession) {
        getSnmpSessionListener().getSnmpTransportContext().getTransportService().process(DeviceTransportType.SNMP,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(getSnmpSessionListener().getToken()).build(),
                new TransportServiceCallback<ValidateDeviceCredentialsResponse>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (msg.hasDeviceInfo()) {
                            sessionInfo = SessionInfoCreator.create(msg, getSnmpSessionListener().getSnmpTransportContext(), UUID.randomUUID());
                            registerSession.accept(sessionInfo);
                            setDeviceInfo(msg.getDeviceInfo());
                        } else {
                            log.warn("[{}] Failed to process device auth", getDeviceId());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to process device auth", getDeviceId(), e);
                    }
                });
    }

    private void initTarget(SnmpDeviceProfileTransportConfiguration config) {
        CommunityTarget communityTarget = new CommunityTarget();
        communityTarget.setAddress(GenericAddress.parse(GenericAddress.TYPE_UDP + ":" + transportConfiguration.getAddress() + "/" + transportConfiguration.getPort()));
        communityTarget.setVersion(getSnmpVersion(transportConfiguration.getProtocolVersion()));
        communityTarget.setCommunity(new OctetString(transportConfiguration.getCommunity()));
        communityTarget.setTimeout(config.getTimeoutMs());
        communityTarget.setRetries(config.getRetries());
        this.target = communityTarget;
    }

    public void sendSnmpRequest() {
        snmpSessionListener.getSnmpTransportContext().getPdusPerProfile().get(deviceProfile.getId()).forEach(pdu -> {
            try {
                log.info("[{}] Sending SNMP message...", pdu.getRequestID());
                snmp.send(pdu,
                        target,
                        deviceProfile.getId(),
                        snmpSessionListener);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    //TODO: replace with enum, wtih preliminary discussion of type version in config (string or integer)
    private int getSnmpVersion(String configSnmpVersion) {
        switch (configSnmpVersion) {
            case ("v1"):
                return SnmpConstants.version1;
            case ("v2c"):
                return SnmpConstants.version2c;
            case ("v3"):
                return SnmpConstants.version3;
            default:
                return -1;
        }
    }
}
