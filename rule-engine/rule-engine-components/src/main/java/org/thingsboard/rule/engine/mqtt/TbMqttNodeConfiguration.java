/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.mqtt;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;

@Data
public class TbMqttNodeConfiguration implements NodeConfiguration<TbMqttNodeConfiguration> {

    private String topicPattern;
    private String host;
    private int port;
    private int connectTimeoutSec;
    private String clientId;
    private boolean appendClientIdSuffix;
    private boolean retainedMessage;

    private boolean cleanSession;
    private boolean ssl;
    private boolean parseToPlainText;
    private ClientCredentials credentials;

    @Override
    public TbMqttNodeConfiguration defaultConfiguration() {
        TbMqttNodeConfiguration configuration = new TbMqttNodeConfiguration();
        configuration.setTopicPattern("my-topic");
        configuration.setPort(1883);
        configuration.setConnectTimeoutSec(10);
        configuration.setCleanSession(true);
        configuration.setSsl(false);
        configuration.setRetainedMessage(false);
        configuration.setParseToPlainText(false);
        configuration.setCredentials(new AnonymousCredentials());
        return configuration;
    }

}
