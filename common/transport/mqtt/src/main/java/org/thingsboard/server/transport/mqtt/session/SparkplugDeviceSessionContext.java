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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
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
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            }
        });
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            /**
             *  NCMD {"metricName":"MyNodeMetric05_String","value":"MyNodeMetric05_String_Value"}
             *  NCMD {"metricName":"MyNodeMetric02_LongInt64","value":2814119464032075444}
             *  NCMD {"metricName":"MyNodeMetric03_Double","value":6336935578763180333}
             *  NCMD {"metricName":"MyNodeMetric04_Float","value":413.18222}
             *  NCMD {"metricName":"Node Control/Rebirth","value":false}
             *  NCMD {"metricName":"MyNodeMetric06_Json_Bytes", "value":[40,47,-49]}
             *  NCMD {"metricName":"Node Control/Rebirth", "value":false}
             *  without backspace
             */
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
//            if (messageType == null) {
//                parent.sendErrorRpcResponse(parent.deviceSessionCtx., rpcRequest.getRequestId(),
//                        ResponseCode.METHOD_NOT_ALLOWED, "Unsupported SparkplugMessageType: " + rpcRequest.getMethodName() + rpcRequest.getParams());
//                return;
//            }
//            SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
//            header.setMessageType(messageType.name());
//            TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
//            if (sparkplugSessionHandler.getNodeBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
//                SparkplugTopic sparkplugTopic = new SparkplugTopic(sparkplugSessionHandler.getSparkplugTopicNode(),
//                        messageType);
//                sparkplugSessionHandler.createSparkplugMqttPublishMsg(tsKvProto,
//                        sparkplugTopic.toString(),
//                        sparkplugSessionHandler.getNodeBirthMetrics().get(tsKvProto.getKv().getKey()))
//                        .ifPresent(payload -> sendToDeviceRpcRequest(payload, rpcRequest));
//            }
        } catch (ThingsboardException e) {
            new ThingsboardException(e.getMessage(), ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

}
