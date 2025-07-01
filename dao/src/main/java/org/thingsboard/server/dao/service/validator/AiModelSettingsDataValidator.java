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
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiModelSettingsDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AiModelSettingsDataValidator extends DataValidator<AiModelSettings> {

    private final TenantService tenantService;
    private final AiModelSettingsDao aiModelSettingsDao;

    @Override
    protected AiModelSettings validateUpdate(TenantId tenantId, AiModelSettings settings) {
        Optional<AiModelSettings> existing = aiModelSettingsDao.findByTenantIdAndId(tenantId, settings.getId());
        if (existing.isEmpty()) {
            throw new DataValidationException("Cannot update non-existent AI model settings!");
        }
        return existing.get();
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AiModelSettings settings) {
        // ID validation
        if (settings.getId() != null) {
            if (settings.getUuidId() == null) {
                throw new DataValidationException("AI model settings UUID should be specified!");
            }
            if (settings.getId().isNullUid()) {
                throw new DataValidationException("AI model settings UUID must not be the reserved null value!");
            }
        }

        // tenant ID validation
        if (settings.getTenantId() == null || settings.getTenantId().getId() == null) {
            throw new DataValidationException("AI model settings should be assigned to tenant!");
        }
        if (settings.getTenantId().isSysTenantId()) {
            throw new DataValidationException("AI model settings cannot be assigned to the system tenant!");
        }
        if (!tenantService.tenantExists(tenantId)) {
            throw new DataValidationException("AI model settings reference a non-existent tenant!");
        }

        // name validation
        validateString("AI model settings name", settings.getName());
        if (settings.getName().length() > 255) {
            throw new DataValidationException("AI model settings name should be between 1 and 255 symbols!");
        }

        // model config validation
        if (settings.getConfiguration() == null) {
            throw new DataValidationException("AI model settings configuration should be specified!");
        }
    }

}
