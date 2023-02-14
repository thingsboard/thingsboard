/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.notification;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class TbSlackNodeConfiguration implements NodeConfiguration<TbSlackNodeConfiguration> {

    private String botToken;
    private boolean useDefaultNotificationSettings;
    @NotEmpty
    private String messageTemplate;

    @NotNull
    private SlackConversationType conversationType;
    private String conversationId; // if not set, need to specify conversationNamePattern
    private String conversationNamePattern;

    @Override
    public TbSlackNodeConfiguration defaultConfiguration() {
        TbSlackNodeConfiguration config = new TbSlackNodeConfiguration();
        config.setBotToken("xoxb-");
        config.setMessageTemplate("Device ${deviceId}: temperature is $[temperature]");
        config.setConversationType(SlackConversationType.PUBLIC_CHANNEL);
        return config;
    }

}
