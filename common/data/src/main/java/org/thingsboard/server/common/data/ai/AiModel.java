// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public final class AiModel extends AiModelInfo implements ExportableEntity<AiModelId> {

    @Serial
    private static final long serialVersionUID = 9017108678716011604L;

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

    @Builder
    public AiModel(TenantId tenantId, Long version, String name, AiModelConfig configuration, boolean customerAccessEnabled, AiModelId externalId) {
        setTenantId(tenantId);
        setVersion(version);
        setName(name);
        setCustomerAccessEnabled(customerAccessEnabled);
        this.configuration = configuration;
        this.externalId = externalId;
    }

    public AiModel(AiModel model) {
        super(model);
        configuration = model.getConfiguration();
        externalId = model.getExternalId() == null ? null : new AiModelId(model.getExternalId().getId());
    }

    @Override
    @JsonIgnore
    public AiProvider getProvider() {
        return configuration != null ? configuration.provider() : null;
    }

    @Override
    @JsonIgnore
    public String getModelId() {
        return configuration instanceof AiChatModelConfig<?> chatModelConfig ? chatModelConfig.modelId() : null;
    }

}
