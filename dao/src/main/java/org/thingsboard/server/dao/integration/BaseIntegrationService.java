// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Slf4j
@Service("IntegrationDaoService")
public class BaseIntegrationService extends CachedVersionedEntityService<IntegrationCacheKey, Integration, IntegrationCacheEvictEvent> implements IntegrationService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_INTEGRATION_ID = "Incorrect integrationId ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private IntegrationInfoDao integrationInfoDao;

    @Autowired
    private DataValidator<Integration> integrationValidator;

    @Autowired
    private EntityCountService entityCountService;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(IntegrationCacheEvictEvent event) {
        if (event.getSavedIntegration() != null) {
            cache.put(IntegrationCacheKey.forId(event.getSavedIntegration().getId()), event.getSavedIntegration());
        } else {
            cache.evict(IntegrationCacheKey.forId(event.getIntegrationId()));
        }
    }

    @Override
    public Integration saveIntegration(Integration integration) {
        return saveEntity(integration, () -> doSaveIntegration(integration));
    }

    private Integration doSaveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        integrationValidator.validate(integration, Integration::getTenantId);
        TenantId tenantId = integration.getTenantId();
        try {
            updateDebugSettings(tenantId, integration, System.currentTimeMillis());
            var result = integrationDao.save(tenantId, integration);
            publishEvictEvent(new IntegrationCacheEvictEvent(result.getId(), result));
            if (integration.getId() == null) {
                entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.INTEGRATION);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId()).entity(result)
                    .entityId(result.getId()).created(integration.getId() == null).build());
            return result;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "integration_external_id_unq_key", "Integration with such external id already exists!");
            throw t;
        }
    }

    @Override
    public Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing findIntegrationById [{}]", integrationId);
        validateId(integrationId, id -> INCORRECT_INTEGRATION_ID + id);
        return cache.get(IntegrationCacheKey.forId(integrationId), () -> integrationDao.findById(tenantId, integrationId.getId()));
    }

    @Override
    public ListenableFuture<Integration> findIntegrationByIdAsync(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing findIntegrationByIdAsync [{}]", integrationId);
        validateId(integrationId, id -> INCORRECT_INTEGRATION_ID + id);
        return integrationDao.findByIdAsync(tenantId, integrationId.getId());
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds) {
        log.trace("Executing findIntegrationsByIdsAsync, tenantId [{}], integrationIds [{}]", tenantId, integrationIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(integrationIds, ids -> "Incorrect integrationIds " + ids);
        return integrationDao.findIntegrationsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(integrationIds));
    }

    @Override
    public Optional<Integration> findIntegrationByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findIntegrationByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect integration routingKey for search request.");
        return integrationDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public List<Integration> findAllIntegrations(TenantId tenantId) {
        log.trace("Executing findAllIntegrations");
        return integrationDao.find(tenantId);
    }

    @Override
    public List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findIntegrationsByConverterId [{}]", converterId);
        validateId(converterId, id -> INCORRECT_CONVERTER_ID + id);
        return integrationDao.findByConverterId(tenantId.getId(), converterId.getId());
    }

    @Override
    public PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantIntegrations, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return integrationDao.findCoreIntegrationsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate) {
        log.trace("Executing findTenantIntegrationInfos, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return integrationInfoDao.findByTenantIdAndIsEdgeTemplate(tenantId.getId(), pageLink, isEdgeTemplate);
    }

    @Override
    public PageData<IntegrationInfo> findTenantIntegrationInfosWithStats(TenantId tenantId, boolean isEdgeTemplate, PageLink pageLink) {
        log.trace("Executing findTenantIntegrationInfosWithStats, tenantId [{}], isEdgeTemplate [{}], pageLink [{}]", tenantId, isEdgeTemplate, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return integrationInfoDao.findAllIntegrationInfosWithStats(tenantId.getId(), isEdgeTemplate, pageLink);
    }

    @Override
    public PageData<Integration> findTenantEdgeTemplateIntegrations(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantEdgeTemplateIntegrations, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return integrationDao.findEdgeTemplateIntegrationsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<Integration> findTenantIntegrationsByName(TenantId tenantId, String name) {
        return integrationDao.findTenantIntegrationsByName(tenantId.getId(), name);
    }

    @Override
    @Transactional
    public void deleteIntegration(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        Integration integration = findIntegrationById(tenantId, integrationId);
        if (integration == null) {
            return;
        }
        integrationDao.removeById(tenantId, integrationId.getId());
        publishEvictEvent(new IntegrationCacheEvictEvent(integrationId));
        entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.INTEGRATION);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(integration).entityId(integrationId).build());
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteIntegration(tenantId, (IntegrationId) id);
    }

    @Override
    @Transactional
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteIntegrationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantIntegrationsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        deleteIntegrationsByTenantId(tenantId);
    }

    @Override
    public Long countCoreIntegrations() {
        log.trace("Executing countCoreIntegrations");
        return integrationDao.countCoreIntegrations();
    }

    @Override
    public List<Integration> findAllCoreIntegrations(IntegrationType integrationType, boolean remote, boolean enabled) {
        log.trace("Executing findAllCoreIntegrations [{}][{}][{}]", integrationType, remote, enabled);
        return integrationDao.findAllCoreIntegrations(integrationType, remote, enabled);
    }

    @Override
    public Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign integration to non-existent edge!");
        }
        if (!edge.getTenantId().equals(integration.getTenantId())) {
            throw new DataValidationException("Can't assign integration to edge from different tenant!");
        }
        if (!integration.isEdgeTemplate()) {
            throw new DataValidationException("Can't assign non edge template integration to edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, integrationId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create integration relation. Edge Id: [{}]", integrationId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(integrationId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return integration;
    }

    @Override
    public Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId, boolean remove) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign integration from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, integrationId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete integration relation. Edge Id: [{}]", integrationId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(integrationId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return integration;
    }

    @Override
    public PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        Validator.validatePageLink(pageLink);
        return integrationDao.findIntegrationsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findIntegrationInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        Validator.validatePageLink(pageLink);
        return integrationInfoDao.findIntegrationsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    private final PaginatedRemover<TenantId, Integration> tenantIntegrationsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Integration> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return integrationDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Integration entity) {
            deleteIntegration(tenantId, new IntegrationId(entity.getId().getId()));
        }

    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findIntegrationById(tenantId, new IntegrationId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findIntegrationByIdAsync(tenantId, new IntegrationId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return integrationDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}
