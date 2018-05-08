/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.aws.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "aws sqs",
        configClazz = TbSqsNodeConfiguration.class,
        nodeDescription = "Publish messages to AWS SQS",
        nodeDetails = "Expects messages with any message type. Will publish message to AWS SQS queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSqsConfig"
)
public class TbSqsNode implements TbNode {

    private static final String MESSAGE_ID = "messageId";
    private static final String REQUEST_ID = "requestId";
    private static final String MESSAGE_BODY_MD5 = "messageBodyMd5";
    private static final String MESSAGE_ATTRIBUTES_MD5 = "messageAttributesMd5";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    private static final String ERROR = "error";

    private TbSqsNodeConfiguration config;
    private AmazonSQS sqsClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSqsNodeConfiguration.class);
        AWSCredentials awsCredentials = new BasicAWSCredentials(this.config.getAccessKeyId(), this.config.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        try {
            this.sqsClient = AmazonSQSClientBuilder.standard()
                    .withCredentials(credProvider)
                    .withRegion(this.config.getRegion())
                    .build();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(publishMessageAsync(ctx, msg),
                m -> ctx.tellNext(m, TbRelationTypes.SUCCESS),
                t -> {
                    TbMsg next = processException(ctx, msg, t);
                    ctx.tellNext(next, TbRelationTypes.FAILURE, t);
                });
    }

    ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    TbMsg publishMessage(TbContext ctx, TbMsg msg) {
        String queueUrl = TbNodeUtils.processPattern(this.config.getQueueUrlPattern(), msg.getMetaData());
        SendMessageRequest sendMsgRequest =  new SendMessageRequest();
        sendMsgRequest.withQueueUrl(queueUrl);
        sendMsgRequest.withMessageBody(msg.getData());
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        this.config.getMessageAttributes().forEach((k,v) -> {
            String name = TbNodeUtils.processPattern(k, msg.getMetaData());
            String val = TbNodeUtils.processPattern(v, msg.getMetaData());
            messageAttributes.put(name, new MessageAttributeValue().withDataType("String").withStringValue(val));
        });
        sendMsgRequest.setMessageAttributes(messageAttributes);
        if (this.config.getQueueType() == TbSqsNodeConfiguration.QueueType.STANDARD) {
            sendMsgRequest.withDelaySeconds(this.config.getDelaySeconds());
        } else {
            sendMsgRequest.withMessageDeduplicationId(msg.getId().toString());
            sendMsgRequest.withMessageGroupId(msg.getOriginator().toString());
        }
        SendMessageResult result = this.sqsClient.sendMessage(sendMsgRequest);
        return processSendMessageResult(ctx, msg, result);
    }

    private TbMsg processSendMessageResult(TbContext ctx, TbMsg origMsg, SendMessageResult result) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(MESSAGE_ID, result.getMessageId());
        metaData.putValue(REQUEST_ID, result.getSdkResponseMetadata().getRequestId());
        if (!StringUtils.isEmpty(result.getMD5OfMessageBody())) {
            metaData.putValue(MESSAGE_BODY_MD5, result.getMD5OfMessageBody());
        }
        if (!StringUtils.isEmpty(result.getMD5OfMessageAttributes())) {
            metaData.putValue(MESSAGE_ATTRIBUTES_MD5, result.getMD5OfMessageAttributes());
        }
        if (!StringUtils.isEmpty(result.getSequenceNumber())) {
            metaData.putValue(SEQUENCE_NUMBER, result.getSequenceNumber());
        }
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    @Override
    public void destroy() {
        if (this.sqsClient != null) {
            try {
                this.sqsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SQS client during destroy()", e);
            }
        }
    }
}
