// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class AzureServiceBusIntegration extends AbstractIntegration<AzureServiceBusIntegrationMsg> {

    private AzureServiceBusClientConfiguration clientConfiguration;
    private ServiceBusSenderClient senderClient;
    private ServiceBusProcessorClient receiverClient;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        clientConfiguration = getClientConfiguration(configuration, AzureServiceBusClientConfiguration.class);

        initReceiver(clientConfiguration);

        if (downlinkConverter != null) {
            senderClient = initSenderClient(clientConfiguration);
        }
    }

    @Override
    public void destroy() {
        if (senderClient != null) {
            senderClient.close();
        }
        if (receiverClient != null) {
            receiverClient.close();
        }
    }

    @Override
    public void process(AzureServiceBusIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(msg.toJson()), status, exception);
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    @Override
    public void checkConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        var configuration = getClientConfiguration(
                integration.getConfiguration().get("clientConfiguration"),
                AzureServiceBusClientConfiguration.class
        );

        try (var _ = buildConsumerClient(configuration)) {
            log.debug("Service bus consumer connection checked");
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    private void doProcess(IntegrationContext context, AzureServiceBusIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        msg.getSystemProperties().forEach(
                (key, value) -> {
                    if (value != null) {
                        mdMap.put("sysProp:" + key, value.toString());
                    }
                }
        );
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkDataBlocking(context, data);
                log.trace("[{}] Processing uplink data", data);
            }
        }
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.debug("Processing message. Session: [{}], Sequence #: [{}]. Contents: [{}]", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
        process(new AzureServiceBusIntegrationMsg(context));
    }

    private boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        if (senderClient == null) {
            return false;
        }
        Map<String, List<ServiceBusMessage>> deviceIdToMessage = convertDownLinkMsg(context, msg);
        ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();
        for (Map.Entry<String, List<ServiceBusMessage>> messageEntry : deviceIdToMessage.entrySet()) {
            for (ServiceBusMessage message : messageEntry.getValue()) {
                logServiceBusDownlink(context, message, messageEntry.getKey(), message.getContentType());
                if (messageBatch.tryAddMessage(message)) {
                    continue;
                }

                senderClient.sendMessages(messageBatch);
                log.debug("Sent a batch of messages to the queue [{}]", clientConfiguration.getDownlinkTopicName());

                messageBatch = senderClient.createMessageBatch();
                if (!messageBatch.tryAddMessage(message)) {
                    log.error("Message is too large for an empty batch. Skipping. Max size: [{}].", messageBatch.getMaxSizeInBytes());
                }
                log.debug("[{}][{}] Sent downlink [{}] successfully.", configuration.getId(), message.getTo(), message.getMessageId());
            }
        }
        if (messageBatch.getCount() > 0) {
            senderClient.sendMessages(messageBatch);
            log.debug("Sent a batch of messages to the queue [{}]", clientConfiguration.getDownlinkTopicName());
        }
        return !deviceIdToMessage.isEmpty();
    }

    private Map<String, List<ServiceBusMessage>> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, List<ServiceBusMessage>> deviceIdToMessage = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String deviceId = data.getMetadata().get("deviceId");
                if (StringUtils.isEmpty(deviceId)) {
                    continue;
                }
                ServiceBusMessage message = new ServiceBusMessage(data.getData());
                message.setMessageId(UUID.randomUUID().toString());
                message.setTo(deviceId);
                message.getApplicationProperties().putAll(data.getMetadata());
                message.setContentType(data.getContentType());
                deviceIdToMessage.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(message);
            }
        }
        return deviceIdToMessage;
    }

    private void initReceiver(AzureServiceBusClientConfiguration configuration) {
        receiverClient = buildConsumerClient(configuration);
        receiverClient.start();
    }

    private ServiceBusProcessorClient buildConsumerClient(AzureServiceBusClientConfiguration configuration) {
        return new ServiceBusClientBuilder()
                .connectionString(configuration.getConnectionString())
                .processor()
                .topicName(configuration.getTopicName())
                .subscriptionName(configuration.getSubName())
                .processMessage(this::processMessage)
                .processError(error -> log.error("It was trouble when receiving: " + error.getException().getMessage()))
                .buildProcessorClient();
    }

    private ServiceBusSenderClient initSenderClient(AzureServiceBusClientConfiguration clientConfiguration) throws Exception {
        return new ServiceBusClientBuilder()
                .connectionString(clientConfiguration.getDownlinkConnectionString())
                .sender()
                .topicName(clientConfiguration.getDownlinkTopicName())
                .buildClient();
    }

    private void logServiceBusDownlink(IntegrationContext context, ServiceBusMessage message, String deviceId, String contentType) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            json.put("deviceId", deviceId);
            json.set("payload", getDownlinkPayloadJson(message, contentType));
            json.set("properties", JacksonUtil.valueToTree(message.getApplicationProperties()));
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

    private JsonNode getDownlinkPayloadJson(ServiceBusMessage message, String contentType) {
        if ("JSON".equals(contentType)) {
            return JacksonUtil.fromBytes(message.getBody().toBytes());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(message.getBody().toBytes(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64.getEncoder().encodeToString(message.getBody().toBytes()));
        }
    }

}
