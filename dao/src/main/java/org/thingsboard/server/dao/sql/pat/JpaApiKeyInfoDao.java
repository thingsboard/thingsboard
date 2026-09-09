// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
