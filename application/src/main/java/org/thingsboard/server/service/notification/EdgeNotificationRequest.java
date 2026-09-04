/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes a notification delivery that the Edge delegates to the Cloud, for channels whose credentials
 * live only on the Cloud (they are stored in the {@code notifications} admin settings that no longer sync
 * to the Edge). {@code method} selects the channel:
 * <ul>
 *     <li>{@code SEND_SLACK} - the Cloud resolves the Slack bot token and posts {@code message} (plus any
 *     {@code files}) to {@code conversationId}. The whole send is delegated.</li>
 *     <li>{@code SEND_MOBILE_PUSH} - the Edge keeps the notification record, device-token lookup and unread
 *     count locally and delegates only the FCM push: the Cloud resolves the Firebase credentials and pushes
 *     the already-built payload ({@code subject}/{@code body}/{@code data}/{@code badge}) to every
 *     {@code fcmToken}.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeNotificationRequest {

    public enum NotificationMethod {
        SEND_SLACK,
        SEND_MOBILE_PUSH
    }

    private NotificationMethod method;

    // SEND_SLACK
    private String conversationId;
    private String message;
    private List<SlackFileData> files;

    // SEND_MOBILE_PUSH
    private Set<String> fcmTokens;
    private String subject;
    private String body;
    private Map<String, String> data;
    private Integer badge;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlackFileData {
        private String name;
        private String type;
        private byte[] data;
    }

}
