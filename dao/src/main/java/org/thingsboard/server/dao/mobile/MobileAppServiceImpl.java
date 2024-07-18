/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.mobile;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppInfo;
import org.thingsboard.server.common.data.mobile.MobileAppOauth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2RegistrationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateIds;

@Slf4j
@Service
public class MobileAppServiceImpl extends AbstractEntityService implements MobileAppService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_MOBILE_APP_ID = "Incorrect mobileApppId ";

    @Autowired
    private OAuth2RegistrationDao oauth2RegistrationDao;
    @Autowired
    private MobileAppDao mobileAppDao;
    @Autowired
    private DataValidator<MobileApp> mobileAppValidator;

    @Override
    public MobileApp saveMobileApp(TenantId tenantId, MobileApp mobileApp) {
        log.trace("Executing saveMobileApp [{}]", mobileApp);
        mobileAppValidator.validate(mobileApp, MobileApp::getTenantId);
        try {
            MobileApp savedMobileApp = mobileAppDao.save(tenantId, mobileApp);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedMobileApp).build());
            return savedMobileApp;
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("mobile_app_unq_key")) {
                throw new DataValidationException("Mobile app with such package already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public void deleteMobileAppById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing deleteMobileAppById [{}]", mobileAppId.getId());
        mobileAppDao.removeById(tenantId, mobileAppId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(mobileAppId).build());
   }

    @Override
    public MobileApp findMobileAppById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing findMobileAppById [{}] [{}]", tenantId, mobileAppId);
        return mobileAppDao.findById(tenantId, mobileAppId.getId());
    }

    @Override
    public List<MobileAppInfo> findMobileAppInfosByTenantId(TenantId tenantId) {
        log.trace("Executing findMobileAppInfosByTenantId [{}]", tenantId);
        List<MobileApp> mobileApps = mobileAppDao.findByTenantId(tenantId);
        List<MobileAppInfo> mobileAppInfos = new ArrayList<>();
        mobileApps.stream().sorted(Comparator.comparing(BaseData::getUuidId)).forEach(mobileApp -> {
            mobileAppInfos.add(new MobileAppInfo(mobileApp, oauth2RegistrationDao.findInfosByMobileAppId(mobileApp.getUuidId())));
        });
        return mobileAppInfos;
    }

    @Override
    public MobileAppInfo findMobileAppInfoById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing findMobileAppInfoById [{}] [{}]", tenantId, mobileAppId);
        MobileApp mobileApp = mobileAppDao.findById(tenantId, mobileAppId.getId());
        if (mobileApp == null) {
            return null;
        }
        return new MobileAppInfo(mobileApp, oauth2RegistrationDao.findInfosByMobileAppId(mobileApp.getUuidId()));
    }

    @Override
    public void updateOauth2Clients(TenantId tenantId, MobileAppId mobileAppId, List<OAuth2RegistrationId> oAuth2ClientIds) {
        log.trace("Executing updateOauth2Clients, mobileAppId [{}], oAuth2ClientIds [{}]", mobileAppId, oAuth2ClientIds);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(mobileAppId, id -> INCORRECT_MOBILE_APP_ID + id);
        Validator.checkNotNull(oAuth2ClientIds, "Incorrect oAuth2ClientIds " + oAuth2ClientIds);
        if (!oAuth2ClientIds.isEmpty()) {
            validateIds(oAuth2ClientIds, ids -> "Incorrect oAuth2ClientIds " + ids);
        }
        List<MobileAppOauth2Registration> oauth2Clients = new ArrayList<>();
        for (OAuth2RegistrationId oAuth2RegistrationId: oAuth2ClientIds) {
            oauth2Clients.add(new MobileAppOauth2Registration(mobileAppId, oAuth2RegistrationId));
        }
        List<MobileAppOauth2Registration> existingClients = mobileAppDao.findOauth2ClientsByMobileAppId(tenantId, mobileAppId);
        List<OAuth2RegistrationId> toRemove = existingClients.stream()
                .map(MobileAppOauth2Registration::getOAuth2RegistrationId)
                .filter(clientId -> oAuth2ClientIds.stream().noneMatch(oauth2ClientId ->
                        oauth2ClientId.equals(clientId))).toList();
        for (OAuth2RegistrationId clientId : toRemove) {
            mobileAppDao.removeOauth2Clients(mobileAppId, clientId);
        }
        for (MobileAppOauth2Registration mobileAppOauth2Registration : oauth2Clients) {
            mobileAppDao.saveOauth2Clients(mobileAppOauth2Registration);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(mobileAppId).created(false).build());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findMobileAppById(tenantId, new MobileAppId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        MobileApp mobileApp = mobileAppDao.findById(tenantId, id.getId());
        if (mobileApp == null) {
            return;
        }
        deleteMobileAppById(tenantId, mobileApp.getId());
    }
}
