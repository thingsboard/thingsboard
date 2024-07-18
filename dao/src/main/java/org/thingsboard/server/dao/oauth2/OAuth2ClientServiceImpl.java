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
package org.thingsboard.server.dao.oauth2;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
public class OAuth2ClientServiceImpl extends AbstractEntityService implements OAuth2ClientService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CLIENT_REGISTRATION_ID = "Incorrect clientRegistrationId ";
    public static final String INCORRECT_DOMAIN_NAME = "Incorrect domainName ";

    @Autowired
    private OAuth2RegistrationDao oauth2RegistrationDao;
    @Autowired
    private DataValidator<OAuth2Registration> oAuth2RegistrationDataValidator;

    @Override
    public List<OAuth2ClientInfo> getWebOAuth2Clients(String domainName, PlatformType platformType) {
        log.trace("Executing getOAuth2Clients [{}] ", domainName);
        validateString(domainName, dn -> INCORRECT_DOMAIN_NAME + dn);
        return oauth2RegistrationDao.findEnabledByDomainNameAndPlatformType(domainName, platformType)
                .stream()
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<OAuth2ClientInfo> getMobileOAuth2Clients(String pkgName, PlatformType platformType) {
        log.trace("Executing getOAuth2Clients pkgName=[{}] platformType=[{}]",pkgName, platformType);
        return oauth2RegistrationDao.findEnabledByPckNameAndPlatformType(pkgName, platformType)
                .stream()
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OAuth2Registration saveOAuth2Client(TenantId tenantId, OAuth2Registration oAuth2Registration) {
        log.trace("Executing saveOAuth2Client [{}]", oAuth2Registration);
        oAuth2RegistrationDataValidator.validate(oAuth2Registration, OAuth2Registration::getTenantId);
        OAuth2Registration savedOauth2Registration = oauth2RegistrationDao.save(tenantId, oAuth2Registration);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entity(oAuth2Registration).build());
        return savedOauth2Registration;
    }

    @Override
    public OAuth2Registration findOAuth2ClientById(TenantId tenantId, OAuth2RegistrationId oAuth2RegistrationId) {
        log.trace("Executing findOAuth2ClientById [{}]", oAuth2RegistrationId);
        validateId(oAuth2RegistrationId, uuid -> INCORRECT_CLIENT_REGISTRATION_ID + uuid);
        return oauth2RegistrationDao.findById(tenantId, oAuth2RegistrationId.getId());
    }

    @Override
    public List<OAuth2RegistrationInfo> findOauth2ClientInfosByTenantId(TenantId tenantId) {
        log.trace("Executing findOauth2ClientInfosByTenantId");
        return oauth2RegistrationDao.findInfosByTenantId(tenantId.getId());
    }

    @Override
    public List<OAuth2Registration> findOauth2ClientsByTenantId(TenantId tenantId) {
        log.trace("Executing findOauth2ClientsByTenantId [{}]", tenantId);
        return oauth2RegistrationDao.findByTenantId(tenantId.getId());
    }

    @Override
    public String findAppSecret(UUID id, String pkgName) {
        log.trace("Executing findAppSecret [{}][{}]", id, pkgName);
        validateId(id, uuid -> INCORRECT_CLIENT_REGISTRATION_ID + uuid);
        validateString(pkgName, "Incorrect package name");
        return oauth2RegistrationDao.findAppSecret(id, pkgName);
    }

    @Override
    @Transactional
    public void deleteById(TenantId tenantId, OAuth2RegistrationId oAuth2RegistrationId) {
        log.trace("[{}][{}] Executing deleteById [{}]", tenantId, oAuth2RegistrationId);
        oauth2RegistrationDao.removeById(tenantId, oAuth2RegistrationId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(oAuth2RegistrationId)
                .build());

    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findOAuth2ClientById(tenantId, new OAuth2RegistrationId(entityId.getId())));
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        OAuth2Registration oAuth2Registration = oauth2RegistrationDao.findById(tenantId, id.getId());
        if (oAuth2Registration == null) {
            return;
        }
        deleteById(tenantId, oAuth2Registration.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

}
