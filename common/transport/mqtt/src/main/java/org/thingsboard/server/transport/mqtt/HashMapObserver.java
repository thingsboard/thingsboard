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
package org.thingsboard.server.transport.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.mqtt.session.GatewayDeviceSessionContext;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class HashMapObserver implements HashMapObserverMBean {
    private final Map<UUID, SessionMetaData> map;

    @Override
    public int getSize() {
        return map.size();
    }

    @Override
    public long getGatewayCount(String unused) {
        return map.values().stream().filter(v-> v.getSessionInfo() != null && v.getSessionInfo().getIsGateway()).count();
    }

    @Override
    public long getNonGatewayCount(String unused) {
        return map.values().stream().filter(v-> v.getSessionInfo() != null && !v.getSessionInfo().getIsGateway()).count();
    }

    @Override
    public String getSessionByUUID(String uuid) {
        return String.valueOf(map.get(UUID.fromString(uuid)));
    }

    void addContent(Object entry, int count, StringBuilder content) {
        String lineContent = String.valueOf(entry).replaceAll(System.lineSeparator()," ");
        log.info("{} content = {}", count, lineContent);
        content.append(lineContent).append(System.lineSeparator());
    }

    @Override
    public String getAllSessions(String unused) {
        log.info("getAllSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                addContent(entry, ++count, content);
            }
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getSubscribedSessions(String unused) {
        log.info("getSubscribedSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                boolean hasSubscription = entry.getValue().isSubscribedToRPC() || entry.getValue().isSubscribedToAttributes();
                if (hasSubscription) {
                    addContent(entry, ++count, content);
                }
            }
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getNonActiveSessions(String unused) {
        log.info("getNonActiveSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                SessionMetaData sessionMetaData = entry.getValue();
                if (sessionMetaData.getListener() instanceof MqttTransportHandler) {
                    MqttTransportHandler listener = (MqttTransportHandler) sessionMetaData.getListener();
                    if (!listener.deviceSessionCtx.getChannel().channel().isActive()) {
                        addContent(entry, ++count, content);
                    }
                }
            }
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getActiveSessions(String unused) {
        log.info("getActiveSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                SessionMetaData sessionMetaData = entry.getValue();
                if (sessionMetaData.getListener() instanceof MqttTransportHandler) {
                    MqttTransportHandler listener = (MqttTransportHandler) sessionMetaData.getListener();
                    if (listener.deviceSessionCtx.getChannel().channel().isActive()) {
                        addContent(entry, ++count, content);
                    }
                } else {
                    addContent(sessionMetaData.getListener().getClass(), ++count, content);
                }
            }
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            throw e;
        }
    }

    @Override
    public String getGatewayDeviceSessionContextConnectedSessions(String unused) {
        log.info("getGatewayDeviceSessionContextConnectedSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                SessionMetaData sessionMetaData = entry.getValue();
                if (sessionMetaData.getListener() instanceof GatewayDeviceSessionContext) {
                    GatewayDeviceSessionContext listener = (GatewayDeviceSessionContext) sessionMetaData.getListener();
                    if (listener.isConnected()) {
                        addContent(entry, ++count, content);
                    }
                }
            }
            addContent(count, count, content);
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }
    @Override
    public String getDeviceAwareSessionContextNotConnectedSessions(String unused) {
        log.info("getDeviceAwareSessionContextNotConnectedSessions()");
        StringBuilder content = new StringBuilder();
        try {
            int count = 0;
            for (Map.Entry<UUID, SessionMetaData> entry : map.entrySet()) {
                SessionMetaData sessionMetaData = entry.getValue();
                if (sessionMetaData.getListener() instanceof DeviceAwareSessionContext) {
                    DeviceAwareSessionContext listener = (DeviceAwareSessionContext) sessionMetaData.getListener();
                    if (!listener.isConnected()) {
                        addContent(entry, ++count, content);
                    }
                }
            }
            addContent(count, count, content);
            return content.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

}

// 4a7d85c9-eb4b-4fbc-8f6c-deb158cc9ac7=SessionMetaData(sessionInfo=nodeId: "bestia.local" sessionIdMSB: 5367593433178001340 sessionIdLSB: -8111863975520724281 tenantIdMSB: -1954197196874116625 tenantIdLSB: -7022192637061768255 deviceIdMSB: -5222516332438875665 deviceIdLSB: -7338416368958642691 deviceName: "Demo Device" deviceType: "default" gwSessionIdMSB: 3730140660294699909 gwSessionIdLSB: -7918622346767288875 deviceProfileIdMSB: -1952135612572036625 deviceProfileIdLSB: -7022192637061768255 customerIdMSB: 1405474927960789426 customerIdLSB: -9187201950435737472 gatewayIdMSB: 164837549830312431 gatewayIdLSB: -7338416368958642691 , sessionType=ASYNC, listener=GatewayDeviceSessionContext(super=AbstractGatewayDeviceSessionContext(super=MqttDeviceAwareSessionContext(super=DeviceAwareSessionContext(sessionId=4a7d85c9-eb4b-4fbc-8f6c-deb158cc9ac7, deviceId=b785e510-d34e-11ef-9a28-b5316a4ee5fd, tenantId=e4e14c00-d341-11ef-9e8c-29007391dbc1, deviceInfo=TransportDeviceInfo(tenantId=e4e14c00-d341-11ef-9e8c-29007391dbc1, customerId=13814000-1dd2-11b2-8080-808080808080, deviceProfileId=e4e89f00-d341-11ef-9e8c-29007391dbc1, deviceId=b785e510-d34e-11ef-9a28-b5316a4ee5fd, deviceName=Demo Device, deviceType=default, powerMode=null, additionalInfo={"lastConnectedGateway":"02499ee0-d348-11ef-9a28-b5316a4ee5fd"}, edrxCycle=null, psmActivityTimer=null, pagingTransmissionWindow=null, gateway=false), deviceProfile=DeviceProfile(tenantId=e4e14c00-d341-11ef-9e8c-29007391dbc1, name=default, description=Default device profile, isDefault=true, type=DEFAULT, transportType=DEFAULT, provisionType=DISABLED, defaultRuleChainId=null, defaultDashboardId=null, defaultQueueName=null, profileData=DeviceProfileData(configuration=DefaultDeviceProfileConfiguration(), transportConfiguration=DefaultDeviceProfileTransportConfiguration(), provisionConfiguration=DisabledDeviceProfileProvisionConfiguration(provisionDeviceSecret=null), alarms=null), provisionDeviceKey=null, firmwareId=null, softwareId=null, defaultEdgeRuleChainId=null, externalId=null, version=1), sessionInfo=nodeId: "bestia.local" sessionIdMSB: 5367593433178001340 sessionIdLSB: -8111863975520724281 tenantIdMSB: -1954197196874116625 tenantIdLSB: -7022192637061768255 deviceIdMSB: -5222516332438875665 deviceIdLSB: -7338416368958642691 deviceName: "Demo Device" deviceType: "default" gwSessionIdMSB: 3730140660294699909 gwSessionIdLSB: -7918622346767288875 deviceProfileIdMSB: -1952135612572036625 deviceProfileIdLSB: -7022192637061768255 customerIdMSB: 1405474927960789426 customerIdLSB: -9187201950435737472 gatewayIdMSB: 164837549830312431 gatewayIdLSB: -7338416368958642691 , connected=true), mqttQoSMap={org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@697a7658=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@d44c5be8=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@e3a8acd0=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@e3d2681a=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@a65b8616=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@133a8264=1, org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher@12e92606=1}), parent=org.thingsboard.server.transport.mqtt.session.GatewaySessionHandler@3d4885c2, transportService=org.thingsboard.server.common.transport.service.DefaultTransportService@77c16f87)), scheduledFuture=null, subscribedToAttributes=true, subscribedToRPC=true, overwriteActivityTime=false)
//1