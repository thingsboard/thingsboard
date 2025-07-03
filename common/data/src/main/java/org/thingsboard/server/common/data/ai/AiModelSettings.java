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
package org.thingsboard.server.common.data.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.ai.model.AiModel;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoNullChar;

import java.io.Serial;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AiModelSettings extends BaseData<AiModelSettingsId> implements HasTenantId, HasVersion, ExportableEntity<AiModelSettingsId> {

    @Serial
    private static final long serialVersionUID = 9017108678716011604L;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "JSON object representing the ID of the tenant associated with these AI model settings",
            example = "e3c4b7d2-5678-4a9b-0c1d-2e3f4a5b6c7d"
    )
    private TenantId tenantId;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "Version of the AI model settings; increments automatically whenever the settings are changed",
            example = "7",
            defaultValue = "1"
    )
    private Long version;

    @NotBlank
    @NoNullChar
    @Length(min = 1, max = 255)
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Human-readable name of the AI model settings; must be unique within the scope of the tenant",
            example = "Rule node assistant"
    )
    private String name;

    @NotNull
    @Valid
    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Configuration of the AI model"
    )
    private AiModel<?> configuration;

    private AiModelSettingsId externalId;

    public AiModelSettings() {}

    public AiModelSettings(AiModelSettingsId id) {
        super(id);
    }

    public AiModelSettings(AiModelSettings settings) {
        super(settings.getId());
        createdTime = settings.getCreatedTime();
        tenantId = settings.getTenantId();
        version = settings.getVersion();
        name = settings.getName();
        configuration = settings.getConfiguration();
        externalId = settings.getExternalId() == null ? null : new AiModelSettingsId(settings.getExternalId().getId());
    }

}
