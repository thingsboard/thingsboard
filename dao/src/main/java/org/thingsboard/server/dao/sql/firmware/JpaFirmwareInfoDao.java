/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.firmware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.firmware.FirmwareInfoDao;
import org.thingsboard.server.dao.model.sql.FirmwareInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class JpaFirmwareInfoDao extends JpaAbstractSearchTextDao<FirmwareInfoEntity, FirmwareInfo> implements FirmwareInfoDao {

    @Autowired
    private FirmwareInfoRepository firmwareInfoRepository;

    @Override
    protected Class<FirmwareInfoEntity> getEntityClass() {
        return FirmwareInfoEntity.class;
    }

    @Override
    protected CrudRepository<FirmwareInfoEntity, UUID> getCrudRepository() {
        return firmwareInfoRepository;
    }

    @Override
    public PageData<FirmwareInfo> findFirmwareInfoByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(firmwareInfoRepository
                .findAllByTenantId(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }
}
