// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AwsSmsSender extends AbstractSmsSender {

    private static final Map<String, MessageAttributeValue> SMS_ATTRIBUTES = new HashMap<>();

    static {
        SMS_ATTRIBUTES.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Transactional")
                .withDataType("String"));
    }

    private AmazonSNS snsClient;

    public AwsSmsSender(AwsSnsSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccessKeyId()) || StringUtils.isEmpty(config.getSecretAccessKey()) || StringUtils.isEmpty(config.getRegion())) {
            throw new IllegalArgumentException("Invalid AWS sms provider configuration: aws accessKeyId, aws secretAccessKey and aws region should be specified!");
        }
        AWSCredentials awsCredentials = new BasicAWSCredentials(config.getAccessKeyId(), config.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        this.snsClient = AmazonSNSClient.builder()
                .withCredentials(credProvider)
                .withRegion(config.getRegion())
                .build();
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);
        try {
            PublishRequest publishRequest = new PublishRequest()
                    .withMessageAttributes(SMS_ATTRIBUTES)
                    .withPhoneNumber(numberTo)
                    .withMessage(message);
            this.snsClient.publish(publishRequest);
            return this.countMessageSegments(message);
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (this.snsClient != null) {
            try {
                this.snsClient.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown SNS client during destroy()", e);
            }
        }
    }
}
