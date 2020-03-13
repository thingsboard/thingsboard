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
package org.thingsboard.server.kafka;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.thingsboard.server.TbQueueAdmin;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.09.18.
 */
public class TBKafkaAdmin implements TbQueueAdmin {

    AdminClient client;

    public TBKafkaAdmin(TbKafkaSettings settings) {
        client = AdminClient.create(settings.toProps());
    }

    @Override
    public ListenableFuture<Void> createTopicIfNotExists(String topic) {

        KafkaFuture<TopicDescription> topicDescriptionFuture = client.describeTopics(Collections.singleton(topic)).values().get(topic);

        ListenableFuture<TopicDescription> topicFuture = JdkFutureAdapters.listenInPoolThread(topicDescriptionFuture);

        return Futures.transformAsync(topicFuture, topicDescription -> {
            KafkaFuture<Void> resultFuture = createTopic(new NewTopic(topic, 1, (short) 1)).values().get(topic);
            return JdkFutureAdapters.listenInPoolThread(resultFuture);
        });
    }

    public void waitForTopic(String topic, long timeout, TimeUnit timeoutUnit) throws InterruptedException, TimeoutException {
        synchronized (this) {
            long timeoutExpiredMs = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
            while (!topicExists(topic)) {
                long waitMs = timeoutExpiredMs - System.currentTimeMillis();
                if (waitMs <= 0) {
                    throw new TimeoutException("Timeout occurred while waiting for topic [" + topic + "] to be available!");
                } else {
                    wait(1000);
                }
            }
        }
    }

    public CreateTopicsResult createTopic(NewTopic topic) {
        return client.createTopics(Collections.singletonList(topic));
    }

    private boolean topicExists(String topic) throws InterruptedException {
        KafkaFuture<TopicDescription> topicDescriptionFuture = client.describeTopics(Collections.singleton(topic)).values().get(topic);
        try {
            topicDescriptionFuture.get();
            return true;
        } catch (ExecutionException e) {
            return false;
        }
    }
}
