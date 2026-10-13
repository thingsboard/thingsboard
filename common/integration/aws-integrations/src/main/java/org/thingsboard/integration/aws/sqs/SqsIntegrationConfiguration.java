// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

/*
 * Created by Valerii Sosliuk on 03.06.19
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqsIntegrationConfiguration {

    @TemplateField(key = "sqsQueueUrl", label = "Queue URL",
                   group = "AWS SQS Connection", required = true)
    private String queueUrl;

    @TemplateField(key = "sqsRegion", label = "AWS region",
                   group = "AWS SQS Connection", required = true)
    private String region;

    @TemplateField(key = "sqsAccessKeyId", label = "Access key ID",
                   secret = true, group = "AWS SQS Connection", required = true)
    private String accessKeyId;

    @TemplateField(key = "sqsSecretAccessKey", label = "Secret access key",
                   type = FormFieldType.PASSWORD, secret = true, group = "AWS SQS Connection", required = true)
    private String secretAccessKey;

    private Long pollingPeriodSeconds;
}
