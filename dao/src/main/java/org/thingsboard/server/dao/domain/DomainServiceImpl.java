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
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateIds;

@Slf4j
@Service
public class DomainServiceImpl extends AbstractEntityService implements DomainService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DOMAIN_ID = "Incorrect domainId ";

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private DomainDao domainDao;

    @Override
    public Domain saveDomain(TenantId tenantId, Domain domain) {
        log.trace("Executing saveDomain [{}]", domain);
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
    public void updateOauth2Clients(TenantId tenantId, DomainId domainId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing updateOauth2Clients, domainId [{}], oAuth2ClientIds [{}]", domainId, oAuth2ClientIds);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(domainId, id -> INCORRECT_DOMAIN_ID + id);
        Validator.checkNotNull(oAuth2ClientIds, "Incorrect oAuth2ClientIds " + oAuth2ClientIds);
        if (!oAuth2ClientIds.isEmpty()) {
            validateIds(oAuth2ClientIds, ids -> "Incorrect oAuth2ClientIds " + ids);
        }
        List<DomainOauth2Client> oauth2Clients = new ArrayList<>();
        for (OAuth2ClientId oAuth2ClientId : oAuth2ClientIds) {
            oauth2Clients.add(new DomainOauth2Client(domainId, oAuth2ClientId));
        }
        List<DomainOauth2Client> existingClients = domainDao.findOauth2ClientsByDomainId(tenantId, domainId);
        List<OAuth2ClientId> toRemove = existingClients.stream()
                .map(DomainOauth2Client::getOAuth2ClientId)
                .filter(clientId -> oAuth2ClientIds.stream().noneMatch(oauth2ClientId ->
                        oauth2ClientId.equals(clientId))).toList();
        for (OAuth2ClientId clientId : toRemove) {
            domainDao.removeOauth2Clients(domainId, clientId);
        }
        for (DomainOauth2Client domainOauth2Client : oauth2Clients) {
            domainDao.saveOauth2Clients(domainOauth2Client);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(domainId).created(false).build());
    }

    @Override
    public void deleteDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing deleteDomainById [{}]", domainId.getId());
        domainDao.removeById(tenantId, domainId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(domainId).build());
    }

    @Override
    public Domain findDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfo [{}] [{}]", tenantId, domainId);
        return domainDao.findById(tenantId, domainId.getId());
    }

    @Override
    public PageData<DomainInfo> findDomainInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDomainInfosByTenantId [{}]", tenantId);
        PageData<Domain> pageData = domainDao.findByTenantId(tenantId, pageLink);
        List<DomainInfo> domainInfos = new ArrayList<>();
        for (Domain domain : pageData.getData()) {
            domainInfos.add(new DomainInfo(domain, oauth2ClientDao.findByDomainId(domain.getUuidId()).stream()
                    .map(OAuth2ClientInfo::new)
                    .collect(Collectors.toList())));
        }
        return new PageData<>(domainInfos, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @Override
    public DomainInfo findDomainInfoById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfoById [{}] [{}]", tenantId, domainId);
        Domain domain = domainDao.findById(tenantId, domainId.getId());
        if (domain == null) {
            return null;
        }
        return new DomainInfo(domain, oauth2ClientDao.findByDomainId(domain.getUuidId()).stream()
                .map(OAuth2ClientInfo::new)
                .collect(Collectors.toList()));
    }

    @Override
    public boolean isOauth2Enabled(TenantId tenantId) {
        log.trace("Executing isOauth2Enabled [{}] ", tenantId);
        return domainDao.countDomainByTenantIdAndOauth2Enabled(tenantId, true) > 0;
    }

    @Override
    public void deleteDomainsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDomainsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        domainDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteDomainsByTenantId(tenantId);
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
