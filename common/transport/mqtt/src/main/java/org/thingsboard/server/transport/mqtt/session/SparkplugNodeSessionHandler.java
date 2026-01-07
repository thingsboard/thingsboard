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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SpecVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.parseMessageType;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.SPARKPLUG_BD_SEQUENCE_NUMBER_KEY;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.SPARKPLUG_SEQUENCE_NUMBER_KEY;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.fromSparkplugBMetricToKeyValueProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.validatedValueByTypeMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_SPLIT_REGEXP;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.TOPIC_STATE_REGEXP;

@Slf4j
@SpecVersion(spec = "sparkplug", version = "3.0.0")
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler<SparkplugDeviceSessionContext> {

    @Getter
    private final SparkplugTopic sparkplugTopicNode;
    @Getter
    private final Map<String, SparkplugBProto.Payload.Metric> nodeBirthMetrics;
    @Getter
    private final Map<Long, String> nodeAlias;
    private final MqttTransportHandler parent;

    public SparkplugNodeSessionHandler(MqttTransportHandler parent, DeviceSessionCtx deviceSessionCtx, UUID sessionId,
                                       boolean overwriteDevicesActivity, SparkplugTopic sparkplugTopicNode) {
        super(deviceSessionCtx, sessionId, overwriteDevicesActivity);
        this.parent = parent;
        this.sparkplugTopicNode = sparkplugTopicNode;
        this.nodeBirthMetrics = new ConcurrentHashMap<>();
        this.nodeAlias = new ConcurrentHashMap<>();
    }

    public void setNodeBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) throws AdaptorException {
        for (var metric : metrics) {
            if (metric.hasName()) {
                this.nodeBirthMetrics.put(metric.getName(), metric);
            } else {
                throw new AdaptorException("The metric name of edgeNode: '" + this.sparkplugTopicNode.getEdgeNodeId() + "' must not be empty or null! Metric: [" + metric + "]");
            }
            if (metric.hasAlias() && this.nodeAlias.putIfAbsent(metric.getAlias(), metric.getName()) != null) {
                throw new AdaptorException("The alias '" + metric.getAlias() + "' already exists in edgeNode: '" + this.sparkplugTopicNode.getEdgeNodeId() + "'");
            }
        }
    }


    public boolean onValidateNDEATH(SparkplugBProto.Payload sparkplugBProto) throws ThingsboardException {
        return sparkplugBProto.getMetricsCount() == 1 && SPARKPLUG_BD_SEQUENCE_NUMBER_KEY.equals(sparkplugBProto.getMetrics(0).getName())
                && this.nodeBirthMetrics.get(SPARKPLUG_BD_SEQUENCE_NUMBER_KEY) != null
                && sparkplugBProto.getMetrics(0).getLongValue() == this.nodeBirthMetrics.get(SPARKPLUG_BD_SEQUENCE_NUMBER_KEY).getLongValue();
    }

    public void onAttributesTelemetryProto(int msgId, SparkplugBProto.Payload sparkplugBProto, SparkplugTopic topic) throws AdaptorException, ThingsboardException {
        String deviceName = topic.getNodeDeviceName();
        checkDeviceName(deviceName);

        ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture;
        if (topic.isNode()) {
            if (topic.isType(NBIRTH)) {
                sendSparkplugStateOnTelemetry(this.deviceSessionCtx.getSessionInfo(), deviceName, ONLINE,
                        sparkplugBProto.getTimestamp());
                setNodeBirthMetrics(sparkplugBProto.getMetricsList());
            }
            contextListenableFuture = Futures.immediateFuture(this.deviceSessionCtx);
        } else {
            ListenableFuture<SparkplugDeviceSessionContext> deviceCtx = onDeviceConnectProto(topic);
            contextListenableFuture = Futures.transform(deviceCtx, ctx -> {
                if (topic.isType(DBIRTH)) {
                    sendSparkplugStateOnTelemetry(ctx.getSessionInfo(), deviceName, ONLINE,
                            sparkplugBProto.getTimestamp());
                    try {
                        ctx.setDeviceBirthMetrics(sparkplugBProto.getMetricsList());
                    } catch (IllegalArgumentException | DuplicateKeyException e) {
                            throw new RuntimeException(e);
                    }
                }
                return ctx;
            }, MoreExecutors.directExecutor());
        }
        Set<String> attributesMetricNames = ((MqttDeviceProfileTransportConfiguration) deviceSessionCtx
                .getDeviceProfile().getProfileData().getTransportConfiguration()).getSparkplugAttributesMetricNames();
        if (attributesMetricNames != null) {
            List<TransportApiProtos.AttributesMsg> attributesMsgList = convertToPostAttributes(sparkplugBProto, attributesMetricNames, deviceName);
            onDeviceAttributesProto(contextListenableFuture, msgId, attributesMsgList, deviceName);
        }
        List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList = convertToPostTelemetry(sparkplugBProto, attributesMetricNames, topic.getType().name());
        onDeviceTelemetryProto(contextListenableFuture, msgId, postTelemetryMsgList, deviceName);
    }

    public void onDeviceTelemetryProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture,
                                       int msgId, List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList, String deviceName) {
        if (CollectionUtils.isEmpty(postTelemetryMsgList)) {
            log.debug("[{}] Device telemetry list is empty for: [{}]", sessionId, gateway.getDeviceId());
        }

        AtomicInteger remaining = new AtomicInteger(postTelemetryMsgList.size());
        AtomicBoolean ackSent = new AtomicBoolean(false);

        process(contextListenableFuture, deviceCtx -> {
                    for (TransportProtos.PostTelemetryMsg telemetryMsg : postTelemetryMsgList) {
                        try {
                            processPostTelemetryMsg(deviceCtx, telemetryMsg, deviceName, msgId, remaining, ackSent);
                        } catch (Throwable e) {
                            log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, telemetryMsg, e);
                            ackOrClose(msgId, ackSent);
                        }
                    }
                },
                t -> processFailure(msgId, deviceName, "Failed to process device telemetry command", ackSent, t));
    }

    private void onDeviceAttributesProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture, int msgId,
                                         List<TransportApiProtos.AttributesMsg> attributesMsgList, String deviceName) throws AdaptorException {
        try {
            if (CollectionUtils.isEmpty(attributesMsgList)) {
                log.debug("[{}] Device attribute list is empty for: [{}]", sessionId, gateway.getDeviceId());
            }

            AtomicInteger remaining = new AtomicInteger(attributesMsgList.size());
            AtomicBoolean ackSent = new AtomicBoolean(false);

            process(contextListenableFuture, deviceCtx -> {
                        for (TransportApiProtos.AttributesMsg attributesMsg : attributesMsgList) {
                            TransportProtos.PostAttributeMsg kvListProto = attributesMsg.getMsg();
                            try {
                                TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto);
                                processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId, remaining, ackSent);
                            } catch (Throwable e) {
                                log.warn("[{}][{}] Failed to process device attributes command: {}", gateway.getDeviceId(), deviceName, kvListProto, e);
                                ackOrClose(msgId, ackSent);
                            }
                        }
                    },
                    t -> processFailure(msgId, deviceName, "Failed to process device attributes command", ackSent, t));
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * Subscribe: spBv1.0/STATE/my_primary_hos -> Implemented as status via checkSparkplugNodeSession
     * Subscribe: CMD/DATA -> Implemented  after connection: SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG/SUBSCRIBE_TO_RPC_ASYNC_MSG
     * @param subscription
     * @throws ThingsboardException
     */
    public void handleSparkplugSubscribeMsg(MqttTopicSubscription subscription) throws ThingsboardException {
        String topic = subscription.topicFilter();
        if (topic != null && topic.startsWith(TOPIC_STATE_REGEXP)) {
            log.trace("Subscribing on it’s own spBv1.0/STATE/[the Sparkplug Host Application] - Implemented as status via checkSparkplugNodeSession");
        } else if (this.validateTopicDataSubscribe(topic)) {
            // TODO if need subscription DATA
            log.trace("Subscribing on it’s own [" + topic + "] - Implemented as SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG/SUBSCRIBE_TO_RPC_ASYNC_MSG via checkSparkplugNode/DeviceSession");
        } else {
            log.trace("Failed to subscribe to the topic: [" + topic + "].");
        }
    }

    public void onDeviceDisconnect(MqttPublishMessage mqttMsg, String deviceName) throws AdaptorException {
        try {
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    private ListenableFuture<SparkplugDeviceSessionContext> onDeviceConnectProto(SparkplugTopic topic) throws
            ThingsboardException {
        try {
            String deviceType = this.gateway.getDeviceType() + " device";
            return onDeviceConnect(topic.getNodeDeviceName(), deviceType);
        } catch (RuntimeException e) {
            log.error("Failed Sparkplug Device connect proto!", e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * Sparkplug 3.0.0 -> 6.4.6. Metric
     * https://sparkplug.eclipse.org/specification/version/3.0/documents/sparkplug-specification-3.0.0.pdf#%5B%7B%22num%22%3A339%2C%22gen%22%3A0%7D%2C%7B%22name%22%3A%22XYZ%22%7D%2C0%2C455.52%2Cnull%5D
     * [tck-id-payloads-name-requirement] The name MUST be included with every metric unless aliases are being used. ◦ All UTF-8 characters are allowed in the metric name. However, special characters including but not limited to the following are discouraged: . , \ @ # $ % ^ & * ( ) [ ] { } | ! ` ~ : ; ' " < > ?. This is because many Sparkplug Host Applications may have issues handling them.
     * • alias (are optional and not required):
     * - This is an unsigned 64-bit integer representing an optional alias for a Sparkplug B payload.
     * - If aliases are used, the following rules apply:
     * --  [tck-id-payloads-alias-uniqueness] If supplied in an NBIRTH or BIRTH it MUST be a unique number across this Edge Node’s entire set of metrics.
     * --  no two metrics for the same Edge Node can have the same alias.
     * --  [tck-id-payloads-alias-birth-requirement] NBIRTH and DBIRTH messages MUST include both a metric name and alias.
     * --  [tck-id-payloads-alias-data-cmd-requirement] NDATA, DDATA, NCMD, and DCMD messages MUST only include an alias and the metric name MUST be excluded.
     * @param sparkplugBProto
     * @param attributesMetricNames
     * @param topicTypeName
     * @return
     * @throws AdaptorException
     */
    private List<TransportProtos.PostTelemetryMsg> convertToPostTelemetry(SparkplugBProto.Payload
                                                                                  sparkplugBProto, Set<String> attributesMetricNames, String topicTypeName) throws AdaptorException {
        try {
            List<TransportProtos.PostTelemetryMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                String metricName = protoMetric.hasName() ? protoMetric.getName() :  protoMetric.hasAlias() ? this.nodeAlias.get(protoMetric.getAlias()) : null;
                if (metricName == null) {
                    throw new ThingsboardException("Metric without metricName and alias.", ThingsboardErrorCode.INVALID_ARGUMENTS);
                } else {
                    if (attributesMetricNames == null || !matches(attributesMetricNames, metricName)) {
                        long ts = protoMetric.getTimestamp();
                        String key = SPARKPLUG_BD_SEQUENCE_NUMBER_KEY.equals(protoMetric.getName()) ?
                                topicTypeName + " " + protoMetric.getName() : protoMetric.getName();
                        Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(key, protoMetric);
                        keyValueProtoOpt.ifPresent(kvProto -> msgs.add(postTelemetryMsgCreated(kvProto, ts)));
                    }
                }
            }

            if (DBIRTH.name().equals(topicTypeName)) {
                TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
                keyValueProtoBuilder.setKey(topicTypeName + " " + SPARKPLUG_SEQUENCE_NUMBER_KEY);
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV(sparkplugBProto.getSeq());
                msgs.add(postTelemetryMsgCreated(keyValueProtoBuilder.build(), sparkplugBProto.getTimestamp()));
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    private List<TransportApiProtos.AttributesMsg> convertToPostAttributes(SparkplugBProto.Payload sparkplugBProto,
                                                                           Set<String> attributesMetricNames,
                                                                           String deviceName) throws AdaptorException {
        try {
            List<TransportApiProtos.AttributesMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                String metricName = protoMetric.hasName() ? protoMetric.getName() :  protoMetric.hasAlias() ? this.nodeAlias.get(protoMetric.getAlias()) : null;
                if (metricName == null) {
                    throw new ThingsboardException("Metric without metricName and alias.", ThingsboardErrorCode.INVALID_ARGUMENTS);
                } else {
                    if (matches(attributesMetricNames, metricName)) {
                        TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
                        Optional<TransportProtos.PostAttributeMsg> msgOpt = getPostAttributeMsg(protoMetric);
                        if (msgOpt.isPresent()) {
                            deviceAttributesMsgBuilder.setDeviceName(deviceName);
                            deviceAttributesMsgBuilder.setMsg(msgOpt.get());
                            msgs.add(deviceAttributesMsgBuilder.build());
                        }
                    }
                }
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    private boolean matches(Set<String> attributesMetricNames, String metricName) {
        for (String attributeMetricFilter : attributesMetricNames) {
            if (metricName.equals(attributeMetricFilter) ||
                    (attributeMetricFilter.endsWith("*") && metricName.startsWith(
                            attributeMetricFilter.substring(0, attributeMetricFilter.length() - 1)))) {
                return true;
            }
        }
        return false;
    }

    private Optional<TransportProtos.PostAttributeMsg> getPostAttributeMsg(SparkplugBProto.Payload.Metric
                                                                                   protoMetric) throws ThingsboardException {
        Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(protoMetric.getName(), protoMetric);
        if (keyValueProtoOpt.isPresent()) {
            TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
            builder.addKv(keyValueProtoOpt.get());
            builder.setShared(true);
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    public Optional<MqttPublishMessage> createSparkplugMqttPublishMsg(TransportProtos.TsKvProto tsKvProto,
                                                                      String sparkplugTopic,
                                                                      SparkplugBProto.Payload.Metric metricBirth) {
        try {
            long ts = tsKvProto.getTs();
            MetricDataType metricDataType = MetricDataType.fromInteger(metricBirth.getDatatype());
            Optional value = validatedValueByTypeMetric(tsKvProto.getKv(), metricDataType);
            if (value.isPresent()) {
                SparkplugBProto.Payload.Builder cmdPayload = SparkplugBProto.Payload.newBuilder()
                        .setTimestamp(ts);
                String metricName = tsKvProto.getKv().getKey();
                Long alias = metricBirth.hasAlias() ? metricBirth.getAlias() : -1;
                cmdPayload.addMetrics(createMetric(value.get(), ts, alias == -1 ? metricName : null, metricDataType, alias));
                byte[] payloadInBytes = cmdPayload.build().toByteArray();
                return Optional.of(getPayloadAdaptor().createMqttPublishMsg(deviceSessionCtx, sparkplugTopic, payloadInBytes));
            } else {
                log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes [{}] response to MQTT sparkplug  msg",
                        deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, tsKvProto.getKv());
            }
        } catch (Exception e) {
            log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes response to MQTT sparkplug  msg",
                    deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, e);
        }
        return Optional.empty();
    }

    @Override
    protected SparkplugDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
        return new SparkplugDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

    protected void sendToDeviceRpcRequest(MqttMessage payload, TransportProtos.ToDeviceRpcRequestMsg
            rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo);
    }

    protected void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo,
                                        int requestId, ThingsboardErrorCode result, String errorMsg) {
        parent.sendErrorRpcResponse(sessionInfo, requestId, result, errorMsg);
    }

    /**
     * Subscribe: spBv1.0/G1/DDATA/E1
     * Subscribe: spBv1.0/G1/DDATA/E1/#
     * Subscribe: spBv1.0/G1/DDATA/E1/+
     * Subscribe: spBv1.0/G1/DDATA/E1/D1
     * Subscribe: spBv1.0/G1/DDATA/E1/D1/#
     * Subscribe: spBv1.0/G1/DDATA/E1/D1/+
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     * @param topic a topic  UTF-8
     * @return a {@link SparkplugTopic} instance
     * @throws ThingsboardException if an error occurs while parsing
     */
    public boolean validateTopicDataSubscribe(String topic) throws ThingsboardException {
        String[] splitTopic = topic.split(TOPIC_SPLIT_REGEXP);
        if (splitTopic.length >= 4 && splitTopic.length <= 5 &&
                splitTopic[0].equals(this.sparkplugTopicNode.getNamespace()) &&
                splitTopic[1].equals(this.sparkplugTopicNode.getGroupId()) &&
                splitTopic[3].equals(this.sparkplugTopicNode.getEdgeNodeId())) {
            SparkplugMessageType messageType = parseMessageType(splitTopic[2]);
            return messageType.isData();
        }
        return false;
    }
}
