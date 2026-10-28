// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoNullChar;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelInfo extends BaseData<AiModelId> implements TenantEntity, HasName, HasVersion {

    @Serial
    private static final long serialVersionUID = -2349470521862370137L;

    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "JSON object representing the ID of the tenant associated with this AI model",
            example = "e3c4b7d2-5678-4a9b-0c1d-2e3f4a5b6c7d"
    )
    private TenantId tenantId;

    @Schema(
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

    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Whether the model can be used via the AI model chat API by users with the 'INVOKE' permission. " +
                    "Roles that grant all operations, such as the default 'Customer Administrator' role, include 'INVOKE'",
            defaultValue = "false"
    )
    private boolean availableViaApi;

    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "AI provider, taken from the model configuration",
            example = "OPENAI"
    )
    private AiProvider provider;

    @Schema(
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "Technical model identifier used by the provider, taken from the model configuration",
            example = "gpt-4o-mini"
    )
    private String modelId;

    public AiModelInfo() {}

    public AiModelInfo(AiModelId id) {
        super(id);
    }

    public AiModelInfo(AiModelInfo modelInfo) {
        super(modelInfo);
        tenantId = modelInfo.getTenantId();
        version = modelInfo.getVersion();
        name = modelInfo.getName();
        availableViaApi = modelInfo.isAvailableViaApi();
        provider = modelInfo.getProvider();
        modelId = modelInfo.getModelId();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}
