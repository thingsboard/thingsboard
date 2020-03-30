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
import com.amazonaws.services.sqs.model.QueueAttributeName;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.HashMap;
import java.util.Map;

public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final TbAwsSqsSettings sqsSettings;
    private final Map<String, String> attributes = new HashMap<>();
    private final AWSStaticCredentialsProvider credProvider;

    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings) {
        this.sqsSettings = sqsSettings;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        this.credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        attributes.put("FifoQueue", "true");
        attributes.put("ContentBasedDeduplication", "true");
        attributes.put(QueueAttributeName.VisibilityTimeout.toString(), sqsSettings.getVisibilityTimeout());
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

        final CreateQueueRequest createQueueRequest =
                new CreateQueueRequest(topic.replaceAll("\\.", "_") + ".fifo")
                        .withAttributes(attributes);
        try {
            sqsClient.createQueue(createQueueRequest);
        } finally {
            if (sqsClient != null) {
                sqsClient.shutdown();
            }
        }
    }
}
