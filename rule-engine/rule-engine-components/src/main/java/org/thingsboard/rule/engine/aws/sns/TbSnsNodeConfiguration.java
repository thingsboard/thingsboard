// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.aws.sns;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbSnsNodeConfiguration implements NodeConfiguration<TbSnsNodeConfiguration> {

    private String topicArnPattern;
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    @Override
    public TbSnsNodeConfiguration defaultConfiguration() {
        TbSnsNodeConfiguration configuration = new TbSnsNodeConfiguration();
        configuration.setTopicArnPattern("arn:aws:sns:us-east-1:123456789012:MyNewTopic");
        configuration.setRegion("us-east-1");
        return configuration;
    }
}
