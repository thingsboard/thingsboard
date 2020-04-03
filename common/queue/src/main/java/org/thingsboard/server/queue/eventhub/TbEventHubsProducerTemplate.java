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
import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class TbEventHubsProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbEventHubsSettings eventHubsSettings;
    private final Map<String, EventHubClient> clients = new HashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService clientsExecutor = Executors.newSingleThreadScheduledExecutor();

    public TbEventHubsProducerTemplate(TbQueueAdmin admin, TbEventHubsSettings eventHubsSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.eventHubsSettings = eventHubsSettings;
    }

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        byte[] payloadBytes = gson.toJson(new DefaultTbQueueMsg(msg)).getBytes();
        EventData sendEvent = EventData.create(payloadBytes);
        CompletableFuture<Void> send = getClient(tpi.getFullTopicName()).send(sendEvent);
        send.whenCompleteAsync((success, err) -> {
            if (err != null) {
                callback.onFailure(err);
            } else {
                callback.onSuccess(null);
            }
        }, executorService);
    }

    @Override
    public void stop() {
        executorService.shutdownNow();
        clients.forEach((t, client) -> client.close());
        clientsExecutor.shutdownNow();
    }

    private EventHubClient getClient(String topic) {
        return clients.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                    .setNamespaceName(eventHubsSettings.getNamespaceName())
                    .setEventHubName(topic)
                    .setSasKeyName(eventHubsSettings.getSasKeyName())
                    .setSasKey(eventHubsSettings.getSasKey());
            try {
                return EventHubClient.createFromConnectionStringSync(connStr.toString(), Executors.newSingleThreadScheduledExecutor());
            } catch (EventHubException | IOException e) {
                log.error("Failed to create EventHubClient with connection string: [{}]", connStr.toString(), e);
                throw new RuntimeException("Failed to create EventHubClient.", e);
            }
        });
    }
}
