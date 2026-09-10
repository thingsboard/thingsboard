// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.ai;

import com.google.common.util.concurrent.FluentFuture;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Optional;

public interface AiModelService extends EntityDaoService {

    AiModel save(AiModel model);

    Optional<AiModel> findAiModelById(TenantId tenantId, AiModelId modelId);

    PageData<AiModel> findAiModelsByTenantId(TenantId tenantId, PageLink pageLink);

    Optional<AiModel> findAiModelByTenantIdAndId(TenantId tenantId, AiModelId modelId);

    FluentFuture<Optional<AiModel>> findAiModelByTenantIdAndIdAsync(TenantId tenantId, AiModelId modelId);

    boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId);

}
