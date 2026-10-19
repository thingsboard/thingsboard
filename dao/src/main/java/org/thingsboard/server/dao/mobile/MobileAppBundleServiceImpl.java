// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.mobile;

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.checkNotNull;

@Slf4j
@Service
public class MobileAppBundleServiceImpl extends AbstractEntityService implements MobileAppBundleService {

    private static final String PLATFORM_TYPE_IS_REQUIRED = "Platform type is required if package name is specified";

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private MobileAppBundleDao mobileAppBundleDao;
    @Autowired
    private DataValidator<MobileAppBundle> mobileAppBundleDataValidator;

    @Override
    public MobileAppBundle saveMobileAppBundle(TenantId tenantId, MobileAppBundle mobileAppBundle) {
        log.trace("Executing saveMobileAppBundle [{}]", mobileAppBundle);
        mobileAppBundleDataValidator.validate(mobileAppBundle, b -> tenantId);
        try {
            MobileAppBundle savedMobileAppBundle = mobileAppBundleDao.save(tenantId, mobileAppBundle);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedMobileAppBundle).build());
            return savedMobileAppBundle;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "mobile_app_bundle_android_app_id_key", "Android mobile app is already configured in another bundle!",
                    "mobile_app_bundle_ios_app_id_key", "IOS mobile app is already configured in another bundle!");
            throw e;
        }
    }

    @Override
    public void updateOauth2Clients(TenantId tenantId, MobileAppBundleId mobileAppBundleId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing updateOauth2Clients, mobileAppId [{}], oAuth2ClientIds [{}]", mobileAppBundleId, oAuth2ClientIds);
        Set<MobileAppBundleOauth2Client> newClientList = oAuth2ClientIds.stream()
                .map(clientId -> new MobileAppBundleOauth2Client(mobileAppBundleId, clientId))
                .collect(Collectors.toSet());

        List<MobileAppBundleOauth2Client> existingClients = mobileAppBundleDao.findOauth2ClientsByMobileAppBundleId(tenantId, mobileAppBundleId);
        List<MobileAppBundleOauth2Client> toRemoveList = existingClients.stream()
                .filter(client -> !newClientList.contains(client))
                .toList();
        newClientList.removeIf(existingClients::contains);

        for (MobileAppBundleOauth2Client client : toRemoveList) {
            mobileAppBundleDao.removeOauth2Client(tenantId, client);
        }
        for (MobileAppBundleOauth2Client client : newClientList) {
            mobileAppBundleDao.addOauth2Client(tenantId, client);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(mobileAppBundleId).created(false).build());
    }

    @Override
    public MobileAppBundle findMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        log.trace("Executing findMobileAppBundleById [{}] [{}]", tenantId, mobileAppBundleId);
        return mobileAppBundleDao.findById(tenantId, mobileAppBundleId.getId());
    }

    @Override
    public PageData<MobileAppBundleInfo> findMobileAppBundleInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileAppBundleInfosByTenantId [{}]", tenantId);
        PageData<MobileAppBundleInfo> mobileBundles = mobileAppBundleDao.findInfosByTenantId(tenantId, pageLink);
        mobileBundles.getData().forEach(this::fetchOauth2Clients);
        return mobileBundles;
    }

    @Override
    public MobileAppBundleInfo findMobileAppBundleInfoById(TenantId tenantId, MobileAppBundleId mobileAppIdBundle) {
        log.trace("Executing findMobileAppBundleFullInfoById [{}] [{}]", tenantId, mobileAppIdBundle);
        MobileAppBundle mobileAppBundle = mobileAppBundleDao.findById(tenantId, mobileAppIdBundle.getId());
        if (mobileAppBundle == null) {
            return null;
        }
        List<OAuth2ClientInfo> clients = oauth2ClientDao.findByMobileAppBundleId(mobileAppBundle.getUuidId()).stream()
                .map(OAuth2ClientInfo::new)
                .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle))
                .collect(Collectors.toList());
        return new MobileAppBundleInfo(mobileAppBundle, clients);
    }

    @Override
    public MobileAppBundle findMobileAppBundleByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platformType, boolean fetchPolicyInfo) {
        log.trace("Executing findMobileAppBundleByPkgNameAndPlatform, tenantId [{}], pkgName [{}], platform [{}]", tenantId, pkgName, platformType);
        checkNotNull(platformType, PLATFORM_TYPE_IS_REQUIRED);
        if (fetchPolicyInfo) {
            return mobileAppBundleDao.findPolicyInfoByPkgNameAndPlatform(tenantId, pkgName, platformType);
        } else {
            return mobileAppBundleDao.findByPkgNameAndPlatform(tenantId, pkgName, platformType);
        }
    }

    @Override
    public MobileSelfRegistrationParams getMobileSelfRegistrationParams(TenantId tenantId, String pkgName, PlatformType platformType) {
        log.trace("Executing findMobileSelfRegistrationSettings, tenantId [{}], pkgName [{}], platform [{}]", tenantId, pkgName, platformType);
        MobileAppBundle appBundle = findMobileAppBundleByPkgNameAndPlatform(TenantId.SYS_TENANT_ID, pkgName, platformType, false);
        return appBundle != null ? appBundle.getSelfRegistrationParams() : null;
    }

    @Override
    public String getMobilePrivacyPolicy(TenantId tenantId, String pkgName, PlatformType platformType) {
        log.trace("Executing findMobilePrivacyPolicy, tenantId [{}], pkgName [{}], platform [{}]", tenantId, pkgName, platformType);
        checkNotNull(platformType, PLATFORM_TYPE_IS_REQUIRED);
        MobileAppBundle appBundle = findMobileAppBundleByPkgNameAndPlatform(tenantId, pkgName, platformType, true);
        if (appBundle != null && appBundle.getSelfRegistrationParams() != null) {
            return appBundle.getSelfRegistrationParams().getPrivacyPolicy();
        }
        return null;
    }

    @Override
    public String getMobileTermsOfUse(TenantId tenantId, String pkgName, PlatformType platformType) {
        log.trace("Executing findMobileTermsOfUse, tenantId [{}], pkgName [{}], platform [{}]", tenantId, pkgName, platformType);
        checkNotNull(platformType, PLATFORM_TYPE_IS_REQUIRED);
        MobileAppBundle appBundle = findMobileAppBundleByPkgNameAndPlatform(tenantId, pkgName, platformType, true);
        if (appBundle != null && appBundle.getSelfRegistrationParams() != null) {
            return appBundle.getSelfRegistrationParams().getTermsOfUse();
        }
        return null;
    }

    @Override
    public void deleteMobileAppBundleById(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        log.trace("Executing deleteMobileAppBundleById [{}]", mobileAppBundleId.getId());
        mobileAppBundleDao.removeById(tenantId, mobileAppBundleId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(mobileAppBundleId).build());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteMobileAppsByTenantId, tenantId [{}]", tenantId);
        mobileAppBundleDao.deleteByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findMobileAppBundleById(tenantId, new MobileAppBundleId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(mobileAppBundleDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteMobileAppBundleById(tenantId, (MobileAppBundleId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP_BUNDLE;
    }

    private void fetchOauth2Clients(MobileAppBundleInfo mobileAppBundleInfo) {
        List<OAuth2ClientInfo> clients = oauth2ClientDao.findByMobileAppBundleId(mobileAppBundleInfo.getUuidId()).stream()
                .map(OAuth2ClientInfo::new)
                .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle))
                .collect(Collectors.toList());
        mobileAppBundleInfo.setOauth2ClientInfos(clients);
    }

}
