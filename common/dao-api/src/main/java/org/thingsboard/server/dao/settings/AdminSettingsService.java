// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.settings;

import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface AdminSettingsService extends EntityDaoService {

    AdminSettings findAdminSettingsById(TenantId tenantId, AdminSettingsId adminSettingsId);

    AdminSettings findAdminSettingsByKey(TenantId tenantId, String key);

    AdminSettings findAdminSettingsByTenantIdAndKey(TenantId tenantId, String key);

    PageData<AdminSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    AdminSettings saveAdminSettings(TenantId tenantId, AdminSettings adminSettings);

    boolean deleteAdminSettingsByTenantIdAndKey(TenantId tenantId, String key);

}
