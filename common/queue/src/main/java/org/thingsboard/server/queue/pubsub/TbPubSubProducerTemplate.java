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
package org.thingsboard.server.queue.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TbPubSubProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final Gson gson = new Gson();

    private final String defaultTopic;
    private final TbQueueAdmin admin;
    private final TbPubSubSettings pubSubSettings;

    private final Map<String, Publisher> publisherMap = new ConcurrentHashMap<>();

    private final ExecutorService pubExecutor = Executors.newCachedThreadPool();

    public TbPubSubProducerTemplate(TbQueueAdmin admin, TbPubSubSettings pubSubSettings, String defaultTopic) {
        this.defaultTopic = defaultTopic;
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
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
        PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();
        pubsubMessageBuilder.setData(getMsg(msg));

        Publisher publisher = getOrCreatePublisher(tpi.getFullTopicName());
        ApiFuture<String> future = publisher.publish(pubsubMessageBuilder.build());

        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
            public void onSuccess(String messageId) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        }, pubExecutor);
    }

    @Override
    public void stop() {
        publisherMap.forEach((k, v) -> {
            if (v != null) {
                try {
                    v.shutdown();
                    v.awaitTermination(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Failed to shutdown PubSub client during destroy()", e);
                }
            }
        });

        if (pubExecutor != null) {
            pubExecutor.shutdownNow();
        }
    }

    private ByteString getMsg(T msg) {
        String json = gson.toJson(new DefaultTbQueueMsg(msg));
        return ByteString.copyFrom(json.getBytes());
    }

    private Publisher getOrCreatePublisher(String topic) {
        if (publisherMap.containsKey(topic)) {
            return publisherMap.get(topic);
        } else {
            try {
                admin.createTopicIfNotExists(topic);
                ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), topic);
                Publisher publisher = Publisher.newBuilder(topicName).setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
                publisherMap.put(topic, publisher);
                return publisher;
            } catch (IOException e) {
                log.error("Failed to create Publisher for the topic [{}].", topic, e);
                throw new RuntimeException("Failed to create Publisher for the topic.", e);
            }
        }

    }

}
