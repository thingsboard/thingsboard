// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.gcp.pubsub;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

@Data
public class TbPubSubNodeConfiguration implements NodeConfiguration<TbPubSubNodeConfiguration> {

    private String projectId;
    private String topicName;
    private Map<String, String> messageAttributes;
    private String serviceAccountKey;
    private String serviceAccountKeyFileName;

    @Override
    public TbPubSubNodeConfiguration defaultConfiguration() {
        TbPubSubNodeConfiguration configuration = new TbPubSubNodeConfiguration();
        configuration.setProjectId("my-google-cloud-project-id");
        configuration.setTopicName("my-pubsub-topic-name");
        configuration.setMessageAttributes(Collections.emptyMap());
        return configuration;
    }
}
