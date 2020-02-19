/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.provisionprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ProvisionProfileEntity;
import org.thingsboard.server.dao.provisionprofile.ProvisionProfileDao;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionProfile;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

@Component
@SqlDao
public class JpaProvisionProfileDao extends JpaAbstractSearchTextDao<ProvisionProfileEntity, ProvisionProfile> implements ProvisionProfileDao {

    @Autowired
    private ProvisionProfileRepository provisionProfileRepository;

    @Override
    public ProvisionProfile findByKey(TenantId tenantId, String key) {
        return DaoUtil.getData(provisionProfileRepository.findByKey(key));
    }

    @Override
    public List<ProvisionProfile> findProfilesByTenantId(UUID tenantId, TextPageLink pageLink) {
        if (StringUtils.isEmpty(pageLink.getTextSearch())) {
            return DaoUtil.convertDataList(
                    provisionProfileRepository.findByTenantId(
                            fromTimeUUID(tenantId),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        } else {
            return DaoUtil.convertDataList(
                    provisionProfileRepository.findByTenantId(
                            fromTimeUUID(tenantId),
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        }
    }

    @Override
    protected Class<ProvisionProfileEntity> getEntityClass() {
        return ProvisionProfileEntity.class;
    }

    @Override
    protected CrudRepository<ProvisionProfileEntity, String> getCrudRepository() {
        return provisionProfileRepository;
    }
}
