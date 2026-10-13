// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureServiceBusClientConfiguration {

    @TemplateField(key = "azureSbConnectionString", label = "Connection string",
                   type = FormFieldType.PASSWORD, secret = true, group = "Azure Service Bus Connection", required = true)
    private String connectionString;

    @TemplateField(key = "azureSbTopicName", label = "Topic name",
                   group = "Azure Service Bus Connection", required = true)
    private String topicName;

    @TemplateField(key = "azureSbSubscriptionName", label = "Subscription name",
                   group = "Azure Service Bus Connection", required = true)
    private String subName;

    private String downlinkConnectionString;
    private String downlinkTopicName;

}
