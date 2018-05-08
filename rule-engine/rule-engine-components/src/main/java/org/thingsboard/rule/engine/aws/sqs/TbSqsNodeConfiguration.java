/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thingsboard.rule.engine.aws.sqs;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

@Data
public class TbSqsNodeConfiguration implements NodeConfiguration<TbSqsNodeConfiguration> {

    private QueueType queueType;
    private String queueUrlPattern;
    private int delaySeconds;
    private Map<String, String> messageAttributes;
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    @Override
    public TbSqsNodeConfiguration defaultConfiguration() {
        TbSqsNodeConfiguration configuration = new TbSqsNodeConfiguration();
        configuration.setQueueType(QueueType.STANDARD);
        configuration.setQueueUrlPattern("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue-name");
        configuration.setDelaySeconds(0);
        configuration.setMessageAttributes(Collections.emptyMap());
        configuration.setRegion("us-east-1");
        return configuration;
    }

    public enum QueueType {
        STANDARD,
        FIFO
    }
}
