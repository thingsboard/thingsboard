/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.queue.rabbitmq;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;
import org.thingsboard.server.queue.settings.TbRabbitMqSettings;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TbRabbitMqProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbRabbitMqSettings rabbitMqSettings;
    private final ListeningExecutorService producerExecutor;
    private final Channel channel;
    private final Connection connection;

    private final Set<TopicPartitionInfo> topics = ConcurrentHashMap.newKeySet();

    public TbRabbitMqProducerTemplate(TbQueueAdmin admin, TbRabbitMqSettings rabbitMqSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.rabbitMqSettings = rabbitMqSettings;
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }

        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
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
        createTopicIfNotExist(tpi);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();
        try {
            channel.basicPublish(rabbitMqSettings.getExchangeName(), tpi.getFullTopicName(), properties, gson.toJson(new DefaultTbQueueMsg(msg)).getBytes());
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (IOException e) {
            log.error("Failed publish message: [{}].", msg, e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    @Override
    public void stop() {
        if (producerExecutor != null) {
            producerExecutor.shutdownNow();
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close the channel.");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close the connection.");
            }
        }
    }

    private void createTopicIfNotExist(TopicPartitionInfo tpi) {
        if (topics.contains(tpi)) {
            return;
        }
        admin.createTopicIfNotExists(tpi.getFullTopicName());
        topics.add(tpi);
    }
}
