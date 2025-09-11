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
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiModelDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AiModelDataValidator extends DataValidator<AiModel> {

    private final TenantService tenantService;
    private final AiModelDao aiModelDao;

    @Override
    protected AiModel validateUpdate(TenantId tenantId, AiModel model) {
        Optional<AiModel> existing = aiModelDao.findByTenantIdAndId(tenantId, model.getId());
        if (existing.isEmpty()) {
            throw new DataValidationException("Cannot update non-existent AI model!");
        }
        return existing.get();
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AiModel model) {
        // ID validation
        if (model.getId() != null) {
            if (model.getUuidId() == null) {
                throw new DataValidationException("AI model UUID should be specified!");
            }
            if (model.getId().isNullUid()) {
                throw new DataValidationException("AI model UUID must not be the reserved null value!");
            }
        }

        // tenant ID validation
        if (model.getTenantId() == null || model.getTenantId().getId() == null) {
            throw new DataValidationException("AI model should be assigned to tenant!");
        }
        if (model.getTenantId().isSysTenantId()) {
            throw new DataValidationException("AI model cannot be assigned to the system tenant!");
        }
        if (!tenantService.tenantExists(tenantId)) {
            throw new DataValidationException("AI model reference a non-existent tenant!");
        }
    }

}
