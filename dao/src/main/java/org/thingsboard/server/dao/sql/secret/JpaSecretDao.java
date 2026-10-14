// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.secret;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SecretEntity;
import org.thingsboard.server.dao.secret.SecretDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@SqlDao
@Component
public class JpaSecretDao extends JpaAbstractDao<SecretEntity, Secret> implements SecretDao {

    @Autowired
    private SecretRepository secretRepository;

    @Override
    public PageData<Secret> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(secretRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Secret findByName(TenantId tenantId, String name) {
        return DaoUtil.getData(secretRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        secretRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public Map<String, Long> countSecretsPerType() {
        return secretRepository.countSecretsPerType().stream().collect(Collectors.toMap(e -> e.getFirst().name(), TbPair::getSecond));
    }

    @Override
    protected Class<SecretEntity> getEntityClass() {
        return SecretEntity.class;
    }

    @Override
    protected JpaRepository<SecretEntity, UUID> getRepository() {
        return secretRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
