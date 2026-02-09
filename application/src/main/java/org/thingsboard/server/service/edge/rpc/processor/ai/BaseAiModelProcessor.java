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
package org.thingsboard.server.service.edge.rpc.processor.ai;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Optional;

@Slf4j
public abstract class BaseAiModelProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<AiModel> aiModelValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateAiModel(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg) {
        boolean isCreated = false;
        boolean isNameUpdated = false;
        try {
            AiModel aiModel = JacksonUtil.fromString(aiModelUpdateMsg.getEntity(), AiModel.class, true);
            if (aiModel == null) {
                throw new RuntimeException("[{" + tenantId + "}] aiModelUpdateMsg {" + aiModelUpdateMsg + " } cannot be converted to aiModel");
            }

            Optional<AiModel> aiModelById = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            if (aiModelById.isEmpty()) {
                aiModel.setCreatedTime(Uuids.unixTimestamp(aiModelId.getId()));
                isCreated = true;
                aiModel.setId(null);
            } else {
                aiModel.setId(aiModelId);
            }

            String aiModelName = aiModel.getName();
            Optional<AiModel> aiModelByName = edgeCtx.getAiModelService().findAiModelByTenantIdAndName(aiModel.getTenantId(), aiModelName);
            if (aiModelByName.isPresent() && !aiModelByName.get().getId().equals(aiModelId)) {
                aiModelName = aiModelName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] aiModel with name {} already exists. Renaming aiModel name to {}",
                        tenantId, aiModel.getName(), aiModelByName.get().getName());
                isNameUpdated = true;
            }
            aiModel.setName(aiModelName);

            aiModelValidator.validate(aiModel, AiModel::getTenantId);

            if (isCreated) {
                aiModel.setId(aiModelId);
            }

            edgeCtx.getAiModelService().save(aiModel, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process aiModel update msg [{}]", tenantId, aiModelUpdateMsg, e);
            throw e;
        }
        return Pair.of(isCreated, isNameUpdated);
    }

    protected void deleteAiModel(TenantId tenantId, Edge edge, AiModelId aiModelId) {
        Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
        if (aiModel.isPresent()) {
            edgeCtx.getAiModelService().deleteByTenantIdAndId(tenantId, aiModelId);
            pushEntityEventToRuleEngine(tenantId, edge, aiModel.get(), TbMsgType.ENTITY_DELETED);
        }
    }

}
