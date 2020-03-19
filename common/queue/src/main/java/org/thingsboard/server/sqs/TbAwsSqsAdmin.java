package org.thingsboard.server.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import org.thingsboard.server.TbQueueAdmin;

import java.util.HashMap;
import java.util.Map;

public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final AmazonSQS sqsClient;
    final Map<String, String> attributes = new HashMap<>();

    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings) {
        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        this.sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();
        attributes.put("FifoQueue", "true");
        attributes.put("ContentBasedDeduplication", "true");
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        final CreateQueueRequest createQueueRequest =
                new CreateQueueRequest(topic.replaceAll("\\.", "_") + ".fifo")
                        .withAttributes(attributes);
        sqsClient.createQueue(createQueueRequest);
    }
}
