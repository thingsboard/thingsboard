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
package org.thingsboard.server.dao.ai;

import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Optional;
import java.util.Set;

public interface AiModelDao extends TenantEntityDao<AiModel>, ExportableEntityDao<AiModelId, AiModel> {

    Optional<AiModel> findByTenantIdAndId(TenantId tenantId, AiModelId modelId);

    boolean deleteById(TenantId tenantId, AiModelId modelId);

    Set<AiModelId> deleteByTenantId(TenantId tenantId);

    boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId);

}
