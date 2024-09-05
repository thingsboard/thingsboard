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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppInfo;
import org.thingsboard.server.common.data.mobile.MobileAppOauth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MobileAppServiceImpl extends AbstractEntityService implements MobileAppService {

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private MobileAppDao mobileAppDao;

    @Override
    public MobileApp saveMobileApp(TenantId tenantId, MobileApp mobileApp) {
        log.trace("Executing saveMobileApp [{}]", mobileApp);
        try {
            MobileApp savedMobileApp = mobileAppDao.save(tenantId, mobileApp);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedMobileApp).build());
            return savedMobileApp;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    Map.of("mobile_app_unq_key", "Mobile app with such package already exists!"));
            throw e;
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
    public PageData<MobileAppInfo> findMobileAppInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileAppInfosByTenantId [{}]", tenantId);
        PageData<MobileApp> mobiles = mobileAppDao.findByTenantId(tenantId, pageLink);
        return mobiles.mapData(this::getMobileAppInfo);
    }

    @Override
    public MobileAppInfo findMobileAppInfoById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing findMobileAppInfoById [{}] [{}]", tenantId, mobileAppId);
        MobileApp mobileApp = mobileAppDao.findById(tenantId, mobileAppId.getId());
        if (mobileApp == null) {
            return null;
        }
        return getMobileAppInfo(mobileApp);
    }

    @Override
    public void updateOauth2Clients(TenantId tenantId, MobileAppId mobileAppId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing updateOauth2Clients, mobileAppId [{}], oAuth2ClientIds [{}]", mobileAppId, oAuth2ClientIds);
        Set<MobileAppOauth2Client> newClientList = oAuth2ClientIds.stream()
                .map(clientId -> new MobileAppOauth2Client(mobileAppId, clientId))
                .collect(Collectors.toSet());

        List<MobileAppOauth2Client> existingClients = mobileAppDao.findOauth2ClientsByMobileAppId(tenantId, mobileAppId);
        List<MobileAppOauth2Client> toRemoveList = existingClients.stream()
                .filter(client -> !newClientList.contains(client))
                .toList();
        newClientList.removeIf(existingClients::contains);

        for (MobileAppOauth2Client client : toRemoveList) {
            mobileAppDao.removeOauth2Client(client);
        }
        for (MobileAppOauth2Client client : newClientList) {
            mobileAppDao.addOauth2Client(client);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(mobileAppId).created(false).build());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findMobileAppById(tenantId, new MobileAppId(entityId.getId())));
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteMobileAppById(tenantId, (MobileAppId) id);
    }

    @Override
    public void deleteMobileAppsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteMobileAppsByTenantId, tenantId [{}]", tenantId);
        mobileAppDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteMobileAppsByTenantId(tenantId);
    }

    private MobileAppInfo getMobileAppInfo(MobileApp mobileApp) {
        List<OAuth2ClientInfo> clients = oauth2ClientDao.findByMobileAppId(mobileApp.getUuidId()).stream()
                .map(OAuth2ClientInfo::new)
                .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle))
                .collect(Collectors.toList());
        return new MobileAppInfo(mobileApp, clients);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }
}
