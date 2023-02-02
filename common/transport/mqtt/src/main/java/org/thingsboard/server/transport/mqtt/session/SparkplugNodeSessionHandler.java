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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Descriptors;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.fromSparkplugBMetricToKeyValueProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.validatedValueByTypeMetric;

/**
 * Created by nickAS21 on 12.12.22
 */
@Slf4j
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler {

    private final SparkplugTopic sparkplugTopicNode;
    private final Map<String, SparkplugBProto.Payload.Metric> metricsBirthNode;

    public SparkplugNodeSessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId,
                                       SparkplugTopic sparkplugTopicNode) {
        super(deviceSessionCtx, sessionId);
        this.sparkplugTopicNode = sparkplugTopicNode;
        this.metricsBirthNode = new ConcurrentHashMap<>();
    }

    public void setMetricsBirthNode(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.metricsBirthNode.putAll(metrics.stream()
                .collect(Collectors.toMap(metric -> metric.getName(), metric -> metric)));
    }

    public Map<String, SparkplugBProto.Payload.Metric> getMetricsBirthNode() {
        return this.metricsBirthNode;
    }

    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        byte[] bytes = getBytes(inbound.payload());
        Descriptors.Descriptor telemetryDynamicMsgDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getTelemetryDynamicMsgDescriptor());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, telemetryDynamicMsgDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    public void onTelemetryProto (int msgId, SparkplugBProto.Payload sparkplugBProto, String deviceName, SparkplugTopic topic) throws AdaptorException, ThingsboardException {
        checkDeviceName(deviceName);
        ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture = topic.isNode() ?
                    Futures.immediateFuture(this.deviceSessionCtx) : onDeviceConnectProto(deviceName);
        List<TransportProtos.PostTelemetryMsg> msgs = convertToPostTelemetry(sparkplugBProto, topic.getType().name());
        if (topic.isType(NBIRTH) || topic.isType(DBIRTH)) {
            try {
                contextListenableFuture.get().setMetricsBirthDevice(sparkplugBProto.getMetricsList());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed add Metrics. MessageType *BIRTH.", e);
            }
        }
        onDeviceTelemetryProto(contextListenableFuture, msgId, msgs, deviceName);
    }

    public void onDeviceTelemetryProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture,
                                       int msgId, List<TransportProtos.PostTelemetryMsg> msgs, String deviceName) throws AdaptorException {
        try {
            int finalMsgId = msgId;
            for (TransportProtos.PostTelemetryMsg msg : msgs) {
                Futures.addCallback(contextListenableFuture,
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                try {
                                    processPostTelemetryMsg(deviceCtx, msg, deviceName, finalMsgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, msg, e);
                                    channel.close();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    public void onDeviceDisconnect(MqttPublishMessage mqttMsg, String deviceName) throws AdaptorException {
        try {
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    private ListenableFuture<MqttDeviceAwareSessionContext> onDeviceConnectProto(String deviceName) throws ThingsboardException {
        try {
            String deviceType =  this.gateway.getDeviceType() + "-node";
            return onDeviceConnect(deviceName, deviceType);
        } catch (RuntimeException e) {
            log.error("Failed Sparkplug Device connect proto!", e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private List<TransportProtos.PostTelemetryMsg> convertToPostTelemetry(SparkplugBProto.Payload sparkplugBProto, String topicTypeName) throws AdaptorException {
        try {
            List<TransportProtos.PostTelemetryMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                long ts = protoMetric.getTimestamp();
                String keys = "bdSeq".equals(protoMetric.getName()) ?
                        topicTypeName + " " + protoMetric.getName() : protoMetric.getName();
                Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(keys, protoMetric);
                if (keyValueProtoOpt.isPresent()) {
                    List<TransportProtos.KeyValueProto> result = new ArrayList<>();
                    result.add(keyValueProtoOpt.get());
                    TransportProtos.PostTelemetryMsg.Builder request = TransportProtos.PostTelemetryMsg.newBuilder();
                    TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
                    builder.setTs(ts);
                    builder.addAllKv(result);
                    request.addTsKvList(builder.build());
                    msgs.add(request.build());
                }
            }
            if (DBIRTH.name().equals(topicTypeName)) {
                List<TransportProtos.KeyValueProto> result = new ArrayList<>();
                TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
                keyValueProtoBuilder.setKey(topicTypeName + " " + "seq");
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV(sparkplugBProto.getSeq());
                result.add(keyValueProtoBuilder.build());
                TransportProtos.PostTelemetryMsg.Builder request = TransportProtos.PostTelemetryMsg.newBuilder();
                TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
                builder.setTs(sparkplugBProto.getTimestamp());
                builder.addAllKv(result);
                request.addTsKvList(builder.build());
                msgs.add(request.build());
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    public SparkplugTopic getSparkplugTopicNode() {
        return this.sparkplugTopicNode;
    }


    public Optional<MqttPublishMessage> createMqttPublishMsg (MqttDeviceAwareSessionContext ctx,
                                                              TransportProtos.AttributeUpdateNotificationMsg notification,
                                                              String... deviceName) {
        try {
            long ts = notification.getSharedUpdated(0).getTs();
            String key = notification.getSharedUpdated(0).getKv().getKey();
            if (metricsBirthNode.containsKey(key)) {
                SparkplugBProto.Payload.Metric metricBirth = metricsBirthNode.get(key);
                MetricDataType metricDataType = MetricDataType.fromInteger(metricBirth.getDatatype());
                Optional value = validatedValueByTypeMetric(notification.getSharedUpdated(0).getKv(), metricDataType);
                if (value.isPresent()) {
                    SparkplugBProto.Payload.Builder cmdPayload = SparkplugBProto.Payload.newBuilder()
                            .setTimestamp(ts);
                    cmdPayload.addMetrics(createMetric(value, ts, key, metricDataType));
                    byte[] payloadInBytes = cmdPayload.build().toByteArray();
                    String topic = deviceName == null ? sparkplugTopicNode.toString() : sparkplugTopicNode.toString()
                            + "/" + deviceName;
                    return Optional.of(getPayloadAdaptor().createMqttPublishMsg(ctx, topic, payloadInBytes));
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT sparkplug  msg", sessionId, e);
            return Optional.empty();
        }
        return Optional.empty();
    }

}
