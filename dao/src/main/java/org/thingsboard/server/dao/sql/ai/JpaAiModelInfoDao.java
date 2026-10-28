// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ai;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.ai.AiModelInfoDao;
import org.thingsboard.server.dao.model.sql.AiModelInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaAiModelInfoDao extends JpaAbstractDao<AiModelInfoEntity, AiModelInfo> implements AiModelInfoDao {

    private final AiModelInfoRepository aiModelInfoRepository;

    @Override
    public PageData<AiModelInfo> findByTenantId(TenantId tenantId, Boolean availableViaApi, PageLink pageLink) {
        return DaoUtil.toPageData(aiModelInfoRepository.findByTenantId(
                tenantId.getId(), availableViaApi, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), DaoUtil.toPageable(pageLink))
        );
    }

    @Override
    protected Class<AiModelInfoEntity> getEntityClass() {
        return AiModelInfoEntity.class;
    }

    @Override
    protected JpaRepository<AiModelInfoEntity, UUID> getRepository() {
        return aiModelInfoRepository;
    }

}
