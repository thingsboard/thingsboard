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

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.ListSubscriptionsRequest;
import com.google.pubsub.v1.ListTopicsRequest;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {
    private static final String ACK_DEADLINE = "ackDeadlineInSec";
    private static final String MESSAGE_RETENTION = "messageRetentionInSec";

    private final TopicAdminClient topicAdminClient;
    private final SubscriptionAdminClient subscriptionAdminClient;

    private final TbPubSubSettings pubSubSettings;
    private final Set<String> topicSet = ConcurrentHashMap.newKeySet();
    private final Set<String> subscriptionSet = ConcurrentHashMap.newKeySet();
    private final Map<String, String> subscriptionProperties;

    public TbPubSubAdmin(TbPubSubSettings pubSubSettings, Map<String, String> subscriptionSettings) {
        this.pubSubSettings = pubSubSettings;
        this.subscriptionProperties = subscriptionSettings;

        TopicAdminSettings topicAdminSettings;
        try {
            topicAdminSettings = TopicAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create TopicAdminSettings");
            throw new RuntimeException("Failed to create TopicAdminSettings.");
        }

        SubscriptionAdminSettings subscriptionAdminSettings;
        try {
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().setCredentialsProvider(pubSubSettings.getCredentialsProvider()).build();
        } catch (IOException e) {
            log.error("Failed to create SubscriptionAdminSettings");
            throw new RuntimeException("Failed to create SubscriptionAdminSettings.");
        }

        try {
            topicAdminClient = TopicAdminClient.create(topicAdminSettings);

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

        try {
            subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);

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
        TopicName topicName = TopicName.newBuilder()
                .setTopic(partition)
                .setProject(pubSubSettings.getProjectId())
                .build();

        if (topicSet.contains(topicName.toString())) {
            createSubscriptionIfNotExists(partition, topicName);
            return;
        }

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

        try {
            topicAdminClient.createTopic(topicName);
            log.info("Created new topic: [{}]", topicName.toString());
        } catch (AlreadyExistsException e) {
            log.info("[{}] Topic already exist.", topicName.toString());
        } finally {
            topicSet.add(topicName.toString());
        }
        createSubscriptionIfNotExists(partition, topicName);
    }

    private void createSubscriptionIfNotExists(String partition, TopicName topicName) {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(pubSubSettings.getProjectId(), partition);

        if (subscriptionSet.contains(subscriptionName.toString())) {
            return;
        }

        ListSubscriptionsRequest listSubscriptionsRequest =
                ListSubscriptionsRequest.newBuilder().setProject(ProjectName.of(pubSubSettings.getProjectId()).toString()).build();
        SubscriptionAdminClient.ListSubscriptionsPagedResponse response = subscriptionAdminClient.listSubscriptions(listSubscriptionsRequest);
        for (Subscription subscription : response.iterateAll()) {
            if (subscription.getName().equals(subscriptionName.toString())) {
                subscriptionSet.add(subscription.getName());
                return;
            }
        }

        Subscription.Builder subscriptionBuilder = Subscription
                .newBuilder()
                .setName(subscriptionName.toString())
                .setTopic(topicName.toString());

        setAckDeadline(subscriptionBuilder);
        setMessageRetention(subscriptionBuilder);

        try {
            subscriptionAdminClient.createSubscription(subscriptionBuilder.build());
            log.info("Created new subscription: [{}]", subscriptionName.toString());
        } catch (AlreadyExistsException e) {
            log.info("[{}] Subscription already exist.", subscriptionName.toString());
        } finally {
            subscriptionSet.add(subscriptionName.toString());
        }
    }

    private void setAckDeadline(Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(ACK_DEADLINE)) {
            builder.setAckDeadlineSeconds(Integer.parseInt(subscriptionProperties.get(ACK_DEADLINE)));
        }
    }

    private void setMessageRetention(Subscription.Builder builder) {
        if (subscriptionProperties.containsKey(MESSAGE_RETENTION)) {
            Duration duration = Duration
                    .newBuilder()
                    .setSeconds(Long.parseLong(subscriptionProperties.get(MESSAGE_RETENTION)))
                    .build();
            builder.setMessageRetentionDuration(duration);
        }
    }

    @Override
    public void destroy() {
        if (topicAdminClient != null) {
            topicAdminClient.close();
        }
        if (subscriptionAdminClient != null) {
            subscriptionAdminClient.close();
        }
    }
}
