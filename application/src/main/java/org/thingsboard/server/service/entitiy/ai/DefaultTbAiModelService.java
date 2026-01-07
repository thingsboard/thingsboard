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
package org.thingsboard.server.service.entitiy.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static java.util.Objects.requireNonNullElseGet;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultTbAiModelService extends AbstractTbEntityService implements TbAiModelService {

    private final AiModelService aiModelService;

    @Override
    public AiModel save(AiModel model, User user) {
        var actionType = model.getId() == null ? ActionType.ADDED : ActionType.UPDATED;

        var tenantId = user.getTenantId();
        model.setTenantId(tenantId);

        AiModel savedModel;
        try {
            savedModel = aiModelService.save(model);
            autoCommit(user, savedModel.getId());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, requireNonNullElseGet(model.getId(), () -> emptyId(EntityType.AI_MODEL)), model, actionType, user, e);
            throw e;
        }

        logEntityActionService.logEntityAction(tenantId, savedModel.getId(), savedModel, actionType, user);

        return savedModel;
    }

    @Override
    public boolean delete(AiModel model, User user) {
        var actionType = ActionType.DELETED;

        var tenantId = user.getTenantId();
        var modelId = model.getId();

        boolean deleted;
        try {
            deleted = aiModelService.deleteByTenantIdAndId(tenantId, modelId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, modelId, model, actionType, user, e, modelId.toString());
            throw e;
        }

        if (deleted) {
            logEntityActionService.logEntityAction(tenantId, modelId, model, actionType, user, modelId.toString());
        }

        return deleted;
    }

}
