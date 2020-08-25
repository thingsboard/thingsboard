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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractParallelTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;
import org.thingsboard.server.queue.settings.TbPubSubSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class TbPubSubConsumerTemplate<T extends TbQueueMsg> extends AbstractParallelTbQueueConsumerTemplate<PubsubMessage, T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbPubSubSettings pubSubSettings;

    private volatile Set<String> subscriptionNames;
    private final List<AcknowledgeRequest> acknowledgeRequests = new CopyOnWriteArrayList<>();

    private final SubscriberStub subscriber;
    private volatile int messagesPerTopic;

    public TbPubSubConsumerTemplate(TbQueueAdmin admin, TbPubSubSettings pubSubSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.pubSubSettings = pubSubSettings;
        this.topic = topic;
        this.decoder = decoder;
        try {
            SubscriberStubSettings subscriberStubSettings =
                    SubscriberStubSettings.newBuilder()
                            .setCredentialsProvider(pubSubSettings.getCredentialsProvider())
                            .setTransportChannelProvider(
                                    SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
                                            .setMaxInboundMessageSize(pubSubSettings.getMaxMsgSize())
                                            .build())
                            .build();
            this.subscriber = GrpcSubscriberStub.create(subscriberStubSettings);
        } catch (IOException e) {
            log.error("Failed to create subscriber.", e);
            throw new RuntimeException("Failed to create subscriber.", e);
        }
    }

    @Override
    protected List<PubsubMessage> doPoll(long durationInMillis) {
        try {
            List<ReceivedMessage> messages = receiveMessages();
            if (!messages.isEmpty()) {
                return messages.stream().map(ReceivedMessage::getMessage).collect(Collectors.toList());
            }
        } catch (ExecutionException | InterruptedException e) {
            if (stopped) {
                log.info("[{}] Pub/Sub consumer is stopped.", topic);
            } else {
                log.error("Failed to receive messages", e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        subscriptionNames = new LinkedHashSet<>(topicNames);
        subscriptionNames.forEach(admin::createTopicIfNotExists);
        initNewExecutor(subscriptionNames.size() + 1);
        messagesPerTopic = pubSubSettings.getMaxMessages() / Math.max(subscriptionNames.size(), 1);
    }

    @Override
    protected void doCommit() {
        acknowledgeRequests.forEach(subscriber.acknowledgeCallable()::futureCall);
        acknowledgeRequests.clear();
    }

    @Override
    protected void doUnsubscribe() {
        if (subscriber != null) {
            subscriber.close();
        }
        shutdownExecutor();
    }

    private List<ReceivedMessage> receiveMessages() throws ExecutionException, InterruptedException {
        List<ApiFuture<List<ReceivedMessage>>> result = subscriptionNames.stream().map(subscriptionId -> {
            String subscriptionName = ProjectSubscriptionName.format(pubSubSettings.getProjectId(), subscriptionId);
            PullRequest pullRequest =
                    PullRequest.newBuilder()
                            .setMaxMessages(messagesPerTopic)
//                            .setReturnImmediately(false) // return immediately if messages are not available
                            .setSubscription(subscriptionName)
                            .build();

            ApiFuture<PullResponse> pullResponseApiFuture = subscriber.pullCallable().futureCall(pullRequest);

            return ApiFutures.transform(pullResponseApiFuture, pullResponse -> {
                if (pullResponse != null && !pullResponse.getReceivedMessagesList().isEmpty()) {
                    List<String> ackIds = new ArrayList<>();
                    for (ReceivedMessage message : pullResponse.getReceivedMessagesList()) {
                        ackIds.add(message.getAckId());
                    }
                    AcknowledgeRequest acknowledgeRequest =
                            AcknowledgeRequest.newBuilder()
                                    .setSubscription(subscriptionName)
                                    .addAllAckIds(ackIds)
                                    .build();

                    acknowledgeRequests.add(acknowledgeRequest);
                    return pullResponse.getReceivedMessagesList();
                }
                return null;
            }, consumerExecutor);

        }).collect(Collectors.toList());

        ApiFuture<List<ReceivedMessage>> transform = ApiFutures.transform(ApiFutures.allAsList(result), listMessages -> {
            if (!CollectionUtils.isEmpty(listMessages)) {
                return listMessages.stream().filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);

        return transform.get();
    }

    @Override
    public T decode(PubsubMessage message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getData().toStringUtf8(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}
