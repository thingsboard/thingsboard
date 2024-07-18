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
package org.thingsboard.server.dao.domain;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.domain.DomainOauth2Registration;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
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

import static org.thingsboard.server.dao.service.Validator.validateIds;

@Slf4j
@Service
public class DomainServiceImpl extends AbstractEntityService implements DomainService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DOMAIN_ID = "Incorrect domainId ";

    @Autowired
    private OAuth2RegistrationDao oauth2RegistrationDao;
    @Autowired
    private DomainDao domainDao;
    @Autowired
    private DataValidator<Domain> domainValidator;

    @Override
    public Domain saveDomain(TenantId tenantId, Domain domain) {
        log.trace("Executing saveDomain [{}]", domain);
        domainValidator.validate(domain, Domain::getTenantId);
        try {
            Domain savedDomain = domainDao.save(tenantId, domain);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedDomain).build());
            return savedDomain;
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("domain_unq_key")) {
                throw new DataValidationException("Domain with such name and scheme already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public void updateOauth2Clients(TenantId tenantId, DomainId domainId, List<OAuth2RegistrationId> oAuth2ClientIds) {
        log.trace("Executing addOauth2Clients, domainId [{}], oAuth2ClientIds [{}]", domainId, oAuth2ClientIds);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(domainId, id -> INCORRECT_DOMAIN_ID + id);
        Validator.checkNotNull(oAuth2ClientIds, "Incorrect oAuth2ClientIds " + oAuth2ClientIds);
        if (!oAuth2ClientIds.isEmpty()) {
            validateIds(oAuth2ClientIds, ids -> "Incorrect oAuth2ClientIds " + ids);
        }
        List<DomainOauth2Registration> oauth2Clients = new ArrayList<>();
        for (OAuth2RegistrationId oAuth2RegistrationId: oAuth2ClientIds) {
            oauth2Clients.add(new DomainOauth2Registration(domainId, oAuth2RegistrationId));
        }
        List<DomainOauth2Registration> existingClients = domainDao.findOauth2ClientsByDomainId(tenantId, domainId);
        List<OAuth2RegistrationId> toRemove = existingClients.stream()
                .map(DomainOauth2Registration::getOAuth2RegistrationId)
                .filter(clientId -> oAuth2ClientIds.stream().noneMatch(oauth2ClientId ->
                        oauth2ClientId.equals(clientId))).toList();
        for (OAuth2RegistrationId clientId : toRemove) {
            domainDao.removeOauth2Clients(domainId, clientId);
        }
        for (DomainOauth2Registration domainOauth2Registration : oauth2Clients) {
            domainDao.saveOauth2Clients(domainOauth2Registration);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(domainId).created(false).build());
    }

    @Override
    public void deleteDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing deleteDomain [{}]", domainId.getId());
        domainDao.removeById(tenantId, domainId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(domainId).build());
    }

    @Override
    public Domain findDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfo [{}] [{}]", tenantId, domainId);
        return domainDao.findById(tenantId, domainId.getId());
    }

    @Override
    public List<DomainInfo> findDomainInfosByTenantId(TenantId tenantId) {
        log.trace("Executing findDomainInfo [{}]", tenantId);
        List<Domain> domains = domainDao.findByTenantId(tenantId);
        List<DomainInfo> domainInfos = new ArrayList<>();
        domains.stream().sorted(Comparator.comparing(BaseData::getUuidId)).forEach(domain -> {
            domainInfos.add(new DomainInfo(domain, oauth2RegistrationDao.findInfosByDomainId(domain.getUuidId())));
        });
        return domainInfos;
    }

    @Override
    public DomainInfo findDomainInfoById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfoById [{}] [{}]", tenantId, domainId);
        Domain domain = domainDao.findById(tenantId, domainId.getId());
        if (domain == null) {
            return null;
        }
        return new DomainInfo(domain, oauth2RegistrationDao.findInfosByDomainId(domain.getUuidId()));
    }

    @Override
    public boolean isOauth2Enabled(TenantId tenantId) {
        log.trace("Executing isOauth2Enabled [{}] ", tenantId);
        return domainDao.countDomainByTenantIdAndOauth2Enabled(tenantId, true) > 0;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDomainById(tenantId, new DomainId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        Domain domain = domainDao.findById(tenantId, id.getId());
        if (domain == null) {
            return;
        }
        deleteDomainById(tenantId, domain.getId());
    }
}
