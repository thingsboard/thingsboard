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
package org.thingsboard.server.queue.pubsub;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
@Component
public class TbPubSubAdmin implements TbQueueAdmin {

    private final TbPubSubSettings pubSubSettings;
    private final SubscriptionAdminSettings subscriptionAdminSettings;
    private final TopicAdminSettings topicAdminSettings;
    private final Lock topicLock = new ReentrantLock();
    private final Lock subscriptionLock = new ReentrantLock();
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();

    public TbPubSubAdmin(TbPubSubSettings pubSubSettings) {
        this.pubSubSettings = pubSubSettings;

        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }
    }

    @Override
    public void createTopicIfNotExists(String partition) {
        String topicId = partition;
        String subscriptionId = partition;
        ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), topicId);

        if (topicSet.contains(topicId)) {
            createSubscriptionIfNotExists(subscriptionId, topicName);
            return;
        }

        topicLock.lock();
        if (topicSet.contains(topicId)) {
            createSubscriptionIfNotExists(subscriptionId, topicName);
            return;
        }

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            Iterable<Topic> topics = response.iterateAll();
            for (Topic topic : topics) {
                if (topic.getName().equals(topicName.toString())) {
                    topicSet.add(topicId);
                    createSubscriptionIfNotExists(subscriptionId, topicName);
                    return;
                }
            }
            topicAdminClient.createTopic(topicName);
            topicSet.add(topicId);
            createSubscriptionIfNotExists(subscriptionId, topicName);
        } catch (IOException e) {
            log.error("Failed to create topic: [{}].", topicId, e);
            throw new RuntimeException("Failed to create topic.", e);
        } finally {
            topicLock.unlock();
        }
    }

    private void createSubscriptionIfNotExists(String subscriptionId, ProjectTopicName topicName) {
        if (subscriptionSet.contains(subscriptionId)) {
            return;
        }

        subscriptionLock.lock();
        if (subscriptionSet.contains(subscriptionId)) {
            return;
        }

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(pubSubSettings.getProjectId(), subscriptionId);


            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (Subscription subscription : response.iterateAll()) {
                if (subscription.getName().equals(subscriptionName.toString())) {
                    subscriptionSet.add(subscriptionId);
                    return;
                }
            }

            subscriptionAdminClient.createSubscription(
                    subscriptionName, topicName, PushConfig.getDefaultInstance(), 0).getName();
            subscriptionSet.add(subscriptionId);
        } catch (IOException e) {
            log.error("Failed to create subscription: [{}].", subscriptionId, e);
            throw new RuntimeException("Failed to create subscription.", e);
        } finally {
            subscriptionLock.unlock();
        }
    }

}
