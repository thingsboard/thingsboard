/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.slack;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiRequest;
import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.ConversationType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.dao.notification.NotificationSettingsService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultSlackService implements SlackService {

    private final NotificationSettingsService notificationSettingsService;

    private final Slack slack = Slack.getInstance();
    private final Cache<String, List<SlackConversation>> cache = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();
    private static final int CONVERSATIONS_LIMIT = 1000;

    @Override
    public void sendMessage(TenantId tenantId, String token, String conversationId, String message) {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(conversationId)
                .text(message)
                .build();
        sendRequest(token, request, MethodsClient::chatPostMessage);
    }

    @Override
    public List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType) {
        return cache.get(conversationType + ":" + token, k -> {
            if (conversationType == SlackConversationType.DIRECT) {
                UsersListRequest request = UsersListRequest.builder()
                        .limit(CONVERSATIONS_LIMIT)
                        .build();

                UsersListResponse response = sendRequest(token, request, MethodsClient::usersList);
                return response.getMembers().stream()
                        .filter(user -> !user.isDeleted() && !user.isStranger() && !user.isBot())
                        .map(user -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setId(user.getId());
                            conversation.setName(String.format("@%s (%s)", user.getName(), user.getRealName()));
                            return conversation;
                        })
                        .collect(Collectors.toList());
            } else {
                ConversationsListRequest request = ConversationsListRequest.builder()
                        .types(List.of(conversationType == SlackConversationType.PUBLIC_CHANNEL ?
                                ConversationType.PUBLIC_CHANNEL :
                                ConversationType.PRIVATE_CHANNEL))
                        .limit(CONVERSATIONS_LIMIT)
                        .excludeArchived(true)
                        .build();

                ConversationsListResponse response = sendRequest(token, request, MethodsClient::conversationsList);
                return response.getChannels().stream()
                        .filter(channel -> !channel.isArchived())
                        .map(channel -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setId(channel.getId());
                            conversation.setName("#" + channel.getName());
                            return conversation;
                        })
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    public SlackConversation findConversation(TenantId tenantId, String token, SlackConversationType conversationType, String namePattern) {
        List<SlackConversation> conversations = listConversations(tenantId, token, conversationType);
        return conversations.stream()
                .filter(conversation -> StringUtils.containsIgnoreCase(conversation.getName(), namePattern))
                .findFirst().orElse(null);
    }

    @Override
    public String getToken(TenantId tenantId) {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        if (slackConfig != null) {
            return slackConfig.getBotToken();
        } else {
            return null;
        }
    }

    private <T extends SlackApiRequest, R extends SlackApiTextResponse> R sendRequest(String token, T request, ThrowingBiFunction<MethodsClient, T, R> method) {
        MethodsClient client = slack.methods(token);
        R response;
        try {
            response = method.apply(client, request);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (!response.isOk()) {
            String error = response.getError();
            if (error == null) {
                error = "unknown error";
            }
            if (error.contains("missing_scope")) {
                String neededScope = response.getNeeded();
                throw new RuntimeException("Bot token scope '" + neededScope + "' is needed");
            }
            throw new RuntimeException("Failed to send message via Slack: " + error);
        }

        return response;
    }

}
