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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiSettingsDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AiSettingsDataValidator extends DataValidator<AiSettings> {

    private final TenantService tenantService;
    private final AiSettingsDao aiSettingsDao;

    @Override
    protected void validateCreate(TenantId tenantId, AiSettings aiSettings) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.AI_SETTINGS);
    }

    @Override
    protected AiSettings validateUpdate(TenantId tenantId, AiSettings aiSettings) {
        Optional<AiSettings> old = aiSettingsDao.findByTenantIdAndId(tenantId, aiSettings.getId());
        if (old.isEmpty()) {
            throw new DataValidationException("Cannot update non-existent AI settings!");
        }
        return old.get();
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AiSettings aiSettings) {
        // ID validation
        if (aiSettings.getId() != null) {
            if (aiSettings.getUuidId() == null) {
                throw new DataValidationException("AI settings UUID should be specified!");
            }
            if (aiSettings.getId().isNullUid()) {
                throw new DataValidationException("AI settings UUID must not be the reserved null value!");
            }
        }

        // tenant ID validation
        if (aiSettings.getTenantId() == null || aiSettings.getTenantId().getId() == null) {
            throw new DataValidationException("AI settings should be assigned to tenant!");
        }
        if (aiSettings.getTenantId().isSysTenantId()) {
            throw new DataValidationException("AI settings cannot be assigned to the system tenant!");
        }
        if (!tenantService.tenantExists(tenantId)) {
            throw new DataValidationException("AI settings reference a non-existent tenant!");
        }

        // name validation
        validateString("AI settings name", aiSettings.getName());
        if (aiSettings.getName().length() > 255) {
            throw new DataValidationException("AI settings name should be between 1 and 255 symbols!");
        }

        // provider validation
        if (aiSettings.getProvider() == null) {
            throw new DataValidationException("AI provider should be specified!");
        }

        // provider config validation
        if (aiSettings.getProviderConfig() == null) {
            throw new DataValidationException("AI provider config should be specified!");
        }
        if (aiSettings.getProviderConfig().getProvider() != aiSettings.getProvider()) {
            throw new DataValidationException("AI provider configuration should match the selected AI provider!");
        }
        validateString("AI provider API key", aiSettings.getProviderConfig().getApiKey());

        // model identifier validation
        validateString("AI model identifier", aiSettings.getModel());
        if (aiSettings.getModel().length() > 255) {
            throw new DataValidationException("AI model identifier should be between 1 and 255 symbols!");
        }

        // model config validation
        if (aiSettings.getModelConfig() != null) {
            if (!Objects.equals(aiSettings.getModelConfig().getModel(), aiSettings.getModel())) {
                throw new DataValidationException("AI model configuration should match the selected AI model!");
            }
        }
    }

}
