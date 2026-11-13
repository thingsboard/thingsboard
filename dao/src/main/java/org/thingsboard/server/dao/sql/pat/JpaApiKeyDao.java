// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

import org.springframework.data.domain.PageRequest;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
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
    public ApiKey findInternalByDescription(TenantId tenantId, String description) {
        return DaoUtil.getData(apiKeyRepository.findFirstByTenantIdAndDescriptionAndInternal(tenantId.getId(), description, true));
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
    public List<ApiKey> findByTenantIdAndUserId(TenantId tenantId, UserId userId) {
        return DaoUtil.convertDataList(apiKeyRepository.findByTenantIdAndUserId(tenantId.getId(), userId.getId()));
    }

    @Override
    public PageData<ApiKey> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(apiKeyRepository.findByTenantId(tenantId.getId(), PageRequest.of(pageLink.getPage(), pageLink.getPageSize())));
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
