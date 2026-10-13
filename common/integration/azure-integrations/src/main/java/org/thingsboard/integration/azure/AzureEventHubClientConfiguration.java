// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureEventHubClientConfiguration {

    @TemplateField(key = "azureEhConnectionString", label = "Connection string",
                   type = FormFieldType.PASSWORD, secret = true, group = "Azure Event Hub Connection", required = true)
    private String connectionString;

    @TemplateField(key = "azureEhIotHubName", label = "IoT Hub name",
                   group = "Azure Event Hub Connection", required = true)
    private String iotHubName;

    @TemplateField(key = "azureEhConsumerGroup", label = "Consumer group",
                   group = "Azure Event Hub Connection")
    private String consumerGroup;

    private int connectTimeoutSec;

    private String storageConnectionString;
    private String containerName;
    private boolean enablePersistentCheckpoints;

    public String getConsumerGroup() {
        return (consumerGroup == null || consumerGroup.isEmpty())
                ? EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME
                : consumerGroup;
    }

}
