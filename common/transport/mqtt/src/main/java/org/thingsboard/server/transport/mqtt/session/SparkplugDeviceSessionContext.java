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
package org.thingsboard.server.transport.mqtt.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProtoFromJsonNode;

@Slf4j
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    private final Map<String, SparkplugBProto.Payload.Metric> deviceBirthMetrics = new ConcurrentHashMap<>();

    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                                 Integer> mqttQoSMap,
                                         TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    public Map<String, SparkplugBProto.Payload.Metric> getDeviceBirthMetrics() {
        return deviceBirthMetrics;
    }

    public void setDeviceBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics)  {
        for (var metric : metrics) {
            if (metric.hasName()) {
                this.deviceBirthMetrics.put(metric.getName(), metric);
            } else {
                throw new IllegalArgumentException("The metric name of device: '" + this.getDeviceInfo().getDeviceName() + "' must not be empty or null! Metric: [" + metric + "]");
            }
            if (metric.hasAlias() && this.parent.getNodeAlias().putIfAbsent(metric.getAlias(), metric.getName()) != null) {
                throw new DuplicateKeyException("The alias '" + metric.getAlias() + "' already exists in device: '" + this.getDeviceInfo().getDeviceName() + "'");
            }
        }
    }


    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProtoShared -> {
            SparkplugMessageType messageType = DCMD;
            TransportProtos.TsKvProto tsKvProto = tsKvProtoShared;
            if ("JSON_V".equals(tsKvProtoShared.getKv().getType().name())) {
                try {
                    messageType = SparkplugMessageType.parseMessageType(tsKvProtoShared.getKv().getKey());
                    tsKvProto = getTsKvProtoFromJsonNode(JacksonUtil.toJsonNode(tsKvProtoShared.getKv().getJsonV()), tsKvProtoShared.getTs());
                } catch (ThingsboardException e) {
                    messageType = null;
                    log.error("Failed attributes update notification to sparkplug device [{}]. ", deviceInfo.getDeviceName(), e);
                }
            }
            if (messageType != null && messageType.isSubscribe() && messageType.isDevice()
                    && getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        messageType, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                                sparkplugTopic.toString(),
                                getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            } else {
                log.trace("Failed attributes update notification to sparkplug device [{}]. ", deviceInfo.getDeviceName());
            }
        });
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
            if (messageType.isSubscribe()) {
                SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
                header.setMessageType(messageType.name());
                TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
                if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                    SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                            messageType, deviceInfo.getDeviceName());
                    parent.createSparkplugMqttPublishMsg(tsKvProto,
                                    sparkplugTopic.toString(),
                                    getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                            .ifPresent(payload -> parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo));
                } else {
                    parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                            ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                                    rpcRequest.getMethodName() + ". This device does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
                }
            }
        } catch (ThingsboardException e) {
            parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                            rpcRequest.getMethodName() + ". " + e.getMessage());
        }
    }

}
