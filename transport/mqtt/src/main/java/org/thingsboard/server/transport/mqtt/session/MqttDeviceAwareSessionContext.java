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
package org.thingsboard.server.transport.mqtt.session;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 30.08.18.
 */
public abstract class MqttDeviceAwareSessionContext extends DeviceAwareSessionContext {

    private final ConcurrentMap<String, Integer> mqttQoSMap;

    public MqttDeviceAwareSessionContext(SessionMsgProcessor processor, DeviceAuthService authService, ConcurrentMap<String, Integer> mqttQoSMap) {
        super(processor, authService);
        this.mqttQoSMap = mqttQoSMap;
    }

    public MqttDeviceAwareSessionContext(SessionMsgProcessor processor, DeviceAuthService authService, Device device, ConcurrentMap<String, Integer> mqttQoSMap) {
        super(processor, authService, device);
        this.mqttQoSMap = mqttQoSMap;
    }

    public ConcurrentMap<String, Integer> getMqttQoSMap() {
        return mqttQoSMap;
    }

    public MqttQoS getQoSForTopic(String topic) {
        Integer qos = mqttQoSMap.get(topic);
        if (qos != null) {
            return MqttQoS.valueOf(qos);
        } else {
            return MqttQoS.AT_LEAST_ONCE;
        }
    }

}
