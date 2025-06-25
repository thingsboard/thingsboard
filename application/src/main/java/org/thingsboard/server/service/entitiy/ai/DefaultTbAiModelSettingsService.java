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
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.ai.AiModelSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static java.util.Objects.requireNonNullElseGet;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultTbAiModelSettingsService extends AbstractTbEntityService implements TbAiModelSettingsService {

    private final AiModelSettingsService aiModelSettingsService;

    @Override
    public AiModelSettings save(AiModelSettings settings, User user) {
        var actionType = settings.getId() == null ? ActionType.ADDED : ActionType.UPDATED;

        var tenantId = user.getTenantId();
        settings.setTenantId(tenantId);

        AiModelSettings savedSettings;
        try {
            savedSettings = aiModelSettingsService.save(settings);
            autoCommit(user, savedSettings.getId());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, requireNonNullElseGet(settings.getId(), () -> emptyId(EntityType.AI_MODEL_SETTINGS)), settings, actionType, user, e);
            throw e;
        }

        logEntityActionService.logEntityAction(tenantId, savedSettings.getId(), savedSettings, actionType, user);

        return savedSettings;
    }

    @Override
    public boolean delete(AiModelSettings settings, User user) {
        var actionType = ActionType.DELETED;

        var tenantId = user.getTenantId();
        var settingsId = settings.getId();

        boolean deleted;
        try {
            deleted = aiModelSettingsService.deleteByTenantIdAndId(tenantId, settingsId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, settingsId, settings, actionType, user, e, settingsId.toString());
            throw e;
        }

        if (deleted) {
            logEntityActionService.logEntityAction(tenantId, settingsId, settings, actionType, user, settingsId.toString());
        }

        return deleted;
    }

}
