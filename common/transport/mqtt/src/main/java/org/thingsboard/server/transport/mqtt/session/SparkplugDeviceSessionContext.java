/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class SparkplugDeviceSessionContext extends GatewayDeviceSessionContext{

    public SparkplugDeviceSessionContext(AbstractGatewaySessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                         Integer> mqttQoSMap,
                                         TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProto -> {
            if (getMetricsBirthDevice().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(((SparkplugNodeSessionHandler)parent).getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                ((SparkplugNodeSessionHandler)parent).createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getMetricsBirthDevice().get(tsKvProto.getKv().getKey()))
                        .ifPresent(parent::writeAndFlush);
            }
        });
    }

}
