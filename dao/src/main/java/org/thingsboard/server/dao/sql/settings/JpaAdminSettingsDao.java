// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
