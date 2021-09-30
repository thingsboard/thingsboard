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
package org.thingsboard.server.channels;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.TransportInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@ConditionalOnProperty(
        value="notifications.slack.enabled",
        havingValue = "true")
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    @Value("${notifications.slack.access_token}")
    private String token;

    @Value("${notifications.slack.channel_id}")
    private String channelId;

    @Override
    public void onTransportUnavailable(TransportInfo transportInfo) {
        try {
            Slack slack = Slack.getInstance();
            String localDate = LocalDate.now().toString();
            String localTime = LocalTime.now().toString();
            String markdownTemplate = "%s | %s | *%s* | %s";

            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(String.format(markdownTemplate, localDate, localTime, transportInfo.getTransportType().name(), transportInfo.getInformation()))
                    .build());
            if (!response.getErrors().isEmpty()) {
                log.error(response.getErrors().toString());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }
}
