/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

    AiModel save(AiModel model, boolean doValidate);

    Optional<AiModel> findAiModelById(TenantId tenantId, AiModelId modelId);

    PageData<AiModel> findAiModelsByTenantId(TenantId tenantId, PageLink pageLink);

    Optional<AiModel> findAiModelByTenantIdAndId(TenantId tenantId, AiModelId modelId);

    FluentFuture<Optional<AiModel>> findAiModelByTenantIdAndIdAsync(TenantId tenantId, AiModelId modelId);

    Optional<AiModel> findAiModelByTenantIdAndName(TenantId tenantId, String name);

    boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId);

}
