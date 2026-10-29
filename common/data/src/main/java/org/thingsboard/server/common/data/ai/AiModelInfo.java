// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema(description = "AI model with the same JSON shape as an AI model, except that 'configuration.providerConfig' is always null, " +
        "so provider credentials are never exposed")
public record AiModelInfo(
        AiModelId id,
        long createdTime,
        TenantId tenantId,
        Long version,
        String name,
        boolean customerAccessEnabled,
        AiModelConfig configuration,
        AiModelId externalId
) {

    public static AiModelInfo from(AiModel model) {
        return new AiModelInfo(model.getId(), model.getCreatedTime(), model.getTenantId(), model.getVersion(),
                model.getName(), model.isCustomerAccessEnabled(), model.getConfiguration().withoutProviderConfig(),
                model.getExternalId());
    }

}
