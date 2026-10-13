// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.domain;

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.CustomerId;
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
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.validator.DomainDataValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Slf4j
@Service
public class DomainServiceImpl extends AbstractEntityService implements DomainService {

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private DomainDao domainDao;
    @Autowired
    private DomainDataValidator domainDataValidator;

    @Override
    public Domain saveDomain(TenantId tenantId, Domain domain) {
        log.trace("Executing saveDomain [{}]", domain);
        try {
            domainDataValidator.validate(domain, Domain::getTenantId);
            Domain savedDomain = domainDao.save(tenantId, domain);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedDomain.getId()).entity(savedDomain).build());
            return savedDomain;
        } catch (Exception e) {
            checkConstraintViolation(e, "domain_name_key", "Domain with such name and scheme already exists!");
            throw e;
        }
    }

    @Override
    public void updateOauth2Clients(TenantId tenantId, DomainId domainId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing updateOauth2Clients, domainId [{}], oAuth2ClientIds [{}]", domainId, oAuth2ClientIds);
        Set<DomainOauth2Client> newClientList = oAuth2ClientIds.stream()
                .map(clientId -> new DomainOauth2Client(domainId, clientId))
                .collect(Collectors.toSet());

        List<DomainOauth2Client> existingClients = domainDao.findOauth2ClientsByDomainId(tenantId, domainId);
        List<DomainOauth2Client> toRemoveList = existingClients.stream()
                .filter(client -> !newClientList.contains(client))
                .toList();
        newClientList.removeIf(existingClients::contains);

        for (DomainOauth2Client client : toRemoveList) {
            domainDao.removeOauth2Client(client);
        }
        for (DomainOauth2Client client : newClientList) {
            domainDao.addOauth2Client(client);
        }
    }

    @Override
    public void deleteDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing deleteDomainById [{}]", domainId.getId());
        try {
            domainDao.removeById(tenantId, domainId.getId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(domainId).build());
        } catch (Exception e) {
            checkConstraintViolation(e, "fk_white_labeling_domain_id", "The domain is referenced by a white labeling settings");
            throw e;
        }
    }

    @Override
    public Domain findDomainById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfo [{}] [{}]", tenantId, domainId);
        return domainDao.findById(tenantId, domainId.getId());
    }

    @Override
    public PageData<DomainInfo> findDomainInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDomainInfosByTenantIdAndCustomerId [{}]", tenantId);
        PageData<Domain> domains = domainDao.findByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        return domains.mapData(this::getDomainInfo);
    }

    @Override
    public DomainInfo findDomainInfoById(TenantId tenantId, DomainId domainId) {
        log.trace("Executing findDomainInfoById [{}] [{}]", tenantId, domainId);
        Domain domain = domainDao.findById(tenantId, domainId.getId());
        return getDomainInfo(domain);
    }

    @Override
    public boolean isOauth2Enabled(TenantId tenantId) {
        log.trace("Executing isOauth2Enabled [{}] ", tenantId);
        return domainDao.countDomainByTenantIdAndOauth2Enabled(tenantId, true) > 0;
    }

    @Override
    public void deleteDomainsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteDomainsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        customerDomainsRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDomainById(tenantId, new DomainId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(domainDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteDomainById(tenantId, (DomainId) id);
    }

    @Override
    public void deleteDomainsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDomainsByTenantId, tenantId [{}]", tenantId);
        domainDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteDomainsByTenantId(tenantId);
    }

    private final PaginatedRemover<CustomerId, Domain> customerDomainsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Domain> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return domainDao.findByTenantIdAndCustomerId(tenantId, id, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Domain entity) {
            deleteEntity(tenantId, new DomainId(entity.getUuidId()), true);
        }
    };

    private DomainInfo getDomainInfo(Domain domain) {
        if (domain == null) {
            return null;
        }
        List<OAuth2ClientInfo> clients = oauth2ClientDao.findByDomainId(domain.getUuidId()).stream()
                .map(OAuth2ClientInfo::new)
                .sorted(Comparator.comparing(OAuth2ClientInfo::getTitle))
                .collect(Collectors.toList());
        return new DomainInfo(domain, clients);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
    }

}
