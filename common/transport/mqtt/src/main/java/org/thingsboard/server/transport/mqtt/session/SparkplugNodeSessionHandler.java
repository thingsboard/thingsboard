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
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.adaptor.ProtoConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getFromSparkplugBMetricToKeyValueProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopicSubscribe;

/**
 * Created by nickAS21 on 12.12.22
 */
@Slf4j
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler {

    public SparkplugNodeSessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId) {
        super(deviceSessionCtx, sessionId);
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

    public void onDeviceTelemetryProto(int msgId, SparkplugBProto.Payload sparkplugBProto, String deviceName, String topicTypeName, boolean isNode) throws AdaptorException {
        try {
            checkDeviceName(deviceName);
            List<TransportProtos.PostTelemetryMsg> msgs = convertToPostTelemetry(sparkplugBProto, topicTypeName);
            int finalMsgId = msgId;
            ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture = isNode ?
                    Futures.immediateFuture(this.deviceSessionCtx) : checkDeviceConnected(deviceName);
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

    public void handleSparkplugSubscribeMsg(List<Integer> grantedQoSList, SparkplugTopic sparkplugTopic, MqttQoS reqQoS) {
        if (sparkplugTopic.getGroupId() == null) {
            // TODO SUBSCRIBE NameSpace
        } else if (sparkplugTopic.getType() == null) {
            // TODO SUBSCRIBE GroupId
        } else if (sparkplugTopic.isNode()) {
            // A node topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case NBIRTH:
                    // TODO
                    break;
                case NCMD:
                    // TODO
                    break;
                case NDATA:
                    // TODO
                    break;
                case NDEATH:
                    // TODO
                    break;
                case NRECORD:
                    // TODO
                    break;
                default:
            }
        } else {
            // A device topic
            switch (sparkplugTopic.getType()) {
                case STATE:
                    // TODO
                    break;
                case DBIRTH:
                    // TODO
                    break;
                case DCMD:
                    // TODO
                    break;
                case DDATA:
                    // TODO
                    break;
                case DDEATH:
                    // TODO
                    break;
                case DRECORD:
                    // TODO
                    break;
                default:
            }
        }
    }

    private List<TransportProtos.PostTelemetryMsg> convertToPostTelemetry(SparkplugBProto.Payload sparkplugBProto, String topicTypeName) throws AdaptorException {
        try {
            List<TransportProtos.PostTelemetryMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                long ts = protoMetric.getTimestamp();
                String keys = "bdSeq".equals(protoMetric.getName()) ?
                        topicTypeName + " " + protoMetric.getName() : protoMetric.getName();
                Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = getFromSparkplugBMetricToKeyValueProto(keys, protoMetric);
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

    public void onDeviceConnectProto(MqttPublishMessage mqttPublishMessage, String nodeDeviceType) throws ThingsboardException {
        try {
            String topic = mqttPublishMessage.variableHeader().topicName();
            SparkplugTopic sparkplugTopic = parseTopicSubscribe(topic);
            String deviceName = checkDeviceName(sparkplugTopic.getDeviceId());
            String deviceType = StringUtils.isEmpty(nodeDeviceType) ? DEFAULT_DEVICE_TYPE : nodeDeviceType;
            processOnConnect(mqttPublishMessage, deviceName, deviceType);
        } catch (RuntimeException | ThingsboardException e) {
            log.error("Failed Sparkplug Device connect proto!", e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

}
