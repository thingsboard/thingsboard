// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.mobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.mobile.QrCodeSettingsDao;
import org.thingsboard.server.dao.model.sql.QrCodeSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;


@Component
@Slf4j
@SqlDao
public class JpaQrCodeSettingsDao extends JpaAbstractDao<QrCodeSettingsEntity, QrCodeSettings> implements QrCodeSettingsDao {

    @Autowired
    private QrCodeSettingsRepository qrCodeSettingsRepository;


    @Override
    public QrCodeSettings findByTenantId(TenantId tenantId) {
        return DaoUtil.getData(qrCodeSettingsRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        qrCodeSettingsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    protected Class<QrCodeSettingsEntity> getEntityClass() {
        return QrCodeSettingsEntity.class;
    }

    @Override
    protected JpaRepository<QrCodeSettingsEntity, UUID> getRepository() {
        return qrCodeSettingsRepository;
    }
}
