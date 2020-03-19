package org.thingsboard.server.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.TbQueueAdmin;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueMsgDecoder;
import org.thingsboard.server.TbQueueMsgHeaders;
import org.thingsboard.server.common.DefaultTbQueueMsgHeaders;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class TbAwsSqsConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private final TbQueueAdmin admin;
    private final AmazonSQS sqsClient;
    private final String topic;
    private final List<String> queueUrlList;
    private ListeningExecutorService consumerExecutor;

    private final Gson gson = new Gson();
    private final TbQueueMsgDecoder<T> decoder;
    private final List<AwsSqsMsgWrapper> messageList = new CopyOnWriteArrayList<>();

    public TbAwsSqsConsumerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.decoder = decoder;
        this.topic = topic;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();
        this.queueUrlList = new CopyOnWriteArrayList<>();

        consumerExecutor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool());
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        queueUrlList.add(getQueueUrl(topic));
        consumerExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(queueUrlList.size()));
    }

    @Override
    public void subscribe(List<Integer> partitions) {
        List<String> topicNames = partitions.stream().map(partition -> topic + "_" + partition).collect(Collectors.toList());
        topicNames.forEach(t -> queueUrlList.add(getQueueUrl(t)));
        consumerExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(queueUrlList.size()));
    }

    @Override
    public void unsubscribe() {
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
        }
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!messageList.isEmpty()) {
            return Collections.emptyList();
        }

        List<ListenableFuture<AwsSqsMsgWrapper>> futures = queueUrlList.stream().map(url -> consumerExecutor.submit(() -> {
            ReceiveMessageRequest request = new ReceiveMessageRequest();
            request
                    .withWaitTimeSeconds((int) (durationInMillis / 1000))
                    .withMessageAttributeNames("headers")
                    .withQueueUrl(url)
                    .withMaxNumberOfMessages(10);
            return new AwsSqsMsgWrapper(url, sqsClient.receiveMessage(request).getMessages());
        })).collect(Collectors.toList());

        try {
            List<AwsSqsMsgWrapper> messages =
                    Futures.allAsList(futures).get()
                            .stream()
                            .filter(msg -> !msg.getMessages().isEmpty())
                            .collect(Collectors.toList());

            if (messages.size() > 0) {
                messageList.addAll(messages);

                return messages.stream()
                        .flatMap(msg -> msg.getMessages().stream())
                        .map(msg -> {
                            try {
                                return decode(msg);
                            } catch (IOException e) {
                                log.error("Failed decoding message: [{}]", msg);
                                return null;
                            }
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();

        } catch (InterruptedException | ExecutionException e) {
            log.error("Failure pooling messages.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void commit() {
        try {
            Futures.successfulAsList(messageList
                    .stream()
                    .map(msg -> consumerExecutor.submit(() -> msg
                            .getMessages()
                            .forEach(message -> sqsClient.deleteMessage(msg.url, message.getReceiptHandle()))
                    )).collect(Collectors.toList())).get();
            messageList.clear();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failure commit messages.", e);
        }
    }

    public T decode(Message message) throws InvalidProtocolBufferException {
        TbAwsSqsMsg msg = gson.fromJson(message.getBody(), TbAwsSqsMsg.class);
        TbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        Map<String, byte[]> headerMap = gson.fromJson(message.getMessageAttributes().get("headers").getStringValue(), new TypeToken<Map<String, byte[]>>() {
        }.getType());
        headerMap.forEach(headers::put);
        msg.setHeaders(headers);
        return decoder.decode(msg);
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
