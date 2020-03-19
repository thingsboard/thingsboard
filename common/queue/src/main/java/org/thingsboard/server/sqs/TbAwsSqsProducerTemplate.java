package org.thingsboard.server.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.TbQueueAdmin;
import org.thingsboard.server.TbQueueCallback;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.discovery.TopicPartitionInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbAwsSqsProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final AmazonSQS sqsClient;
    private final Gson gson = new Gson();
    private final Map<String, String> queueUrlMap = new ConcurrentHashMap<>();
    private final TbQueueAdmin admin;

    public TbAwsSqsProducerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();
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
        SendMessageRequest sendMsgRequest = new SendMessageRequest();
        sendMsgRequest.withQueueUrl(getQueueUrl(tpi.getComplexTopic()));
        sendMsgRequest.withMessageBody(gson.toJson(new TbAwsSqsMsg(msg.getKey(), msg.getData())));

        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        attributes.put("headers", new MessageAttributeValue()
                .withStringValue(gson.toJson(msg.getHeaders().getData()))
                .withDataType("String"));

        sendMsgRequest.withMessageAttributes(attributes);
        sendMsgRequest.withMessageGroupId(msg.getKey().toString());
        sqsClient.sendMessage(sendMsgRequest);
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void stop() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    private String getQueueUrl(String topic) {
        return queueUrlMap.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
        });
    }
}
