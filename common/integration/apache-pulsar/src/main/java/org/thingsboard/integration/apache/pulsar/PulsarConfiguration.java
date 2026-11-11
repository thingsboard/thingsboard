// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.apache.pulsar;

import lombok.Data;
import org.thingsboard.integration.apache.pulsar.credentials.PulsarCredentials;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
public class PulsarConfiguration {

    @TemplateField(key = "pulsarServiceUrl", label = "Service URL",
                   group = "Pulsar Connection", required = true)
    private String serviceUrl;

    @TemplateField(key = "pulsarTopics", label = "Topics",
                   group = "Pulsar Connection", required = true)
    private String topics;

    @TemplateField(key = "pulsarSubscriptionName", label = "Subscription name",
                   group = "Pulsar Connection", required = true)
    private String subscriptionName;

    private int maxNumMessages;
    private int maxNumBytes;
    private int timeoutInMs;
    private PulsarCredentials credentials;
}
