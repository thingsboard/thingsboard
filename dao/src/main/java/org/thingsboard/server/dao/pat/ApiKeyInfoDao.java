// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.Dao;

public interface ApiKeyInfoDao extends Dao<ApiKeyInfo> {

    PageData<ApiKeyInfo> findByUserId(TenantId tenantId, UserId userId, PageLink pageLink);

}
