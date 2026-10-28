// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.ai;

import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

public interface AiModelInfoDao extends Dao<AiModelInfo> {

    PageData<AiModelInfo> findByTenantId(TenantId tenantId, Boolean customerAccessEnabled, PageLink pageLink);

}
