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
package org.thingsboard.server.dao.sql.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.AdminSettingsEntity;
import org.thingsboard.server.dao.settings.AdminSettingsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaAdminSettingsDao extends JpaAbstractDao<AdminSettingsEntity, AdminSettings> implements AdminSettingsDao, TenantEntityDao<AdminSettings> {

    private final AdminSettingsRepository adminSettingsRepository;

    @Override
    public AdminSettings findByTenantIdAndKey(UUID tenantId, String key) {
        return DaoUtil.getData(adminSettingsRepository.findByTenantIdAndKey(tenantId, key));
    }

    @Override
    @Transactional
    public boolean removeByTenantIdAndKey(UUID tenantId, String key) {
        if (adminSettingsRepository.existsByTenantIdAndKey(tenantId, key)) {
            adminSettingsRepository.deleteByTenantIdAndKey(tenantId, key);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void removeByTenantId(UUID tenantId) {
        adminSettingsRepository.deleteByTenantId(tenantId);
    }

    @Override
    public PageData<AdminSettings> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(adminSettingsRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<AdminSettingsEntity> getEntityClass() {
        return AdminSettingsEntity.class;
    }

    @Override
    protected JpaRepository<AdminSettingsEntity, UUID> getRepository() {
        return adminSettingsRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ADMIN_SETTINGS;
    }

}
