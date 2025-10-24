/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.dashboard;

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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

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
    private EdgeDao edgeDao;

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
    public Dashboard saveDashboard(Dashboard dashboard) {
        return saveDashboard(dashboard, true);
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard, boolean doValidate) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        if (doValidate) {
            dashboardValidator.validate(dashboard, DashboardInfo::getTenantId);
        }
        try {
            TenantId tenantId = dashboard.getTenantId();
            if (CollectionUtils.isNotEmpty(dashboard.getResources())) {
                resourceService.importResources(tenantId, dashboard.getResources());
            }
            imageService.updateImagesUsage(dashboard);
            resourceService.updateResourcesUsage(tenantId, dashboard);

            var saved = dashboardDao.save(tenantId, dashboard);
            publishEvictEvent(new DashboardTitleEvictEvent(saved.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                    .entityId(saved.getId()).entity(saved).created(dashboard.getId() == null).build());
            if (dashboard.getId() == null) {
                countService.publishCountEntityEvictEvent(tenantId, EntityType.DASHBOARD);
            }
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

    private void updateAssignedCustomer(TenantId tenantId, DashboardId dashboardId, Customer customer) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        if (dashboard.updateAssignedCustomer(customer)) {
            saveDashboard(dashboard);
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
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
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
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboards from non-existent customer!");
        }
        new CustomerDashboardsRemover(customer).removeEntities(tenantId, customer);
    }

    @Override
    public void updateCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing updateCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, id -> "Incorrect customerId " + id);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't update dashboards for non-existent customer!");
        }
        new CustomerDashboardsUpdater(customer).removeEntities(tenantId, customer);
    }

    @Override
    public Dashboard assignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent edge!");
        }
        if (!edge.getTenantId().equals(dashboard.getTenantId())) {
            throw new DataValidationException("Can't assign dashboard to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return dashboard;
    }

    @Override
    public Dashboard unassignDashboardFromEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return dashboard;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
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

    private class CustomerDashboardsRemover extends PaginatedRemover<Customer, DashboardInfo> {

        private final Customer customer;

        CustomerDashboardsRemover(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            unassignDashboardFromCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer.getId());
        }

    }

    private class CustomerDashboardsUpdater extends PaginatedRemover<Customer, DashboardInfo> {

        private final Customer customer;

        CustomerDashboardsUpdater(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            updateAssignedCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer);
        }

    }

}
