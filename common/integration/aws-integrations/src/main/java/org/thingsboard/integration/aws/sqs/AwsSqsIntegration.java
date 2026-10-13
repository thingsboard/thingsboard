// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class AwsSqsIntegration extends AbstractIntegration<SqsIntegrationMsg> {

    private IntegrationContext context;
    private SqsIntegrationConfiguration sqsConfiguration;
    private AmazonSQS sqs;
    private ScheduledFuture<?> taskFuture;
    private volatile boolean stopped;
    private final Lock pollLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;
        this.context = params.getContext();
        this.sqsConfiguration = JacksonUtil.fromString(
                JacksonUtil.toString(configuration.getConfiguration().get("sqsConfiguration")),
                SqsIntegrationConfiguration.class);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(sqsConfiguration.getAccessKeyId(), sqsConfiguration.getSecretAccessKey());
        sqs = AmazonSQSClientBuilder.standard().withRegion(sqsConfiguration.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        schedulePoll();
    }

    private void schedulePoll() {
        taskFuture = this.context.getScheduledExecutorService().schedule(this::submitPoll, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
    }

    private void submitPoll() {
        context.getExecutorService().execute(this::pollMessages);
    }

    private void pollMessages() {
        pollLock.lock();
        try {
            if (stopped) {
                return;
            }
            ReceiveMessageRequest sqsRequest = new ReceiveMessageRequest();
            sqsRequest.setQueueUrl(sqsConfiguration.getQueueUrl());
            sqsRequest.setMaxNumberOfMessages(10);
            List<Message> messages = sqs.receiveMessage(sqsRequest).getMessages();
            if (!CollectionUtils.isEmpty(messages)) {
                for (Message message : messages) {
                    try {
                        SqsIntegrationMsg sqsMessage = toSqsIntegrationMsg(message);
                        process(sqsMessage);
                    } catch (IOException e) {
                        log.error("Failed to process message: " + message + ". Reason: " + e.getMessage(), e);
                    } finally {
                        sqs.deleteMessage(sqsConfiguration.getQueueUrl(), message.getReceiptHandle());
                    }
                }
                if (!stopped) {
                    submitPoll();
                }
            } else {
                schedulePoll();
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            persistDebug(context, "Uplink", getDefaultUplinkContentType(), e.getMessage(), "ERROR", e);
            schedulePoll();
        } finally {
            pollLock.unlock();
        }
    }

    private SqsIntegrationMsg toSqsIntegrationMsg(Message message) throws IOException {
        String unescaped = StringEscapeUtils.unescapeJson(message.getBody());
        unescaped = StringUtils.removeStart(unescaped, "\"");
        unescaped = StringUtils.removeEnd(unescaped, "\"");
        JsonNode node = JacksonUtil.toJsonNode(unescaped);
        return new SqsIntegrationMsg(node, metadataTemplate.getKvMap());
    }

    @Override
    public void process(SqsIntegrationMsg message) {
        String status = "OK";
        Exception exception = null;
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, message.getPayload(), new UplinkMetaData<>(getDefaultUplinkContentType(), message.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkDataBlocking(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            exception = e;
            status = "ERROR";
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(message.getJson()), status, exception);
    }

    @Override
    public void destroy() {
        stopped = true;
        pollLock.lock();
        try {
            if (sqs != null) {
                sqs.shutdown();
            }
            if (taskFuture != null) {
                taskFuture.cancel(true);
            }
        } finally {
            pollLock.unlock();
        }
    }

}
