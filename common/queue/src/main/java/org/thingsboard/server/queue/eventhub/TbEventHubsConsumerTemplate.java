/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.eventhub;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import com.microsoft.azure.eventhubs.EventPosition;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class TbEventHubsConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbEventHubsSettings eventHubsSettings;
    private Set<CustomClient> clients;

    private volatile Set<TopicPartitionInfo> partitions;
    private volatile boolean subscribed;
    private volatile boolean stopped = false;

    public TbEventHubsConsumerTemplate(TbQueueAdmin admin, TbEventHubsSettings eventHubsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.decoder = decoder;
        this.topic = topic;
        this.eventHubsSettings = eventHubsSettings;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
        subscribed = false;
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = false;
    }

    @Override
    public void unsubscribe() {
        stopped = true;

    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed && partitions == null) {
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                log.debug("Failed to await subscription", e);
            }
        } else {
            if (!subscribed) {
                clients = partitions.stream()
                        .map(TopicPartitionInfo::getFullTopicName)
                        .map(topic -> {
                            admin.createTopicIfNotExists(topic);
                            return new CustomClient(topic);
                        }).collect(Collectors.toSet());
                subscribed = true;
            }

            List<CompletableFuture<Iterable<EventData>>> futureList = clients.stream().map(client -> client.receiver.receive(10)).collect(Collectors.toList());

            CompletableFuture<Iterable<EventData>>[] arrayFuture = new CompletableFuture[futureList.size()];
            futureList.toArray(arrayFuture);

            try {
                List<Iterable<EventData>> eventsList =
                        CompletableFuture
                                .allOf(arrayFuture)
                                .thenApply(v -> futureList
                                        .stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList()))
                                .get();

                List<T> result = new ArrayList<>();

                eventsList.forEach(events -> {
                    if (events != null) {
                        events.forEach(eventData -> {
                            try {
                                result.add(decode(eventData));
                            } catch (InvalidProtocolBufferException e) {
                                log.error("Failed to decode eventData: [{}]", eventData);
                            }
                        });
                    }
                });

                return result;
            } catch (InterruptedException | ExecutionException e) {
                if (stopped) {
                    log.info("[{}] Event hubs consumer is stopped.", topic);
                } else {
                    log.error("Failed to receive messages", e);
                }
            }

        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {

    }

    private T decode(EventData data) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(data.getBytes()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

    private EventHubClient createEventHubClient(String eventHubName, ScheduledExecutorService executorService) {
        final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                .setNamespaceName(eventHubsSettings.getNamespaceName())
                .setEventHubName(eventHubName)
                .setSasKeyName(eventHubsSettings.getSasKeyName())
                .setSasKey(eventHubsSettings.getSasKey());
        try {
            return EventHubClient.createFromConnectionStringSync(connStr.toString(), executorService);
        } catch (EventHubException | IOException e) {
            log.error("Failed to create EventHubClient with connection string: [{}]", connStr.toString(), e);
            throw new RuntimeException("Failed to create EventHubClient.", e);
        }
    }

    @Data
    private class CustomClient {
        private final EventHubClient client;
        private final PartitionReceiver receiver;
        private final ScheduledExecutorService executorService;
        private static final String PARTITION = "0";

        public CustomClient(String eventHubName) {
            this.executorService = Executors.newSingleThreadScheduledExecutor();
            this.client = createEventHubClient(eventHubName, executorService);

            try {
                this.receiver = client.createReceiverSync(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, PARTITION, EventPosition.fromEndOfStream());
            } catch (EventHubException e) {
                log.error("Failed to create Event Hubs Receiver.", e);
                throw new RuntimeException("Failed to create Event Hubs Receiver.", e);
            }
        }

        public void close() {
            if (receiver != null) {
                receiver.close();
            }
            if (client != null) {
                client.close();
            }
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }
}
