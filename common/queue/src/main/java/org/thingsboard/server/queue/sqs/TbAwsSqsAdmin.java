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
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.settings.TbAwsSqsSettings;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final Map<String, String> attributes;
    private final AmazonSQS sqsClient;
    private final Map<String, String> queues;

    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings, Map<String, String> attributes) {
        this.attributes = attributes;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(sqsSettings.getRegion())
                .build();

        queues = sqsClient
                .listQueues()
                .getQueueUrls()
                .stream()
                .map(this::getQueueNameFromUrl)
                .collect(Collectors.toMap(this::convertTopicToQueueName, Function.identity()));
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            return;
        }
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);
        String queueUrl = sqsClient.createQueue(createQueueRequest).getQueueUrl();
        queues.put(getQueueNameFromUrl(queueUrl), queueUrl);
    }

    private String getQueueNameFromUrl(String queueUrl) {
        int delimiterIndex = queueUrl.lastIndexOf("/");
        return queueUrl.substring(delimiterIndex + 1);
    }

    private String convertTopicToQueueName(String topic) {
        return topic.replaceAll("\\.", "_") + ".fifo";
    }

    @Override
    public void deleteTopic(String topic) {
        String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            sqsClient.deleteQueue(queues.get(queueName));
        } else {
            GetQueueUrlResult queueUrl = sqsClient.getQueueUrl(queueName);
            if (queueUrl != null) {
                sqsClient.deleteQueue(queueUrl.getQueueUrl());
            } else {
                log.warn("Aws SQS queue [{}] does not exist!", queueName);
            }
        }
    }

    @Override
    public void destroy() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }
}
