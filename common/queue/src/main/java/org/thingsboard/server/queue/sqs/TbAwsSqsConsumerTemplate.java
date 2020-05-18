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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractParallelTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;
import org.thingsboard.server.queue.settings.TbAwsSqsSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TbAwsSqsConsumerTemplate<T extends TbQueueMsg> extends AbstractParallelTbQueueConsumerTemplate<Message, T> {

    private static final int MAX_NUM_MSGS = 10;

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final AmazonSQS sqsClient;
    private final TbQueueMsgDecoder<T> decoder;
    private final TbAwsSqsSettings sqsSettings;

    private final List<AwsSqsMsgWrapper> pendingMessages = new CopyOnWriteArrayList<>();
    private volatile Set<String> queueUrls;

    public TbAwsSqsConsumerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.sqsSettings = sqsSettings;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        queueUrls = topicNames.stream().map(this::getQueueUrl).collect(Collectors.toSet());
        initNewExecutor(queueUrls.size() * sqsSettings.getThreadsPerTopic() + 1);
    }

    @Override
    protected List<Message> doPoll(long durationInMillis) {
        int duration = (int) TimeUnit.MILLISECONDS.toSeconds(durationInMillis);
        List<ListenableFuture<List<Message>>> futureList = queueUrls
                .stream()
                .map(url -> poll(url, duration))
                .collect(Collectors.toList());
        ListenableFuture<List<List<Message>>> futureResult = Futures.allAsList(futureList);
        try {
            return futureResult.get().stream()
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Aws SQS consumer is stopped.", getTopic());
            } else {
                log.error("Failed to pool messages.", e);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public T decode(Message message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getBody(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

    @Override
    protected void doCommit() {
        pendingMessages.forEach(msg ->
                consumerExecutor.submit(() -> {
                    List<DeleteMessageBatchRequestEntry> entries = msg.getMessages()
                            .stream()
                            .map(message -> new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()))
                            .collect(Collectors.toList());
                    sqsClient.deleteMessageBatch(msg.getUrl(), entries);
                }));
        pendingMessages.clear();
    }

    @Override
    protected void doUnsubscribe() {
        stopped = true;
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        shutdownExecutor();
    }

    private ListenableFuture<List<Message>> poll(String url, int waitTimeSeconds) {
        List<ListenableFuture<List<Message>>> result = new ArrayList<>();

        for (int i = 0; i < sqsSettings.getThreadsPerTopic(); i++) {
            result.add(consumerExecutor.submit(() -> {
                ReceiveMessageRequest request = new ReceiveMessageRequest();
                request
                        .withWaitTimeSeconds(waitTimeSeconds)
                        .withQueueUrl(url)
                        .withMaxNumberOfMessages(MAX_NUM_MSGS);
                return sqsClient.receiveMessage(request).getMessages();
            }));
        }
        return Futures.transform(Futures.allAsList(result), list -> {
            if (!CollectionUtils.isEmpty(list)) {
                return list.stream()
                        .flatMap(messageList -> {
                            if (!messageList.isEmpty()) {
                                this.pendingMessages.add(new AwsSqsMsgWrapper(url, messageList));
                                return messageList.stream();
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);
    }

    @Data
    private static class AwsSqsMsgWrapper {
        private final String url;
        private final List<Message> messages;

        public AwsSqsMsgWrapper(String url, List<Message> messages) {
            this.url = url;
            this.messages = messages;
        }
    }

    private String getQueueUrl(String topic) {
        admin.createTopicIfNotExists(topic);
        return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
    }
}
