/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.settings;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
@Component
@Data
public class TbAwsSqsSettings {

    @Value("${queue.aws_sqs.access_key_id}")
    private String accessKeyId;

    @Value("${queue.aws_sqs.secret_access_key}")
    private String secretAccessKey;

    @Value("${queue.aws_sqs.region}")
    private String region;

    @Value("${queue.aws_sqs.threads_per_topic}")
    private int threadsPerTopic;

}
