/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.sql.OtaPackageEntity;
import org.thingsboard.server.dao.ota.OtaPackageDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaOtaPackageDao extends JpaAbstractDao<OtaPackageEntity, OtaPackage> implements OtaPackageDao {

    @Autowired
    private OtaPackageRepository otaPackageRepository;

    @Override
    protected Class<OtaPackageEntity> getEntityClass() {
        return OtaPackageEntity.class;
    }

    @Override
    protected JpaRepository<OtaPackageEntity, UUID> getRepository() {
        return otaPackageRepository;
    }

    @Override
    public Long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageRepository.sumDataSizeByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}
