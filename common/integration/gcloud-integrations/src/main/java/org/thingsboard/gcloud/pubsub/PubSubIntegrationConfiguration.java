// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.gcloud.pubsub;

import lombok.Data;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

@Data
public class PubSubIntegrationConfiguration {

    @TemplateField(key = "pubsubProjectId", label = "Project ID",
                   group = "Pub/Sub Connection", required = true)
    private String projectId;

    @TemplateField(key = "pubsubSubscriptionId", label = "Subscription ID",
                   group = "Pub/Sub Connection", required = true)
    private String subscriptionId;

    @TemplateField(key = "pubsubServiceAccountKey", label = "Service account key (JSON)",
                   type = FormFieldType.PASSWORD, secret = true, group = "Pub/Sub Connection", required = true)
    private String serviceAccountKey;

    private String serviceAccountKeyFileName;

}
