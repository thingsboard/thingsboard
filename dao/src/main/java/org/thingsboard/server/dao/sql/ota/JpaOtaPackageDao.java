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
package org.thingsboard.server.dao.sql.ota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.OtaPackageEntity;
import org.thingsboard.server.dao.ota.OtaPackageDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaOtaPackageDao extends JpaAbstractDao<OtaPackageEntity, OtaPackage> implements OtaPackageDao, TenantEntityDao<OtaPackage> {

    @Autowired
    private OtaPackageRepository otaPackageRepository;

    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @Transactional
    @Override
    public OtaPackage findOtaPackageByTenantIdAndTitleAndVersion(TenantId tenantId, String title, String version) {
        return DaoUtil.getData(otaPackageRepository.findByTenantIdAndTitleAndVersion(tenantId.getId(), title, version));
    }

    @Transactional
    @Override
    public PageData<OtaPackage> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(otaPackageRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Transactional
    @Override
    public PageData<OtaPackage> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAllByTenantId(TenantId.fromUUID(tenantId), pageLink);
    }

    @Override
    public PageData<OtaPackageId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(otaPackageRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink)).map(OtaPackageId::new));
    }

    @Transactional
    @Override
    public OtaPackage findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(otaPackageRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public OtaPackageId getExternalIdByInternal(OtaPackageId internalId) {
        return DaoUtil.toEntityId(otaPackageRepository.getExternalIdById(internalId.getId()), OtaPackageId::new);
    }

    @Override
    protected Class<OtaPackageEntity> getEntityClass() {
        return OtaPackageEntity.class;
    }

    @Override
    protected JpaRepository<OtaPackageEntity, UUID> getRepository() {
        return otaPackageRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
