// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.basic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.mqtt.AbstractMqttIntegration;
import org.thingsboard.integration.mqtt.BasicMqttIntegrationMsg;
import org.thingsboard.integration.mqtt.MqttClientConfiguration;
import org.thingsboard.integration.mqtt.MqttTopicFilter;
import org.thingsboard.mqtt.MqttClientCallback;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class BasicMqttIntegration extends AbstractMqttIntegration<BasicMqttIntegrationMsg> {

    protected String downlinkTopicPattern = "${topic}";

    private static final String DEFAULT_DOWNLINK_TOPIC_PATTERN = "${topic}";

    private volatile WeakReference<ListenableFuture<?>> subscribeFuture = new WeakReference<>(Futures.immediateVoidFuture());

    String getOwnerId(Integration configuration) {
        return "Tenant[" + configuration.getTenantId().getId() + "]Integration[" + configuration.getId().getId() + "]";
    }

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            return;
        }
        log.debug("[{}][{}] MQTT Integration initializing MQTT client", configuration.getId(), configuration.getName());

        mqttClient = initClient(
                getOwnerId(this.configuration),
                mqttClientConfiguration,
                (topic, data) -> processAsync(new BasicMqttIntegrationMsg(topic, data)),
                getRetransmissionConfig(context)
        );

        subscribeToTopics();

        this.downlinkTopicPattern = getDownlinkTopicPattern();
        this.mqttClient.setCallback(new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.info("[{}][{}] MQTT Integration lost connection to the target broker", configuration.getId(), configuration.getName());
            }

            @Override
            public void onSuccessfulReconnect() {
                log.info("[{}][{}] MQTT Integration successfully reconnected to the target broker", configuration.getId(), configuration.getName());
                Optional.ofNullable(subscribeFuture.get()).ifPresent(f -> f.cancel(true));
                var future = mqttClient.getHandlerExecutor().submit(() -> {
                    try {
                        subscribeToTopics();
                    } catch (IOException e) {
                        log.info("[{}][{}] MQTT Integration failed to subscribe to topics", configuration.getId(), configuration.getName());
                    }
                });
                subscribeFuture = new WeakReference<>(future);
            }
        });
    }

    @Override
    public void doCheckConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        context = ctx;
        this.configuration = integration;
        try {
            mqttClientConfiguration = getClientConfiguration(configuration, MqttClientConfiguration.class);
            log.debug("mqttClientConfiguration from JSON: {}", mqttClientConfiguration);
            if (mqttClientConfiguration.getConnectTimeoutSec() > ctx.getIntegrationConnectTimeoutSec() && ctx.getIntegrationConnectTimeoutSec() > 0) {
                log.debug("Reduce connection timeout sec down to the limit [{}]", mqttClientConfiguration.getConnectTimeoutSec());
                mqttClientConfiguration.setConnectTimeoutSec(ctx.getIntegrationConnectTimeoutSec());
            }
            mqttClient = initClient(
                    getOwnerId(integration),
                    mqttClientConfiguration,
                    (topic, data) -> processAsync(new BasicMqttIntegrationMsg(topic, data)),
                    getRetransmissionConfig(context)
            );
        } catch (RuntimeException e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private static MqttClientConfig.RetransmissionConfig getRetransmissionConfig(IntegrationContext context) {
        return new MqttClientConfig.RetransmissionConfig(
                context.getMqttClientRetransmissionMaxAttempts(),
                context.getMqttClientRetransmissionInitialDelayMillis(),
                context.getMqttClientRetransmissionJitterFactor()
        );
    }

    private void subscribeToTopics() throws java.io.IOException {
        List<MqttTopicFilter> topics = getMqttTopicFilters(configuration);

        for (MqttTopicFilter topicFilter : topics) {
            mqttClient.on(topicFilter.getFilter(), (topic, data) ->
                    processAsync(new BasicMqttIntegrationMsg(topic, data)), MqttQoS.valueOf(topicFilter.getQos()));
        }
    }

    protected String getDownlinkTopicPattern() {
        String downlinkTopicPattern = null;
        if (configuration.getConfiguration().has("downlinkTopicPattern")) {
            downlinkTopicPattern = configuration.getConfiguration().get("downlinkTopicPattern").asText();
        }
        if (StringUtils.isEmpty(downlinkTopicPattern)) {
            downlinkTopicPattern = DEFAULT_DOWNLINK_TOPIC_PATTERN;
        }
        return downlinkTopicPattern;
    }

    @Override
    protected List<UplinkData> convertToUplinkDataList(IntegrationContext context, byte[] data, UplinkMetaData md) {
        throw new RuntimeException("MQTT integrations does not support blocking call on convertToUplinkDataList, use convertToUplinkDataListAsync instead");
    }

    @Override
    protected ListenableFuture<Void> doProcess(IntegrationContext context, BasicMqttIntegrationMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.put("topic", msg.getTopic());

        var stopWatch = TbStopWatch.create();
        ListenableFuture<List<UplinkData>> uplinkDataListFuture = convertToUplinkDataListAsync(context, msg.getPayload(), new UplinkMetaData<>(msg.getContentType(), mdMap));
        return Futures.transformAsync(uplinkDataListFuture, uplinkDataList -> {
            if (log.isDebugEnabled()) {
                log.debug("convertToUplinkDataList took {}ms for integration {}", stopWatch.stopAndGetTotalTimeMillis(), configuration.getName());
            }

            if (CollectionsUtil.isEmpty(uplinkDataList)) {
                return Futures.immediateFuture(null);
            }

            List<ListenableFuture<Void>> futures = uplinkDataList.stream().map(data -> processUplinkData(context, data)).toList();
            return Futures.transform(Futures.allAsList(futures), _ -> {
                log.debug("[{}] Successfully processed all uplink data to topic", configuration.getId());
                return null;
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, List<DownlinkData>> topicToDataMap = convertDownLinkMsg(context, msg);
        for (Map.Entry<String, List<DownlinkData>> topicEntry : topicToDataMap.entrySet()) {
            for (DownlinkData data : topicEntry.getValue()) {
                String topic = topicEntry.getKey();
                mqttClient.publish(topic, Unpooled.wrappedBuffer(data.getData()), MqttQoS.AT_LEAST_ONCE, mqttClientConfiguration.isRetainedMessage());
                logMqttDownlink(context, topic, data);
            }
        }
        return !topicToDataMap.isEmpty();
    }

    private Map<String, List<DownlinkData>> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, List<DownlinkData>> topicToDataMap = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String downlinkTopic = compileDownlinkTopic(data.getMetadata());
                topicToDataMap.computeIfAbsent(downlinkTopic, k -> new ArrayList<>()).add(data);
            }
        }
        return topicToDataMap;
    }

    private String compileDownlinkTopic(Map<String, String> md) {
        if (md != null) {
            String result = downlinkTopicPattern;
            for (Map.Entry<String, String> mdEntry : md.entrySet()) {
                String key = "${" + mdEntry.getKey() + "}";
                result = result.replace(key, mdEntry.getValue());
            }
            return result;
        }
        return downlinkTopicPattern;
    }

    private void logMqttDownlink(IntegrationContext context, String topic, DownlinkData data) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            json.put("topic", topic);
            json.set("payload", getDownlinkPayloadJson(data));
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

}
