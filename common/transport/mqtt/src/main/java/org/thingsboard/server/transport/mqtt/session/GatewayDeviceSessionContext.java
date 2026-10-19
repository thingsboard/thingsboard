// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.session;

import lombok.ToString;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;

import java.util.concurrent.ConcurrentMap;

@ToString(callSuper = true)
public class GatewayDeviceSessionContext extends AbstractGatewayDeviceSessionContext<GatewaySessionHandler> {

    public GatewayDeviceSessionContext(GatewaySessionHandler parent,
                                       TransportDeviceInfo deviceInfo,
                                       DeviceProfile deviceProfile,
                                       ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                       TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

}
