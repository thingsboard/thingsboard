// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.amqp.implementation.ConnectionStringProperties;
import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.Message;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
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
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
public class AzureEventHubIntegration extends AbstractIntegration<AzureEventHubIntegrationMsg> {

    private ServiceClient serviceClient;
    private EventProcessorClient eventProcessorClient;
    private LocalCheckpointStore localCheckpointStore;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        AzureEventHubClientConfiguration clientConfiguration = getClientConfiguration(configuration, AzureEventHubClientConfiguration.class);

        eventProcessorClient = buildProcessorClient(clientConfiguration);
        eventProcessorClient.start();

        if (downlinkConverter != null) {
            serviceClient = initServiceClient(clientConfiguration);
        }
    }

    @Override
    public void destroy() {
        if (serviceClient != null) {
            serviceClient.closeAsync();
        }
        if (eventProcessorClient != null) {
            eventProcessorClient.stop();
        }
    }

    @Override
    public void process(AzureEventHubIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
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
    public void checkConnection(Integration integration, IntegrationContext ctx) {
        var configuration = getClientConfiguration(
                integration.getConfiguration().get("clientConfiguration"),
                AzureEventHubClientConfiguration.class
        );

        EventHubClientBuilder builder = new EventHubClientBuilder()
                .consumerGroup(configuration.getConsumerGroup())
                .connectionString(configuration.getConnectionString())
                .retry(new AmqpRetryOptions().setTryTimeout(Duration.ofSeconds(configuration.getConnectTimeoutSec())));
        try (var consumerClient = builder.buildAsyncConsumerClient()) {
            try {
                consumerClient.getPartitionIds().blockFirst();
            } catch (Exception e) {
                throw new RuntimeException("Unable to connect. Check for correct Connection String!", e);
            }
            try {
                if (configuration.isEnablePersistentCheckpoints()) {
                    buildContainerClient(configuration).exists().block();
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to connect. Check for correct Storage Connection String!", e);
            }
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

    private void doProcess(IntegrationContext context, AzureEventHubIntegrationMsg msg) throws Exception {
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

    private boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        if (serviceClient == null) {
            return false;
        }
        Map<String, List<Message>> deviceIdToMessage = convertDownLinkMsg(context, msg);
        for (Map.Entry<String, List<Message>> messageEntry : deviceIdToMessage.entrySet()) {
            for (Message message : messageEntry.getValue()) {
                logEventHubDownlink(context, message, messageEntry.getKey(), message.getProperties().get("content-type"));
                var future = serviceClient.sendAsync(messageEntry.getKey(), message);
                future.whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.debug("[{}][{}] Failed to send downlink [{}] due to: ", configuration.getId(), message.getTo(), message.getMessageId(), ex);
                    } else {
                        log.debug("[{}][{}] Sent downlink [{}] successfully.", configuration.getId(), message.getTo(), message.getMessageId());
                    }
                });
            }
        }
        return !deviceIdToMessage.isEmpty();
    }

    private Map<String, List<Message>> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, List<Message>> deviceIdToMessage = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String deviceId = data.getMetadata().get("deviceId");
                if (StringUtils.isEmpty(deviceId)) {
                    continue;
                }
                Message message = new Message(data.getData());
                message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
                message.setMessageId(UUID.randomUUID().toString());
                message.setTo(deviceId);
                message.getProperties().putAll(data.getMetadata());
                message.getProperties().put("content-type", data.getContentType());
                deviceIdToMessage.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(message);
            }
        }
        return deviceIdToMessage;
    }

    private EventProcessorClient buildProcessorClient(AzureEventHubClientConfiguration configuration) {
        return new EventProcessorClientBuilder()
                .consumerGroup(configuration.getConsumerGroup())
                .connectionString(configuration.getConnectionString())
                .retryOptions(new AmqpRetryOptions().setTryTimeout(Duration.ofSeconds(configuration.getConnectTimeoutSec())))
                .processEvent(eventContext -> {
                    process(new AzureEventHubIntegrationMsg(eventContext.getEventData()));
                    eventContext.updateCheckpoint();
                })
                .processError(error -> log.error("It was trouble when receiving: {}", error.getThrowable().getMessage()))
                .checkpointStore(buildCheckpointStore(configuration))
                .buildEventProcessorClient();
    }

    private CheckpointStore buildCheckpointStore(AzureEventHubClientConfiguration configuration) {
        CheckpointStore checkpointStore;
        if (configuration.isEnablePersistentCheckpoints()) {
            BlobContainerAsyncClient containerClient = buildContainerClient(configuration);
            containerClient.createIfNotExists().block();
            checkpointStore = new BlobCheckpointStore(containerClient);
        } else {
            checkpointStore = Objects.requireNonNullElseGet(localCheckpointStore, () -> localCheckpointStore = new LocalCheckpointStore());
        }
        return checkpointStore;
    }

    private BlobContainerAsyncClient buildContainerClient(AzureEventHubClientConfiguration configuration) {
        return new BlobContainerClientBuilder()
                .connectionString(configuration.getStorageConnectionString())
                .containerName(configuration.getContainerName())
                .buildAsyncClient();
    }

    private ServiceClient initServiceClient(AzureEventHubClientConfiguration clientConfiguration) throws Exception {
        if (StringUtils.isEmpty(clientConfiguration.getIotHubName())) {
            return null;
        }
        ConnectionStringProperties connectionStringProperties = new ConnectionStringProperties(clientConfiguration.getConnectionString());

        String iotHubConnectionString =
                String.format("HostName=%s.azure-devices.net;SharedAccessKeyName=%s;SharedAccessKey=%s", clientConfiguration.getIotHubName(),
                        connectionStringProperties.getSharedAccessKeyName(), connectionStringProperties.getSharedAccessKey());
        ServiceClient serviceClient = ServiceClient.createFromConnectionString(iotHubConnectionString, IotHubServiceClientProtocol.AMQPS);
        CompletableFuture<Void> serviceClientFuture = serviceClient.openAsync();
        try {
            serviceClientFuture.get(clientConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            serviceClientFuture.cancel(true);
            throw new RuntimeException(String.format("Failed to connect to the IoT Hub %s within specified timeout.",
                    clientConfiguration.getIotHubName()));
        }
        return serviceClient;
    }

    private void logEventHubDownlink(IntegrationContext context, Message message, String deviceId, String contentType) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            json.put("deviceId", deviceId);
            json.set("payload", getDownlinkPayloadJson(message, contentType));
            json.set("properties", JacksonUtil.valueToTree(message.getProperties()));
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

    private JsonNode getDownlinkPayloadJson(Message message, String contentType) {
        if ("JSON".equals(contentType)) {
            return JacksonUtil.fromBytes(message.getBytes());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(message.getBytes(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64.getEncoder().encodeToString(message.getBytes()));
        }
    }

    private static class LocalCheckpointStore implements CheckpointStore {

        private final Map<String, PartitionOwnership> ownershipMap = new ConcurrentHashMap<>();
        private final Map<String, Checkpoint> checkpointMap = new ConcurrentHashMap<>();

        @Override
        public Flux<PartitionOwnership> listOwnership(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
            return Flux.fromIterable(ownershipMap.values())
                    .filter(o -> matches(o, fullyQualifiedNamespace, eventHubName, consumerGroup));
        }

        @Override
        public Flux<PartitionOwnership> claimOwnership(List<PartitionOwnership> requestedPartitionOwnerships) {
            return Flux.fromIterable(requestedPartitionOwnerships).flatMap(partitionOwnership -> {
                String key = getKey(partitionOwnership);
                ownershipMap.compute(key, (k, existingOwnership) -> {
                    if (existingOwnership == null || existingOwnership.getETag() == null || existingOwnership.getETag().equals(partitionOwnership.getETag())) {
                        partitionOwnership.setETag(UUID.randomUUID().toString());
                        partitionOwnership.setLastModifiedTime(System.currentTimeMillis());
                        return partitionOwnership;
                    } else {
                        return existingOwnership;
                    }
                });

                return Mono.just(partitionOwnership);
            });
        }

        @Override
        public Flux<Checkpoint> listCheckpoints(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
            return Flux.fromIterable(checkpointMap.values())
                    .filter(checkpoint -> matches(checkpoint, fullyQualifiedNamespace, eventHubName, consumerGroup));
        }

        @Override
        public Mono<Void> updateCheckpoint(Checkpoint checkpoint) {
            return Mono.fromRunnable(() -> checkpointMap.put(getKey(checkpoint), checkpoint));
        }

        private boolean matches(Object item, String namespace, String eventHub, String consumerGroup) {
            if (item instanceof PartitionOwnership ownership) {
                return ownership.getFullyQualifiedNamespace().equals(namespace) &&
                        ownership.getEventHubName().equals(eventHub) &&
                        ownership.getConsumerGroup().equals(consumerGroup);
            } else if (item instanceof Checkpoint checkpoint) {
                return checkpoint.getFullyQualifiedNamespace().equals(namespace) &&
                        checkpoint.getEventHubName().equals(eventHub) &&
                        checkpoint.getConsumerGroup().equals(consumerGroup);
            }
            return false;
        }

        private String getKey(PartitionOwnership ownership) {
            return String.format("%s/%s/%s/%s",
                    ownership.getFullyQualifiedNamespace(),
                    ownership.getEventHubName(),
                    ownership.getConsumerGroup(),
                    ownership.getPartitionId()
            );
        }

        private String getKey(Checkpoint checkpoint) {
            return String.format("%s/%s/%s/%s",
                    checkpoint.getFullyQualifiedNamespace(),
                    checkpoint.getEventHubName(),
                    checkpoint.getConsumerGroup(),
                    checkpoint.getPartitionId()
            );
        }

    }

}
