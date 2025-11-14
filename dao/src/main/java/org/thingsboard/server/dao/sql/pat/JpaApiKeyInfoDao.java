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
package org.thingsboard.server.dao.sql.pat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ApiKeyInfoEntity;
import org.thingsboard.server.dao.pat.ApiKeyInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@SqlDao
@Component
public class JpaApiKeyInfoDao extends JpaAbstractDao<ApiKeyInfoEntity, ApiKeyInfo> implements ApiKeyInfoDao {

    @Autowired
    private ApiKeyInfoRepository apiKeyInfoRepository;

    @Override
    public PageData<ApiKeyInfo> findByUserId(TenantId tenantId, UserId userId, PageLink pageLink) {
        return DaoUtil.toPageData(apiKeyInfoRepository.findByUserId(tenantId.getId(), userId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<ApiKeyInfoEntity> getEntityClass() {
        return ApiKeyInfoEntity.class;
    }

    @Override
    protected JpaRepository<ApiKeyInfoEntity, UUID> getRepository() {
        return apiKeyInfoRepository;
    }

}
