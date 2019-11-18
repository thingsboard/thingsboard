/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.queue.TbAbstractMsgQueueService;
import org.thingsboard.server.service.queue.TbMsgQueuePack;
import org.thingsboard.server.service.queue.TbMsgQueueState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "aws")
public class TbAwsMsgQueueService extends TbAbstractMsgQueueService {

    private AmazonSQS sqsClient;

    @Value("${backpressure.aws_sqs.access_key_id}")
    private String accessKeyId;

    @Value("${backpressure.aws_sqs.secret_access_key}")
    private String secretAccessKey;

    @Value("${backpressure.aws_sqs.region}")
    private String region;

    @Value("${backpressure.aws_sqs.queue_url}")
    private String queueUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void init() {
        //max value for aws
        if (msgPackSize > 10) {
            msgPackSize = 10;
        }

        ackMap.put(collectiveTenantId, new AtomicBoolean(true));
        specialTenants.forEach(tenantId -> ackMap.put(tenantId, new AtomicBoolean(true)));

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        try {
            sqsClient = AmazonSQSClientBuilder.standard()
                    .withCredentials(credProvider)
                    .withRegion(region)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build aws sqs client");
            throw new RuntimeException(e);
        }

        executor.submit(() -> {
            while (true) {
                if (ackMap.get(collectiveTenantId).get()) {
                    ReceiveMessageRequest request = new ReceiveMessageRequest();
                    request
                            .withQueueUrl(queueUrl)
                            .withMessageAttributeNames(TENANT_KEY)
                            .withMaxNumberOfMessages(msgPackSize);
                    List<Message> messages = sqsClient.receiveMessage(request).getMessages();
                    if (messages.size() > 0) {
                        ackMap.get(collectiveTenantId).set(false);
                        createAndSendTbMsgQueuePack(messages);
                    }
                }
            }
        });
    }

    @Override
    public void add(TbMsg msg, TenantId tenantId) {
        SendMessageRequest sendMsgRequest = new SendMessageRequest();
        sendMsgRequest.withQueueUrl(queueUrl);

        try {
            sendMsgRequest.withMessageBody(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize msg: [{}]", msg);
            throw new RuntimeException(e);
        }

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(TENANT_KEY, new MessageAttributeValue().withDataType("String").withStringValue(tenantId.toString()));
        sendMsgRequest.setMessageAttributes(messageAttributes);

        sendMsgRequest.withMessageDeduplicationId(msg.getId().toString());
        sendMsgRequest.withMessageGroupId(msg.getOriginator().toString());

        sqsClient.sendMessage(sendMsgRequest);
        log.info("Add new message: [{}] for tenant: [{}]", msg, tenantId.getId());
    }

    private void createAndSendTbMsgQueuePack(List<Message> msgList) {
        UUID packId = UUID.randomUUID();
        TbMsgQueuePack pack = new TbMsgQueuePack(packId, new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicBoolean(false), collectiveTenantId);

        msgList
                .parallelStream()
                .forEach(awsMsg -> {
                    try {
                        TbMsg tbMsg = objectMapper.readValue(awsMsg.getBody(), TbMsg.class);
                        TenantId tenantId = new TenantId(UUID.fromString(awsMsg.getMessageAttributes().get("tenantId").getStringValue()));
                        TbMsgQueueState msgQueueState = new TbMsgQueueState(
                                tbMsg.copy(tbMsg.getId(), packId, tbMsg.getRuleChainId(), tbMsg.getRuleNodeId(), tbMsg.getClusterPartition()),
                                tenantId,
                                new AtomicInteger(0),
                                new AtomicBoolean(false));
                        pack.addMsg(msgQueueState);
                        sqsClient.deleteMessage(queueUrl, awsMsg.getReceiptHandle());
                    } catch (IOException e) {
                        log.error("Failed to process aws msg");
                    }
                });
        packMap.put(pack.getTenantId(), pack);
        send(pack);
    }

    @Override
    @PreDestroy
    protected void destroy() {
        super.destroy();
        if (sqsClient != null) {
            try {
                sqsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SQS client during destroy()", e);
            }
        }
    }
}
