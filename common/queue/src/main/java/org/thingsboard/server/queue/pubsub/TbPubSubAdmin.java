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
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {

    private final TbPubSubSettings pubSubSettings;
    private final SubscriptionAdminSettings subscriptionAdminSettings;
    private final TopicAdminSettings topicAdminSettings;
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

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (Topic topic : response.iterateAll()) {
                topicSet.add(topic.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get topics.", e);
            throw new RuntimeException("Failed to get topics.", e);
        }

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {

            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (Subscription subscription : response.iterateAll()) {
                subscriptionSet.add(subscription.getName());
            }
        } catch (IOException e) {
            log.error("Failed to get subscriptions.", e);
            throw new RuntimeException("Failed to get subscriptions.", e);
        }
    }

    @Override
    public void createTopicIfNotExists(String partition) {
        ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), partition);

        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            ListTopicsRequest listTopicsRequest =
                    ListTopicsRequest.newBuilder().setProject(ProjectName.format(pubSubSettings.getProjectId())).build();
            TopicAdminClient.ListTopicsPagedResponse response = topicAdminClient.listTopics(listTopicsRequest);
            for (Topic topic : response.iterateAll()) {
                if (topic.getName().contains(topicName.toString())) {
                    topicSet.add(topic.getName());
                    createSubscriptionIfNotExists(partition, topicName);
                    return;
                }
            }

            topicAdminClient.createTopic(topicName);
            topicSet.add(topicName.toString());
            log.info("Created new topic: [{}]", topicName.toString());
            createSubscriptionIfNotExists(partition, topicName);
        } catch (IOException e) {
            log.error("Failed to create topic: [{}].", topicName.toString(), e);
            throw new RuntimeException("Failed to create topic.", e);
        }
    }

    private void createSubscriptionIfNotExists(String partition, ProjectTopicName topicName) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), partition);

        if (subscriptionSet.contains(subscriptionName.toString())) {
            return;
        }

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            ListSubscriptionsRequest listSubscriptionsRequest =
                    ListSubscriptionsRequest.newBuilder()
                            .setProject(ProjectName.of(pubSubSettings.getProjectId()).toString())
                            .build();
            SubscriptionAdminClient.ListSubscriptionsPagedResponse response =
                    subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);

            for (Subscription subscription : response.iterateAll()) {
                if (subscription.getName().equals(subscriptionName.toString())) {
                    subscriptionSet.add(subscription.getName());
                    return;
                }
            }

            subscriptionAdminClient.createSubscription(
                    subscriptionName, topicName, PushConfig.getDefaultInstance(), pubSubSettings.getAckDeadline()).getName();
            subscriptionSet.add(subscriptionName.toString());
            log.info("Created new subscription: [{}]", subscriptionName.toString());
        } catch (IOException e) {
            log.error("Failed to create subscription: [{}].", subscriptionName.toString(), e);
            throw new RuntimeException("Failed to create subscription.", e);
        }
    }

}
