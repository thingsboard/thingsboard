// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceCertificateRevocationDao;
import org.thingsboard.server.dao.model.sql.DeviceCertificateRevocationEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@Slf4j
@SqlDao
public class JpaDeviceCertificateRevocationDao extends JpaAbstractDao<DeviceCertificateRevocationEntity, DeviceCertificateRevocation> implements DeviceCertificateRevocationDao {

    @Autowired
    private DeviceCertificateRevocationRepository revocationRepository;

    @Override
    public DeviceCertificateRevocation findByTenantIdAndCertificateHash(TenantId tenantId, String certificateHash) {
        return DaoUtil.getData(revocationRepository.findByTenantIdAndCertificateHash(tenantId.getId(), certificateHash));
    }

    @Override
    public PageData<DeviceCertificateRevocation> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(revocationRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        revocationRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public int deleteAllByNotAfterBefore(long ts) {
        return revocationRepository.deleteAllByNotAfterBefore(ts);
    }

    @Override
    protected Class<DeviceCertificateRevocationEntity> getEntityClass() {
        return DeviceCertificateRevocationEntity.class;
    }

    @Override
    protected JpaRepository<DeviceCertificateRevocationEntity, UUID> getRepository() {
        return revocationRepository;
    }

}
