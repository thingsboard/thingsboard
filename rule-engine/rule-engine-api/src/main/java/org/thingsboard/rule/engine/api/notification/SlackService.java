// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.notification;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.notification.targets.slack.SlackFile;

import java.util.List;

public interface SlackService {

    void sendMessage(TenantId tenantId, String token, String conversationId, String message);

    void sendMessage(TenantId tenantId, String token, String conversationId, String message, List<SlackFile> files);

    List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType);

    String getToken(TenantId tenantId);

}
