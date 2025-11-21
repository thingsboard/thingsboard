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
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoNullChar;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AiModel extends BaseData<AiModelId> implements HasTenantId, HasVersion, ExportableEntity<AiModelId> {

    @Serial
    private static final long serialVersionUID = 9017108678716011604L;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "JSON object representing the ID of the tenant associated with this AI model",
            example = "e3c4b7d2-5678-4a9b-0c1d-2e3f4a5b6c7d"
    )
    private TenantId tenantId;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "Version of the AI model record; increments automatically whenever the record is changed",
            example = "7",
            defaultValue = "1"
    )
    private Long version;

    @NotBlank
    @NoNullChar
    @Length(min = 1, max = 255)
    @NoXss
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Display name for this AI model configuration; not the technical model identifier",
            example = "Fast and cost-efficient model"
    )
    private String name;

    @NotNull
    @Valid
    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Configuration of the AI model"
    )
    private AiModelConfig configuration;

    private AiModelId externalId;

    public AiModel() {}

    public AiModel(AiModelId id) {
        super(id);
    }

    public AiModel(AiModel model) {
        super(model.getId());
        createdTime = model.getCreatedTime();
        tenantId = model.getTenantId();
        version = model.getVersion();
        name = model.getName();
        configuration = model.getConfiguration();
        externalId = model.getExternalId() == null ? null : new AiModelId(model.getExternalId().getId());
    }

}
