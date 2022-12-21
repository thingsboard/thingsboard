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
package org.thingsboard.rule.engine.api.slack;

import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface SlackService {

    void sendMessage(TenantId tenantId, String token, String conversationId, String message);

    List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversation.Type conversationType);

    SlackConversation findConversation(TenantId tenantId, String token, SlackConversation.Type conversationType, String namePattern);

    String getToken(TenantId tenantId);

}
