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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AiSettings extends BaseData<AiSettingsId> implements HasTenantId, HasVersion, HasName {

    @Serial
    private static final long serialVersionUID = 9017108678716011604L;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "JSON object representing the ID of the tenant associated with these AI settings",
            example = "e3c4b7d2-5678-4a9b-0c1d-2e3f4a5b6c7d"
    )
    TenantId tenantId;

    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "Version of the AI settings; increments automatically whenever the settings are changed",
            example = "7",
            defaultValue = "1"
    )
    Long version;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Human-readable name of the AI settings",
            example = "Default AI Settings"
    )
    String name;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Name of the LLM provider, e.g. 'openai', 'anthropic'",
            example = "openai"
    )
    String provider;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Identifier of the LLM model to use, e.g. 'gpt-4o-mini'",
            example = "gpt-4o-mini"
    )
    String model;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "API key for authenticating with the selected LLM provider",
            example = "sk-********************************"
    )
    String apiKey;

    public AiSettings(AiSettingsId id) {
        super(id);
    }

}
