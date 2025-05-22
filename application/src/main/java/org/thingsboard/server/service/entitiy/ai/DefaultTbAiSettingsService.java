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
package org.thingsboard.server.service.entitiy.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.ai.AiSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static java.util.Objects.requireNonNullElseGet;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultTbAiSettingsService extends AbstractTbEntityService implements TbAiSettingsService {

    private final AiSettingsService aiSettingsService;

    @Override
    public AiSettings save(AiSettings aiSettings, User user) {
        var actionType = aiSettings.getId() == null ? ActionType.ADDED : ActionType.UPDATED;

        var tenantId = user.getTenantId();
        aiSettings.setTenantId(tenantId);

        AiSettings savedSettings;
        try {
            savedSettings = aiSettingsService.save(aiSettings);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, requireNonNullElseGet(aiSettings.getId(), () -> emptyId(EntityType.AI_SETTINGS)), aiSettings, actionType, user, e);
            throw e;
        }

        logEntityActionService.logEntityAction(tenantId, savedSettings.getId(), savedSettings, actionType, user);

        return savedSettings;
    }

    @Override
    public boolean delete(AiSettings aiSettings, User user) {
        var actionType = ActionType.DELETED;

        var tenantId = user.getTenantId();
        var aiSettingsId = aiSettings.getId();

        boolean deleted;
        try {
            deleted = aiSettingsService.deleteByTenantIdAndId(tenantId, aiSettingsId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, aiSettingsId, aiSettings, actionType, user, e, aiSettingsId.toString());
            throw e;
        }

        if (deleted) {
            logEntityActionService.logEntityAction(tenantId, aiSettingsId, aiSettings, actionType, user, aiSettingsId.toString());
        }

        return deleted;
    }

}
