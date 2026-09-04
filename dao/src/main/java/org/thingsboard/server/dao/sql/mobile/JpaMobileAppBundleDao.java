// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.mobile;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.mobile.MobileAppBundleDao;
import org.thingsboard.server.dao.model.sql.MobileAppBundleEntity;
import org.thingsboard.server.dao.model.sql.MobileAppBundleOauth2ClientEntity;
import org.thingsboard.server.dao.model.sql.MobileAppOauth2ClientCompositeKey;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaMobileAppBundleDao extends JpaAbstractDao<MobileAppBundleEntity, MobileAppBundle> implements MobileAppBundleDao {

    private final MobileAppBundleRepository mobileAppBundleRepository;
    private final MobileAppBundleOauth2ClientRepository mobileOauth2ProviderRepository;

    @Override
    protected Class<MobileAppBundleEntity> getEntityClass() {
        return MobileAppBundleEntity.class;
    }

    @Override
    protected JpaRepository<MobileAppBundleEntity, UUID> getRepository() {
        return mobileAppBundleRepository;
    }

    @Override
    public PageData<MobileAppBundleInfo> findInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(mobileAppBundleRepository.findInfoByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public MobileAppBundleInfo findInfoById(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        return DaoUtil.getData(mobileAppBundleRepository.findInfoById(mobileAppBundleId.getId()));
    }

    @Override
    public List<MobileAppBundleOauth2Client> findOauth2ClientsByMobileAppBundleId(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        return DaoUtil.convertDataList(mobileOauth2ProviderRepository.findAllByMobileAppBundleId(mobileAppBundleId.getId()));
    }

    @Override
    public void addOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileOauth2ProviderRepository.save(new MobileAppBundleOauth2ClientEntity(mobileAppBundleOauth2Client));
    }

    @Override
    public void removeOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileOauth2ProviderRepository.deleteById(new MobileAppOauth2ClientCompositeKey(mobileAppBundleOauth2Client.getMobileAppBundleId().getId(),
                mobileAppBundleOauth2Client.getOAuth2ClientId().getId()));
    }

    @Override
    public MobileAppBundle findByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform) {
        return DaoUtil.getData(mobileAppBundleRepository.findByPkgNameAndPlatformType(pkgName, platform));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        mobileAppBundleRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP_BUNDLE;
    }

}

