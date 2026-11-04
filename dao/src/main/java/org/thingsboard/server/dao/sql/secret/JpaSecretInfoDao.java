// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.secret;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SecretInfoEntity;
import org.thingsboard.server.dao.secret.SecretInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@SqlDao
@Component
public class JpaSecretInfoDao extends JpaAbstractDao<SecretInfoEntity, SecretInfo> implements SecretInfoDao {

    @Autowired
    private SecretInfoRepository secretInfoRepository;

    @Override
    public PageData<SecretInfo> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(secretInfoRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public SecretInfo findByName(TenantId tenantId, String name) {
        return DaoUtil.getData(secretInfoRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public List<String> findAllNamesByTenantId(TenantId tenantId) {
        return secretInfoRepository.findAllNamesByTenantId(tenantId.getId());
    }

    @Override
    protected Class<SecretInfoEntity> getEntityClass() {
        return SecretInfoEntity.class;
    }

    @Override
    protected JpaRepository<SecretInfoEntity, UUID> getRepository() {
        return secretInfoRepository;
    }

}
