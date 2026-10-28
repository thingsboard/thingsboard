// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.ai;

import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Optional;
import java.util.Set;

public interface AiModelDao extends TenantEntityDao<AiModel>, ExportableEntityDao<AiModelId, AiModel> {

    Optional<AiModel> findByTenantIdAndId(TenantId tenantId, AiModelId modelId);

    PageData<AiModel> findByTenantId(TenantId tenantId, Boolean customerAccessEnabled, PageLink pageLink);

    boolean deleteById(TenantId tenantId, AiModelId modelId);

    Set<AiModelId> deleteByTenantId(TenantId tenantId);

    boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId);

}
