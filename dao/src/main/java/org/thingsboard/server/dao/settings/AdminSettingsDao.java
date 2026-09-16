// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.settings;

import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface AdminSettingsDao extends Dao<AdminSettings> {

    AdminSettings save(TenantId tenantId, AdminSettings adminSettings);

    AdminSettings findByTenantIdAndKey(UUID tenantId, String key);

    PageData<AdminSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    boolean removeByTenantIdAndKey(UUID tenantId, String key);

    void removeByTenantId(UUID tenantId);

}
