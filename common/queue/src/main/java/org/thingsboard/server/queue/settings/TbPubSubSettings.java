/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
@Component
@Data
public class TbPubSubSettings {

    @Value("${queue.pubsub.project_id}")
    private String projectId;

    @Value("${queue.pubsub.service_account}")
    private String serviceAccount;

    @Value("${queue.pubsub.max_msg_size}")
    private int maxMsgSize;

    @Value("${queue.pubsub.max_messages}")
    private int maxMessages;

    private CredentialsProvider credentialsProvider;

    @PostConstruct
    private void init() throws IOException {
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(serviceAccount.getBytes()));
        credentialsProvider = FixedCredentialsProvider.create(credentials);
    }

}
