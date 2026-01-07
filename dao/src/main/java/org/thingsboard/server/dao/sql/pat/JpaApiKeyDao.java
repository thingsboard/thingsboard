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
package org.thingsboard.server.dao.sql.pat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ApiKeyEntity;
import org.thingsboard.server.dao.pat.ApiKeyDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Set;
import java.util.UUID;

@Slf4j
@SqlDao
@Component
public class JpaApiKeyDao extends JpaAbstractDao<ApiKeyEntity, ApiKey> implements ApiKeyDao {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Override
    public ApiKey findByValue(String value) {
        return DaoUtil.getData(apiKeyRepository.findByValue(value));
    }

    @Override
    public Set<String> deleteByTenantId(TenantId tenantId) {
        return apiKeyRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public Set<String> deleteByUserId(TenantId tenantId, UserId userId) {
        return apiKeyRepository.deleteByUserId(tenantId.getId(), userId.getId());
    }

    @Override
    public int deleteAllByExpirationTimeBefore(long ts) {
        return apiKeyRepository.deleteAllByExpirationTimeBefore(ts);
    }

    @Override
    protected Class<ApiKeyEntity> getEntityClass() {
        return ApiKeyEntity.class;
    }

    @Override
    protected JpaRepository<ApiKeyEntity, UUID> getRepository() {
        return apiKeyRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}
