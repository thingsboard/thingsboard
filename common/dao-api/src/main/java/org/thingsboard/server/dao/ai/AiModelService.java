// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.ai;

import com.google.common.util.concurrent.FluentFuture;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Optional;

public interface AiModelService extends EntityDaoService {

    AiModel save(AiModel model);

    AiModel save(AiModel model, boolean doValidate);

    Optional<AiModel> findAiModelById(TenantId tenantId, AiModelId modelId);

    PageData<AiModel> findAiModelsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AiModel> findAiModelsByTenantId(TenantId tenantId, Boolean availableViaApi, PageLink pageLink);

    PageData<AiModelInfo> findAiModelInfosByTenantId(TenantId tenantId, Boolean availableViaApi, PageLink pageLink);

    Optional<AiModel> findAiModelByTenantIdAndId(TenantId tenantId, AiModelId modelId);

    FluentFuture<Optional<AiModel>> findAiModelByTenantIdAndIdAsync(TenantId tenantId, AiModelId modelId);

    Optional<AiModel> findAiModelByTenantIdAndName(TenantId tenantId, String name);

    boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId);

}
