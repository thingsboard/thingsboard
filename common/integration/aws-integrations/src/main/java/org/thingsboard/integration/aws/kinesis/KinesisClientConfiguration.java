// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.kinesis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KinesisClientConfiguration {

    @TemplateField(key = "kinesisStreamName", label = "Stream name",
                   group = "AWS Kinesis Connection", required = true)
    private String streamName;

    @TemplateField(key = "kinesisRegion", label = "AWS region",
                   group = "AWS Kinesis Connection", required = true)
    private String region;

    @TemplateField(key = "kinesisAccessKeyId", label = "Access key ID",
                   secret = true, group = "AWS Kinesis Connection", required = true)
    private String accessKeyId;

    @TemplateField(key = "kinesisSecretAccessKey", label = "Secret access key",
                   type = FormFieldType.PASSWORD, secret = true, group = "AWS Kinesis Connection", required = true)
    private String secretAccessKey;

    private boolean useConsumersWithEnhancedFanOut;
    private boolean useCredentialsFromInstanceMetadata;
    private String applicationName;
    private String initialPositionInStream;
    private Integer maxRecords;
    private Long requestTimeout;
}
