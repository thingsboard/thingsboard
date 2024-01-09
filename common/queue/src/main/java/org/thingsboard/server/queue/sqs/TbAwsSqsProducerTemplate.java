/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TbAwsSqsProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    private final String defaultTopic;
    private final AmazonSQSAsync sqsClient;
    private final Gson gson = new Gson();
    private final Map<String, String> queueUrlMap = new ConcurrentHashMap<>();
    private final TbAwsSqsAdmin admin;

    public TbAwsSqsProducerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String defaultTopic) {
        this.admin = (TbAwsSqsAdmin) admin;
        this.defaultTopic = defaultTopic;

        AWSCredentialsProvider credentialsProvider;
        if (sqsSettings.getUseDefaultCredentialProviderChain()) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        }

        sqsClient = AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(sqsSettings.getRegion())
                .withExecutorFactory(this.admin::getProducerExecutor)
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
        sendMsgRequest.withQueueUrl(getQueueUrl(tpi.getFullTopicName()));
        sendMsgRequest.withMessageBody(gson.toJson(new DefaultTbQueueMsg(msg)));

        String sqsMsgId = UUID.randomUUID().toString();
        sendMsgRequest.withMessageGroupId(sqsMsgId);
        sendMsgRequest.withMessageDeduplicationId(sqsMsgId);

        sqsClient.sendMessageAsync(sendMsgRequest, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
            @Override public void onError(Exception e) {
                if (callback != null) {
                    callback.onFailure(e);
                }
            }

            @Override public void onSuccess(SendMessageRequest request,
                                            SendMessageResult sendMessageResult) {
                if (callback != null) {
                    callback.onSuccess(new AwsSqsTbQueueMsgMetadata(sendMessageResult.getSdkHttpMetadata()));
                }
            }
        });
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
