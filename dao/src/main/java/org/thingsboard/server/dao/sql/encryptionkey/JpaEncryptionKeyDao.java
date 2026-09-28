// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.encryptionkey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.encryptionkey.EncryptionKeyDao;
import org.thingsboard.server.dao.model.sql.EncryptionKeyEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@SqlDao
@Component
public class JpaEncryptionKeyDao extends JpaAbstractDao<EncryptionKeyEntity, EncryptionKey> implements EncryptionKeyDao {

    @Autowired
    private EncryptionKeyRepository encryptionKeyRepository;

    @Override
    public EncryptionKey findByTenantId(TenantId tenantId) {
        return DaoUtil.getData(encryptionKeyRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        encryptionKeyRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    protected Class<EncryptionKeyEntity> getEntityClass() {
        return EncryptionKeyEntity.class;
    }

    @Override
    protected JpaRepository<EncryptionKeyEntity, UUID> getRepository() {
        return encryptionKeyRepository;
    }

}
