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

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.ConversationType;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultSlackService implements SlackService {

    private final Slack slack = Slack.getInstance();

    @Override
    public void sendMessage(TenantId tenantId, String token, String conversationId, String message) throws Exception {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(conversationId)
                .text(message)
                .build();
        ChatPostMessageResponse response = slack.methods(token).chatPostMessage(request);
        check(response);
    }

    @Override
    public List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversation.Type conversationType) throws Exception {
        MethodsClient methods = slack.methods(token);
        if (conversationType == SlackConversation.Type.USER) {
            UsersListResponse usersListResponse = methods.usersList(UsersListRequest.builder()
                    .limit(1000)
                    .build());
            check(usersListResponse);
            return usersListResponse.getMembers().stream()
                    .filter(user -> !user.isDeleted() && !user.isStranger() && !user.isBot())
                    .map(user -> {
                        SlackConversation conversation = new SlackConversation();
                        conversation.setId(user.getId());
                        conversation.setName(String.format("@%s (%s)", user.getName(), user.getRealName()));
                        return conversation;
                    })
                    .collect(Collectors.toList());
        } else {
            ConversationsListResponse conversationsListResponse = methods.conversationsList(ConversationsListRequest.builder()
                    .types(List.of(conversationType == SlackConversation.Type.PUBLIC_CHANNEL ?
                            ConversationType.PUBLIC_CHANNEL :
                            ConversationType.PRIVATE_CHANNEL))
                    .limit(1000)
                    .excludeArchived(true)
                    .build());
            check(conversationsListResponse);
            return conversationsListResponse.getChannels().stream()
                    .filter(channel -> !channel.isArchived())
                    .map(channel -> {
                        SlackConversation conversation = new SlackConversation();
                        conversation.setId(channel.getId());
                        conversation.setName("#" + channel.getName());
                        return conversation;
                    })
                    .collect(Collectors.toList());
        }
    }

    private void check(SlackApiTextResponse slackResponse) {
        if (!slackResponse.isOk()) {
            String error = slackResponse.getError();
            if (error == null) {
                error = "unknown error";
            }
            if (error.contains("missing_scope")) {
                String neededScope = slackResponse.getNeeded();
                throw new RuntimeException("Bot token scope '" + neededScope + "' is needed");
            }
            throw new RuntimeException("Failed to send message via Slack: " + error);
        }
    }

}
