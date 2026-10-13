// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.BaseAgentProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.component.RuleNodeClassInfo;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.DbUpgradeExecutorService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.lts.LtsMigrationService;
import org.thingsboard.server.utils.TbNodeUpgradeUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultDataUpdateService implements DataUpdateService {

    private static final int MAX_PENDING_SAVE_RULE_NODE_FUTURES = 256;
    private static final int BACKFILL_BATCH_SIZE = 500;
    private static final int DEFAULT_PAGE_SIZE = 1024;
    private static final int DEFAULT_LIMIT = 100;

    private final TenantService tenantService;
    private final RelationService relationService;
    private final RuleChainService ruleChainService;
    private final IntegrationService integrationService;
    private final EntityGroupService entityGroupService;
    private final UserService userService;
    private final WhiteLabelingService whiteLabelingService;
    private final CustomerService customerService;
    private final AssetService assetService;
    private final DeviceService deviceService;
    private final DashboardService dashboardService;
    private final EntityViewService entityViewService;
    private final EdgeService edgeService;
    private final AgentProfileService agentProfileService;
    private final SystemDataLoaderService systemDataLoaderService;
    private final ComponentDiscoveryService componentDiscoveryService;
    private final DbUpgradeExecutorService executorService;
    private final DatabaseSchemaSettingsService schemaSettingsService;
    private final LtsMigrationService ltsMigrationService;

    @Override
    public void updateData(boolean fromCe) throws Exception {
        log.info("Updating data ...");
        if (fromCe) {
            updateDataFromCe();
        }
        //TODO: should be cleaned after each release
        backfillForExistingTenants();
        ltsMigrationService.runDataMigrations(schemaSettingsService.getDbSchemaVersion(), schemaSettingsService.getPackageSchemaVersion(),
                schemaSettingsService.isForcedReUpgrade());
        log.info("Data updated.");
    }

    private void backfillForExistingTenants() throws Exception {
        log.info("Starting backfill for existing tenants ...");

        List<Consumer<TenantId>> tenantBackfillTasks = List.of(
                this::backfillAgentGroupAllForTenant,
                this::ensureDefaultAgentProfile
        );

        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, BACKFILL_BATCH_SIZE);
        List<ListenableFuture<?>> batch = new ArrayList<>(BACKFILL_BATCH_SIZE);
        AtomicInteger count = new AtomicInteger();

        for (TenantId tenantId : tenants) {
            batch.add(executorService.submit(() -> {
                for (Consumer<TenantId> task : tenantBackfillTasks) {
                    try {
                        task.accept(tenantId);
                    } catch (Exception e) {
                        log.error("Backfill task failed for tenant [{}]", tenantId, e);
                    }
                }
                count.incrementAndGet();
            }));
            if (batch.size() >= BACKFILL_BATCH_SIZE) {
                Futures.allAsList(batch).get();
                batch.clear();
                log.info("{} tenants processed", count.get());
            }
        }
        Futures.allAsList(batch).get();

        log.info("Backfill for existing tenants completed. Processed {} tenants.", count.get());
    }

    private void backfillAgentGroupAllForTenant(TenantId tenantId) {
        ensureAgentGroupAll(tenantId);
        new PageDataIterable<>(
                pageLink -> customerService.findCustomersByTenantId(tenantId, pageLink), DEFAULT_PAGE_SIZE
        ).forEach(customer -> {
            if (customer.getId() == null || customer.getId().isNullUid()) {
                return;
            }
            ensureAgentGroupAll(customer.getId());
        });
    }

    private void ensureAgentGroupAll(EntityId parentId) {
        Optional<EntityGroup> existing = entityGroupService.findEntityGroupByTypeAndName(
                TenantId.SYS_TENANT_ID, parentId, EntityType.AGENT, EntityGroup.GROUP_ALL_NAME);
        if (existing.isEmpty()) {
            entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, parentId, EntityType.AGENT);
        }
    }

    private void ensureDefaultAgentProfile(TenantId tenantId) {
        agentProfileService.findOrCreateAgentProfile(tenantId, BaseAgentProfileService.DEFAULT_AGENT_PROFILE_NAME);
    }

    @Override
    @Transactional
    public void postUpdateData() throws Exception {
        //TODO: should be cleaned after each release

    }

    private void updateDataFromCe() throws Exception {
        tenantsCustomersGroupAllUpdater.updateEntities();
        tenantEntitiesGroupAllUpdater.updateEntities();
        tenantIntegrationUpdater.updateEntities();
        //for 2.4.0
        JsonNode mailTemplatesSettings = whiteLabelingService.findMailTemplatesByTenantId(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
        if (mailTemplatesSettings.isEmpty()) {
            systemDataLoaderService.loadMailTemplates();
        } else {
            systemDataLoaderService.updateMailTemplates(mailTemplatesSettings);
        }
    }

    @Override
    public void upgradeRuleNodes() {
        int totalRuleNodesUpgraded = 0;
        log.info("Starting rule nodes upgrade ...");
        var nodeClassToVersionMap = componentDiscoveryService.getVersionedNodes();
        log.debug("Found {} versioned nodes to check for upgrade!", nodeClassToVersionMap.size());
        for (var ruleNodeClassInfo : nodeClassToVersionMap) {
            var ruleNodeTypeForLogs = ruleNodeClassInfo.getSimpleName();
            var toVersion = ruleNodeClassInfo.getCurrentVersion();
            try {
                log.debug("Going to check for nodes with type: {} to upgrade to version: {}.", ruleNodeTypeForLogs, toVersion);
                var ruleNodesIdsToUpgrade = getRuleNodesIdsWithTypeAndVersionLessThan(ruleNodeClassInfo.getClassName(), toVersion);
                if (ruleNodesIdsToUpgrade.isEmpty()) {
                    log.debug("There are no active nodes with type {}, or all nodes with this type already set to latest version!", ruleNodeTypeForLogs);
                    continue;
                }
                var ruleNodeIdsPartitions = Lists.partition(ruleNodesIdsToUpgrade, MAX_PENDING_SAVE_RULE_NODE_FUTURES);
                for (var ruleNodePack : ruleNodeIdsPartitions) {
                    totalRuleNodesUpgraded += processRuleNodePack(ruleNodePack, ruleNodeClassInfo);
                    log.info("{} upgraded rule nodes so far ...", totalRuleNodesUpgraded);
                }
            } catch (Exception e) {
                log.error("Unexpected error during {} rule nodes upgrade: ", ruleNodeTypeForLogs, e);
            }
        }
        log.info("Finished rule nodes upgrade. Upgraded rule nodes count: {}", totalRuleNodesUpgraded);
    }

    private int processRuleNodePack(List<RuleNodeId> ruleNodeIdsBatch, RuleNodeClassInfo ruleNodeClassInfo) {
        var saveFutures = new ArrayList<ListenableFuture<?>>(MAX_PENDING_SAVE_RULE_NODE_FUTURES);
        String ruleNodeType = ruleNodeClassInfo.getSimpleName();
        int toVersion = ruleNodeClassInfo.getCurrentVersion();
        var ruleNodesPack = ruleChainService.findAllRuleNodesByIds(ruleNodeIdsBatch);
        for (var ruleNode : ruleNodesPack) {
            if (ruleNode == null) {
                continue;
            }
            var ruleNodeId = ruleNode.getId();
            int fromVersion = ruleNode.getConfigurationVersion();
            log.debug("Going to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                    ruleNodeId, ruleNodeType, fromVersion, toVersion);
            try {
                TbNodeUpgradeUtils.upgradeConfigurationAndVersion(ruleNode, ruleNodeClassInfo);
                saveFutures.add(executorService.submit(() -> {
                    ruleChainService.saveRuleNode(TenantId.SYS_TENANT_ID, ruleNode);
                    log.debug("Successfully upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, fromVersion, toVersion);
                }));
            } catch (Exception e) {
                log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {} due to: ",
                        ruleNodeId, ruleNodeType, fromVersion, toVersion, e);
            }
        }
        try {
            return Futures.allAsList(saveFutures).get().size();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to process save rule nodes requests due to: ", e);
        }
    }

    private List<RuleNodeId> getRuleNodesIdsWithTypeAndVersionLessThan(String type, int toVersion) {
        var ruleNodeIds = new ArrayList<RuleNodeId>();
        new PageDataIterable<>(pageLink ->
                ruleChainService.findAllRuleNodeIdsByTypeAndVersionLessThan(type, toVersion, pageLink), DEFAULT_PAGE_SIZE
        ).forEach(ruleNodeIds::add);
        return ruleNodeIds;
    }

    private PaginatedUpdater<String, Tenant> tenantsCustomersGroupAllUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants customers group all updater";
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    new EntityGroupsOwnerUpdater(tenant.getId()).updateEntities(tenant.getId());
                    EntityGroup entityGroup;
                    Optional<EntityGroup> customerGroupOptional =
                            entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
                    entityGroup = customerGroupOptional.orElseGet(() -> entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), EntityType.CUSTOMER));
                    new CustomersGroupAllUpdater(entityGroup).updateEntities(tenant.getId());
                }
            };

    private PaginatedUpdater<String, Tenant> tenantEntitiesGroupAllUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenant entities group all updater";
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.AGENT};
                        for (EntityType groupType : entityGroupTypes) {
                            EntityGroup entityGroup;
                            Optional<EntityGroup> entityGroupOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, tenant.getId(), groupType, EntityGroup.GROUP_ALL_NAME);
                            boolean fetchAllTenantEntities;
                            if (entityGroupOptional.isEmpty()) {
                                entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, tenant.getId(), groupType);
                                fetchAllTenantEntities = true;
                            } else {
                                entityGroup = entityGroupOptional.get();
                                fetchAllTenantEntities = false;
                            }
                            switch (groupType) {
                                case USER:
                                    new CustomerUsersTenantGroupAllRemover(entityGroup).updateEntities(tenant.getId());
                                    entityGroupService.findOrCreateTenantUsersGroup(tenant.getId());
                                    Optional<EntityGroup> tenantAdminsOptional =
                                            entityGroupService.findEntityGroupByTypeAndName(tenant.getId(), tenant.getId(), EntityType.USER, EntityGroup.GROUP_TENANT_ADMINS_NAME);
                                    if (tenantAdminsOptional.isEmpty()) {
                                        EntityGroup tenantAdmins = entityGroupService.findOrCreateTenantAdminsGroup(tenant.getId());
                                        new TenantAdminsGroupAllUpdater(entityGroup, tenantAdmins).updateEntities(tenant.getId());
                                    }
                                    break;
                                case ASSET:
                                    new AssetsGroupAllUpdater(assetService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DEVICE:
                                    new DevicesGroupAllUpdater(deviceService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case ENTITY_VIEW:
                                    new EntityViewGroupAllUpdater(entityViewService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case EDGE:
                                    new EdgesGroupAllUpdater(edgeService, customerService, entityGroupService, entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                                case DASHBOARD:
                                    new DashboardsGroupAllUpdater(entityGroup, fetchAllTenantEntities).updateEntities(tenant.getId());
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private class EntityGroupsOwnerUpdater extends PaginatedUpdater<EntityId, EntityGroup> {

        private final EntityId ownerId;

        public EntityGroupsOwnerUpdater(EntityId ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        protected String getName() {
            return "Entity groups owner updater";
        }

        @Override
        protected PageData<EntityGroup> findEntities(EntityId parentEntityId, PageLink pageLink) {
            return entityGroupService.findAllEntityGroupsByParentRelation(TenantId.SYS_TENANT_ID, parentEntityId, pageLink);
        }

        @Override
        protected void updateEntity(EntityGroup entityGroup) {
            if (entityGroup.getOwnerId() == null || entityGroup.getOwnerId().isNullUid()) {
                entityGroup.setOwnerId(this.ownerId);
                entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, this.ownerId, entityGroup);
            }
        }

    }

    private class TenantAdminsGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, User> {

        private final EntityGroup tenantAdmins;

        public TenantAdminsGroupAllUpdater(EntityGroup groupAll, EntityGroup tenantAdmins) {
            super(groupAll);
            this.tenantAdmins = tenantAdmins;
        }

        @Override
        protected String getName() {
            return "Tenant admins group all updater";
        }

        @Override
        protected PageData<User> findEntities(TenantId id, PageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, tenantAdmins.getId(), entity.getId());
        }

    }

    private class CustomerUsersTenantGroupAllRemover extends PaginatedUpdater<TenantId, User> {

        private final EntityGroup groupAll;

        public CustomerUsersTenantGroupAllRemover(EntityGroup groupAll) {
            this.groupAll = groupAll;
        }

        @Override
        protected String getName() {
            return "Customer users tenant group all remover";
        }

        @Override
        protected PageData<User> findEntities(TenantId id, PageLink pageLink) {
            try {
                List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(TenantId.SYS_TENANT_ID, groupAll.getId(), new PageLink(Integer.MAX_VALUE)).get();
                List<UserId> userIds = entityIds.stream().map(entityId -> new UserId(entityId.getId())).collect(Collectors.toList());
                List<User> users;
                if (!userIds.isEmpty()) {
                    users = userService.findUsersByTenantIdAndIds(id, userIds);
                } else {
                    users = Collections.emptyList();
                }
                return new PageData<>(users, 1, users.size(), false);
            } catch (Exception e) {
                log.error("Failed to get users from group all!", e);
                throw new RuntimeException("Failed to get users from group all!", e);
            }
        }

        @Override
        protected void updateEntity(User entity) {
            if (Authority.CUSTOMER_USER.equals(entity.getAuthority())) {
                entityGroupService.removeEntityFromEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            }
        }

    }

    private class CustomerUsersGroupAllUpdater extends GroupAllPaginatedUpdater<CustomerId, User> {

        private final TenantId tenantId;
        private final EntityGroup customerUsers;

        public CustomerUsersGroupAllUpdater(TenantId tenantId, EntityGroup groupAll, EntityGroup customerUsers) {
            super(groupAll);
            this.tenantId = tenantId;
            this.customerUsers = customerUsers;
        }

        @Override
        protected String getName() {
            return "Customer users group all updater";
        }

        @Override
        protected PageData<User> findEntities(CustomerId id, PageLink pageLink) {
            return userService.findCustomerUsers(this.tenantId, id, pageLink);
        }

        @Override
        protected void updateGroupEntity(User entity, EntityGroup groupAll) {
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), entity.getId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, customerUsers.getId(), entity.getId());
        }

    }

    private class CustomersGroupAllUpdater extends GroupAllPaginatedUpdater<TenantId, Customer> {

        public CustomersGroupAllUpdater(EntityGroup groupAll) {
            super(groupAll);
        }

        @Override
        protected String getName() {
            return "Customers group all updater";
        }

        @Override
        protected PageData<Customer> findEntities(TenantId id, PageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }

        @Override
        protected void updateGroupEntity(Customer customer, EntityGroup groupAll) {
            if (customer.getId() == null || customer.getId().isNullUid()) {
                log.warn("Customer has invalid id [{}]", customer.getId());
                log.warn("[{}]", customer);
                return;
            }
            if (customer.isSubCustomer()) {
                return;
            }
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, groupAll.getId(), customer.getId());
            new EntityGroupsOwnerUpdater(customer.getId()).updateEntities(customer.getId());
            EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.DASHBOARD, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.AGENT};
            for (EntityType groupType : entityGroupTypes) {
                Optional<EntityGroup> entityGroupOptional =
                        entityGroupService.findEntityGroupByTypeAndName(TenantId.SYS_TENANT_ID, customer.getId(), groupType, EntityGroup.GROUP_ALL_NAME);
                if (entityGroupOptional.isEmpty()) {
                    EntityGroup entityGroup = entityGroupService.createEntityGroupAll(TenantId.SYS_TENANT_ID, customer.getId(), groupType);
                    if (groupType == EntityType.USER) {
                        if (!customer.isPublic()) {
                            entityGroupService.findOrCreateCustomerAdminsGroup(customer.getTenantId(), customer.getId(), null);
                            Optional<EntityGroup> customerUsersOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(customer.getTenantId(), customer.getId(), EntityType.USER, EntityGroup.GROUP_CUSTOMER_USERS_NAME);
                            if (customerUsersOptional.isEmpty()) {
                                EntityGroup customerUsers = entityGroupService.findOrCreateCustomerUsersGroup(customer.getTenantId(), customer.getId(), null);
                                new CustomerUsersGroupAllUpdater(customer.getTenantId(), entityGroup, customerUsers).updateEntities(customer.getId());
                            }
                        } else {
                            entityGroupService.findOrCreatePublicUsersGroup(customer.getTenantId(), customer.getId());
                        }
                    }
                }
            }
        }

    }


    private class DashboardsGroupAllUpdater extends PaginatedUpdater<TenantId, DashboardInfo> {

        private final EntityGroup groupAll;
        private final boolean fetchAllTenantEntities;

        private Map<CustomerId, EntityGroupId> customersGroupMap = new HashMap<>();
        private Map<CustomerId, Customer> customersMap = new HashMap<>();

        public DashboardsGroupAllUpdater(EntityGroup groupAll,
                                         boolean fetchAllTenantEntities) {
            this.groupAll = groupAll;
            this.fetchAllTenantEntities = fetchAllTenantEntities;
        }

        @Override
        protected String getName() {
            return "Dashboards group all updater";
        }

        @Override
        protected PageData<DashboardInfo> findEntities(TenantId id, PageLink pageLink) {
            if (fetchAllTenantEntities) {
                return dashboardService.findDashboardsByTenantId(id, pageLink);
            } else {
                try {
                    List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(TenantId.SYS_TENANT_ID, groupAll.getId(), new PageLink(Integer.MAX_VALUE)).get();
                    List<DashboardId> dashboardIds = entityIds.stream().map(entityId -> new DashboardId(entityId.getId())).collect(Collectors.toList());
                    List<DashboardInfo> dashboards;
                    if (!dashboardIds.isEmpty()) {
                        dashboards = dashboardService.findDashboardInfoByIds(TenantId.SYS_TENANT_ID, dashboardIds);
                    } else {
                        dashboards = Collections.emptyList();
                    }
                    return new PageData<>(dashboards, 1, dashboards.size(), false);
                } catch (Exception e) {
                    log.error("Failed to get dashboards from group all!", e);
                    throw new RuntimeException("Failed to get dashboards from group all!", e);
                }
            }
        }

        @Override
        protected void updateEntity(DashboardInfo entity) {
            entityGroupService.addEntityToEntityGroupAll(TenantId.SYS_TENANT_ID, entity.getTenantId(), entity.getId());
            if (entity.getAssignedCustomers() != null) {
                for (ShortCustomerInfo customer : entity.getAssignedCustomers()) {
                    Customer customer1 = customersMap.computeIfAbsent(customer.getCustomerId(), customerId ->
                            customerService.findCustomerById(entity.getTenantId(), customer.getCustomerId()));
                    if (customer1 != null) {
                        EntityGroupId customerEntityGroupId = customersGroupMap.computeIfAbsent(
                                customer.getCustomerId(), customerId ->
                                        entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(entity.getTenantId(),
                                                customerId, entity.getEntityType()).getId()
                        );
                        entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, customerEntityGroupId, entity.getId());
                        dashboardService.unassignDashboardFromCustomer(TenantId.SYS_TENANT_ID, entity.getId(), customer.getCustomerId());
                    } else {
                        Dashboard dashboard = dashboardService.findDashboardById(TenantId.SYS_TENANT_ID, entity.getId());
                        if (dashboard.removeAssignedCustomerInfo(customer)) {
                            EntityRelation relationToDelete =
                                    new EntityRelation(customer.getCustomerId(), entity.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD);
                            relationService.deleteRelation(TenantId.SYS_TENANT_ID, relationToDelete);
                            dashboardService.saveDashboard(dashboard);
                        }
                    }
                }
            }
        }

    }

    private PaginatedUpdater<String, Tenant> tenantIntegrationUpdater = new PaginatedUpdater<String, Tenant>() {
        @Override
        protected PageData<Tenant> findEntities(String id, PageLink pageLink) {
            return tenantService.findTenants(pageLink);
        }

        @Override
        protected String getName() {
            return "Tenant integration updater";
        }

        @Override
        protected void updateEntity(Tenant tenant) {
            updateTenantIntegrations(tenant.getId());
        }
    };

    private void updateTenantIntegrations(TenantId tenantId) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        PageData<Integration> pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
        boolean hasNext = true;
        while (hasNext) {
            for (Integration integration : pageData.getData()) {
                try {
                    Field enabledField = AbstractIntegration.class.getDeclaredField("enabled");
                    enabledField.setAccessible(true);
                    Boolean booleanVal = (Boolean) enabledField.get(integration);
                    if (booleanVal == null) {
                        integration.setEnabled(true);
                        integrationService.saveIntegration(integration);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
                pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
            } else {
                hasNext = false;
            }
        }
    }

    public static boolean getEnv(String name, boolean defaultValue) {
        String env = System.getenv(name);
        if (env == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(env);
        }
    }

}
