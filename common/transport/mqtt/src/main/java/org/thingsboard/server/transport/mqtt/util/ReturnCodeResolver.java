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
package org.thingsboard.server.transport.mqtt.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReturnCodeResolver {

    public static MqttConnectReturnCode getConnectionReturnCode(MqttVersion mqttVersion, MqttConnectReturnCode returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !MqttConnectReturnCode.CONNECTION_ACCEPTED.equals(returnCode)) {
            switch (returnCode) {
                case CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD:
                case CONNECTION_REFUSED_NOT_AUTHORIZED_5:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
                case CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
                case CONNECTION_REFUSED_SERVER_UNAVAILABLE_5:
                case CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
                default:
                    log.warn("Unknown return code for conversion: {}", returnCode.name());
            }
        }
        return MqttConnectReturnCode.valueOf(returnCode.byteValue());
    }

    public static int getSubscriptionReturnCode(MqttVersion mqttVersion, MqttReasonCodes.SubAck returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !(MqttReasonCodes.SubAck.GRANTED_QOS_0.equals(returnCode) ||
                MqttReasonCodes.SubAck.GRANTED_QOS_1.equals(returnCode) ||
                MqttReasonCodes.SubAck.GRANTED_QOS_2.equals(returnCode))) {
            switch (returnCode) {
                case UNSPECIFIED_ERROR:
                case TOPIC_FILTER_INVALID:
                case IMPLEMENTATION_SPECIFIC_ERROR:
                case NOT_AUTHORIZED:
                case PACKET_IDENTIFIER_IN_USE:
                case QUOTA_EXCEEDED:
                case SHARED_SUBSCRIPTIONS_NOT_SUPPORTED:
                case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
                case WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED:
                    return MqttQoS.FAILURE.value();
            }
        }
        return returnCode.byteValue();
    }
}
