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
package org.thingsboard.server.transport.mqtt.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReturnCodeResolver {

    public static MqttConnectReturnCode getConnectionReturnCode(MqttVersion mqttVersion, MqttConnectReturnCode returnCode) {
        if (MqttVersion.MQTT_5.equals(mqttVersion) || MqttConnectReturnCode.CONNECTION_ACCEPTED.equals(returnCode)) {
            return returnCode;
        }
        switch (returnCode) {
            case CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD:
                return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
            case CONNECTION_REFUSED_NOT_AUTHORIZED_5:
                return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE_5:
                return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
            case CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID:
                return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
            default:
                log.warn("Unknown return code for conversion: {}", returnCode.name());
                return returnCode;
        }
    }
}
