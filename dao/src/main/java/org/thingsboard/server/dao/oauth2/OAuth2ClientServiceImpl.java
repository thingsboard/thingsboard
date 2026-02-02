/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.oauth2;

import com.google.common.util.concurrent.FluentFuture;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Slf4j
@Service("OAuth2ClientService")
public class OAuth2ClientServiceImpl extends AbstractEntityService implements OAuth2ClientService {

    private static final String PLATFORM_TYPE_IS_REQUIRED = "Platform type is required if package name is specified";

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private DataValidator<OAuth2Client> oAuth2ClientDataValidator;

    @Override
    public List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByDomainName(String domainName) {
        log.trace("Executing findOAuth2ClientLoginInfosByDomainName [{}] ", domainName);
        return oauth2ClientDao.findEnabledByDomainName(domainName)
                .stream()
                .map(OAuth2Utils::toClientLoginInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(String pkgName, PlatformType platformType) {
        log.trace("Executing findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType pkgName=[{}] platformType=[{}]", pkgName, platformType);
        return oauth2ClientDao.findEnabledByPkgNameAndPlatformType(pkgName, platformType)
                .stream()
                .map(OAuth2Utils::toClientLoginInfo)
                .collect(Collectors.toList());
    }

    @Override
    public OAuth2Client saveOAuth2Client(TenantId tenantId, OAuth2Client oAuth2Client) {
        log.trace("Executing saveOAuth2Client [{}]", oAuth2Client);
        oAuth2ClientDataValidator.validate(oAuth2Client, OAuth2Client::getTenantId);
        OAuth2Client savedOauth2Client = oauth2ClientDao.save(tenantId, oAuth2Client);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedOauth2Client.getId()).entity(savedOauth2Client).build());
        return savedOauth2Client;
    }

    @Override
    public OAuth2Client findOAuth2ClientById(TenantId tenantId, OAuth2ClientId oAuth2ClientId) {
        log.trace("Executing findOAuth2ClientById [{}]", oAuth2ClientId);
        return oauth2ClientDao.findById(tenantId, oAuth2ClientId.getId());
    }

    @Override
    public List<OAuth2Client> findOAuth2ClientsByTenantId(TenantId tenantId) {
        log.trace("Executing findOAuth2ClientsByTenantId [{}]", tenantId);
        return oauth2ClientDao.findByTenantId(tenantId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
    }

    @Override
    public String findAppSecret(OAuth2ClientId oAuth2ClientId, String pkgName, PlatformType platformType) {
        log.trace("Executing findAppSecret oAuth2ClientId = [{}] pkgName = [{}], platform [{}]", oAuth2ClientId, pkgName, platformType);
        Validator.checkNotNull(platformType, PLATFORM_TYPE_IS_REQUIRED);
        return oauth2ClientDao.findAppSecret(oAuth2ClientId.getId(), pkgName, platformType);
    }

    @Override
    public void deleteOAuth2ClientById(TenantId tenantId, OAuth2ClientId oAuth2ClientId) {
        log.trace("Executing deleteOAuth2ClientById [{}]", oAuth2ClientId);
        oauth2ClientDao.removeById(tenantId, oAuth2ClientId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(oAuth2ClientId)
                .build());
    }

    @Override
    public void deleteOauth2ClientsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteOauth2ClientsByTenantId, tenantId [{}]", tenantId);
        oauth2ClientDao.deleteByTenantId(tenantId.getId());
    }

    @Override
    public PageData<OAuth2ClientInfo> findOAuth2ClientInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findOAuth2ClientInfosByTenantId tenantId=[{}]", tenantId);
        PageData<OAuth2Client> clients = oauth2ClientDao.findByTenantId(tenantId.getId(), pageLink);
        return clients.mapData(OAuth2ClientInfo::new);
    }

    @Override
    public List<OAuth2ClientInfo> findOAuth2ClientInfosByIds(TenantId tenantId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing findQueueStatsByIds, tenantId [{}], oAuth2ClientIds [{}]", tenantId, oAuth2ClientIds);
        return oauth2ClientDao.findByIds(tenantId.getId(), oAuth2ClientIds)
                .stream()
                .map(OAuth2ClientInfo::new)
                .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPropagateOAuth2ClientToEdge(TenantId tenantId, OAuth2ClientId oAuth2ClientId) {
        log.trace("Executing isPropagateOAuth2ClientToEdge, tenantId [{}], oAuth2ClientId [{}]", tenantId, oAuth2ClientId);
        return oauth2ClientDao.isPropagateToEdge(tenantId, oAuth2ClientId.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteOauth2ClientsByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findOAuth2ClientById(tenantId, new OAuth2ClientId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(oauth2ClientDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteOAuth2ClientById(tenantId, (OAuth2ClientId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

}
