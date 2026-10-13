// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("DashboardDaoService")
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

    @Autowired
    protected TbTransactionalCache<DashboardId, String> cache;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JpaExecutorService executor;

    protected void publishEvictEvent(DashboardTitleEvictEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    @TransactionalEventListener(classes = DashboardTitleEvictEvent.class)
    public void handleEvictEvent(DashboardTitleEvictEvent event) {
        cache.evict(event.getKey());
    }

    @Override
    public Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, id -> INCORRECT_DASHBOARD_ID + id);
        return dashboardDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardByIdAsync [{}]", dashboardId);
        validateId(dashboardId, id -> INCORRECT_DASHBOARD_ID + id);
        return dashboardDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, id -> INCORRECT_DASHBOARD_ID + id);
        return dashboardInfoDao.findById(tenantId, dashboardId.getId());
    }

    @Override
    public String findDashboardTitleById(TenantId tenantId, DashboardId dashboardId) {
        return cache.getAndPutInTransaction(dashboardId,
                () -> dashboardInfoDao.findTitleById(tenantId.getId(), dashboardId.getId()), true);
    }

    @Override
    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoByIdAsync [{}]", dashboardId);
        validateId(dashboardId, id -> INCORRECT_DASHBOARD_ID + id);
        return dashboardInfoDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    @Override
    public ListenableFuture<List<DashboardInfo>> findDashboardInfoByIdsAsync(TenantId tenantId, List<DashboardId> dashboardIds) {
        log.trace("Executing findDashboardInfoByIdsAsync, dashboardIds [{}]", dashboardIds);
        validateIds(dashboardIds, ids -> "Incorrect dashboardIds " + ids);
        return dashboardInfoDao.findDashboardsByIdsAsync(tenantId.getId(), toUUIDs(dashboardIds));
    }

    @Override
    public List<DashboardInfo> findDashboardInfoByIds(TenantId tenantId, List<DashboardId> dashboardIds) {
        log.trace("Executing findDashboardInfoByIds, dashboardIds [{}]", dashboardIds);
        return dashboardInfoDao.findDashboardsByIds(tenantId.getId(), toUUIDs(dashboardIds));
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        return saveDashboard(dashboard, true);
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard, boolean doValidate) {
        return saveEntity(dashboard, () -> doSaveDashboard(dashboard, doValidate));
    }

    private Dashboard doSaveDashboard(Dashboard dashboard, boolean doValidate) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        if (doValidate) {
            dashboardValidator.validate(dashboard, Dashboard::getTenantId);
        }
        try {
            TenantId tenantId = dashboard.getTenantId();
            if (CollectionUtils.isNotEmpty(dashboard.getResources())) {
                resourceService.importResources(tenantId, dashboard.getCustomerId(), dashboard.getResources());
            }
            imageService.updateImagesUsage(dashboard);
            resourceService.updateResourcesUsage(tenantId, dashboard);

            var saved = dashboardDao.save(tenantId, dashboard);
            if (dashboard.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(tenantId, saved.getOwnerId(), saved.getId());
                countService.publishCountEntityEvictEvent(tenantId, EntityType.DASHBOARD);
            }
            publishEvictEvent(new DashboardTitleEvictEvent(saved.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                    .entityId(saved.getId()).entity(saved).created(dashboard.getId() == null).build());
            return saved;
        } catch (Exception e) {
            if (dashboard.getId() != null) {
                publishEvictEvent(new DashboardTitleEvictEvent(dashboard.getId()));
            }
            checkConstraintViolation(e, "dashboard_external_id_unq_key", "Dashboard with such external id already exists!");
            throw e;
        }
    }

    @Override
    public Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
        }
        if (!customer.getTenantId().getId().equals(dashboard.getTenantId().getId())) {
            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
        }
        if (dashboard.addAssignedCustomer(customer)) {
            try {
                createRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to create dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent customer!");
        }
        if (dashboard.removeAssignedCustomer(customer)) {
            try {
                deleteRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    @Transactional
    public void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, id -> INCORRECT_DASHBOARD_ID + id);
        try {
            dashboardDao.removeById(tenantId, dashboardId.getId());
            publishEvictEvent(new DashboardTitleEvictEvent(dashboardId));
            countService.publishCountEntityEvictEvent(tenantId, EntityType.DASHBOARD);
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(dashboardId).build());
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "fk_default_dashboard_device_profile", "The dashboard is referenced by a device profile",
                    "fk_default_dashboard_asset_profile", "The dashboard is referenced by an asset profile"
            ));
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteDashboard(tenantId, (DashboardId) id);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public Long countDashboards() {
        log.trace("Executing countDashboards");
        return dashboardDao.countDashboards();
    }

    @Override
    public PageData<DashboardInfo> findTenantDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findTenantDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteDashboardsByTenantId(tenantId);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerIdIncludingSubsCustomers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        customerDashboardsRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findDashboardsByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, id -> "Incorrect entityGroupId " + id);
        validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findDashboardsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    @Override
    public PageData<DashboardInfo> findMobileDashboardsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByEntityGroupIds(toUUIDs(groupIds), pageLink);
    }

    @Override
    public DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findFirstDashboardInfoByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return dashboardInfoDao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public ListenableFuture<DashboardInfo> findFirstDashboardInfoByTenantIdAndNameAsync(TenantId tenantId, String name) {
        log.trace("Executing findFirstDashboardInfoByTenantIdAndNameAsync [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return executor.submit(() -> findFirstDashboardInfoByTenantIdAndName(tenantId, name));
    }

    @Override
    public List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title) {
        return dashboardDao.findByTenantIdAndTitle(tenantId.getId(), title);
    }

    @Override
    public boolean existsById(TenantId tenantId, DashboardId dashboardId) {
        return dashboardDao.existsById(tenantId, dashboardId.getId());
    }

    @Override
    public PageData<DashboardId> findAllDashboardsIds(PageLink pageLink) {
        return dashboardDao.findAllIds(pageLink);
    }

    private final PaginatedRemover<TenantId, DashboardId> tenantDashboardsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<DashboardId> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return dashboardDao.findIdsByTenantId(id, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardId dashboardId) {
            deleteDashboard(tenantId, dashboardId);
        }
    };

    private final PaginatedRemover<CustomerId, DashboardInfo> customerDashboardsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            deleteDashboard(tenantId, new DashboardId(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDashboardById(tenantId, new DashboardId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findDashboardByIdAsync(tenantId, new DashboardId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return dashboardDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

    @Override
    public List<Dashboard> exportDashboards(TenantId tenantId, EntityGroupId entityGroupId, TimePageLink pageLink) throws ThingsboardException {
        PageData<DashboardInfo> pageData = findDashboardsByEntityGroupId(entityGroupId, pageLink);
        if (pageData != null && !CollectionUtils.isEmpty(pageData.getData())) {
            List<DashboardInfo> dashboardViews = pageData.getData();
            Map<DashboardId, DashboardId> idMapping = new HashMap<>();
            List<Dashboard> dashboards = new ArrayList<>();
            for (DashboardInfo dashboardInfo : dashboardViews) {
                Dashboard dashboard = findDashboardById(tenantId, dashboardInfo.getId());
                DashboardId oldDashboardId = dashboard.getId();
                DashboardId newDashboardId = new DashboardId(Uuids.timeBased());
                idMapping.put(oldDashboardId, newDashboardId);
                dashboard.setId(newDashboardId);
                dashboard.setTenantId(null);
                dashboard.setCustomerId(null);
                dashboards.add(dashboard);
            }
            for (Dashboard dashboard : dashboards) {
                JsonNode configuration = dashboard.getConfiguration();
                searchDashboardIdRecursive(idMapping, configuration);
            }
            return dashboards;
        }
        return Collections.emptyList();
    }

    private void searchDashboardIdRecursive(Map<DashboardId, DashboardId> idMapping, JsonNode node) throws ThingsboardException {
        Iterator<String> iter = node.fieldNames();
        boolean isDashboardId = false;
        try {
            while (iter.hasNext()) {
                String field = iter.next();
                if ("targetDashboardId".equals(field)) {
                    isDashboardId = true;
                    break;
                }
            }
            if (isDashboardId) {
                ObjectNode objNode = (ObjectNode) node;
                String oldDashboardIdStr = node.get("targetDashboardId").asText();
                DashboardId dashboardId = new DashboardId(UUID.fromString(oldDashboardIdStr));
                DashboardId replacement = idMapping.get(dashboardId);
                if (replacement != null) {
                    objNode.put("targetDashboardId", replacement.getId().toString());
                }
            } else {
                for (JsonNode jsonNode : node) {
                    searchDashboardIdRecursive(idMapping, jsonNode);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public void importDashboards(TenantId tenantId, EntityGroupId entityGroupId, List<Dashboard> dashboards, boolean overwrite) throws ThingsboardException {
        EntityGroup dashboardGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
        resetDashboardOwnerCustomer(tenantId, dashboardGroup.getOwnerId(), dashboards);
        if (overwrite) {
            PageData<DashboardInfo> dashboardData = findDashboardsByEntityGroupId(entityGroupId, new PageLink(Integer.MAX_VALUE));
            try {
                List<DashboardInfo> dashboardInfos = dashboardData.getData();
                if (!CollectionUtils.isEmpty(dashboardInfos)) {
                    replaceOverwriteDashboardIds(dashboards, dashboardInfos);
                } else {
                    replaceDashboardIds(dashboards);
                }
                dashboards.forEach(d -> saveDashboardToEntityGroup(tenantId, entityGroupId, d));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
            }
        } else {
            try {
                replaceDashboardIds(dashboards);
                dashboards.forEach(d -> saveDashboardToEntityGroup(tenantId, entityGroupId, d));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
            }
        }
    }

    private void saveDashboardToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, Dashboard dashboard) {
        Dashboard savedDashboard = saveDashboard(dashboard);
        entityGroupService.addEntityToEntityGroupAll(savedDashboard.getTenantId(), savedDashboard.getOwnerId(), savedDashboard.getId());
        entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, savedDashboard.getId());
    }

    private void replaceOverwriteDashboardIds(List<Dashboard> dashboards, List<DashboardInfo> persistentDashboards) throws Exception {
        Map<DashboardId, DashboardId> idMapping = new HashMap<>();
        for (Dashboard dashboard : dashboards) {
            Optional<DashboardInfo> overwriteDashboardInfoOpt = persistentDashboards.stream().filter(d -> d.getTitle().equals(dashboard.getTitle())).findAny();
            DashboardId importDashboardId = dashboard.getId();
            DashboardId overwriteDashboardId;
            if (overwriteDashboardInfoOpt.isPresent()) {
                overwriteDashboardId = overwriteDashboardInfoOpt.get().getId();
            } else {
                overwriteDashboardId = new DashboardId(Uuids.timeBased());
            }
            idMapping.put(importDashboardId, overwriteDashboardId);
            dashboard.setId(overwriteDashboardId);
        }
        for (Dashboard dashboard : dashboards) {
            JsonNode configuration = dashboard.getConfiguration();
            searchDashboardIdRecursive(idMapping, configuration);
        }
    }

    private void resetDashboardOwnerCustomer(TenantId tenantId, EntityId ownerId, List<Dashboard> dashboards) {
        for (Dashboard dashboard : dashboards) {
            dashboard.setTenantId(tenantId);
            dashboard.setOwnerId(ownerId);
        }
    }

    List<Dashboard> replaceDashboardIds(List<Dashboard> dashboards) throws Exception {
        Map<DashboardId, DashboardId> idMapping = new HashMap<>();
        for (Dashboard dashboard : dashboards) {
            if (dashboard.getId() != null) {
                DashboardId oldId = dashboard.getId();
                DashboardId newId = new DashboardId(Uuids.timeBased());
                idMapping.put(oldId, newId);
                dashboard.setId(newId);
            } else {
                DashboardId newId = new DashboardId(Uuids.timeBased());
                dashboard.setId(newId);
            }
        }
        for (Dashboard dashboard : dashboards) {
            searchDashboardIdRecursive(idMapping, dashboard.getConfiguration());
        }
        return dashboards;
    }

}
