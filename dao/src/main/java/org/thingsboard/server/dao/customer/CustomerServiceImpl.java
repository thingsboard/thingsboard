// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.customer.CustomerCacheEvictEvent;
import org.thingsboard.server.cache.customer.CustomerCacheKey;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("CustomerDaoService")
@Slf4j
public class CustomerServiceImpl extends AbstractCachedEntityService<CustomerCacheKey, Customer, CustomerCacheEvictEvent> implements CustomerService {

    public static final String PUBLIC_CUSTOMER_SUFFIX = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_OWNER_ID = "Incorrect ownerId ";
    public static final JsonNode PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON = JacksonUtil.toJsonNode("{\"isPublic\":true}");
    public static final String CUSTOMER_UNIQUE_TITLE_EX_MSG = "Customer with such title already exists!";

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private CustomerInfoDao customerInfoDao;

    @Autowired
    private UserService userService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AgentService agentService;

    @Lazy
    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private BlobEntityService blobEntityService;

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Lazy
    @Autowired
    private ReportService reportService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private DataValidator<Customer> customerValidator;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private JpaExecutorService executor;

    @TransactionalEventListener(classes = CustomerCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomerCacheEvictEvent event) {
        List<CustomerCacheKey> keys = new ArrayList<>(2);
        keys.add(new CustomerCacheKey(event.tenantId(), event.newTitle()));
        if (StringUtils.isNotEmpty(event.oldTitle()) && !event.oldTitle().equals(event.newTitle())) {
            keys.add(new CustomerCacheKey(event.tenantId(), event.oldTitle()));
        }
        cache.evict(keys);
    }

    @Override
    public Customer findCustomerById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerDao.findById(tenantId, customerId.getId());
    }

    @Override
    public CustomerInfo findCustomerInfoById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerInfoById [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerInfoDao.findById(tenantId, customerId.getId());
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitle [{}] [{}]", tenantId, title);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return Optional.ofNullable(cache.getAndPutInTransaction(new CustomerCacheKey(tenantId, title),
                () -> customerDao.findCustomerByTenantIdAndTitle(tenantId.getId(), title)
                        .orElse(null), true));
    }

    @Override
    public ListenableFuture<Optional<Customer>> findCustomerByTenantIdAndTitleAsync(TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitleAsync [{}] [{}]", tenantId, title);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return executor.submit(() -> findCustomerByTenantIdAndTitle(tenantId, title));
    }

    @Override
    public ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerByIdAsync [{}]", customerId);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return customerDao.findByIdAsync(tenantId, customerId.getId());
    }

    @Override
    public ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(TenantId tenantId, List<CustomerId> customerIds) {
        log.trace("Executing findCustomersByTenantIdAndIdsAsync, tenantId [{}], customerIds [{}]", tenantId, customerIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(customerIds, ids -> "Incorrect customerIds " + ids);
        return customerDao.findCustomersByTenantIdAndIdsAsync(tenantId.getId(), customerIds.stream().map(CustomerId::getId).collect(Collectors.toList()));
    }

    @Override
    public List<Customer> findCustomersByTenantIdAndIds(TenantId tenantId, List<CustomerId> customerIds) {
        log.trace("Executing findCustomersByTenantIdAndIds, tenantId [{}], customerIds [{}]", tenantId, customerIds);
        return customerDao.findCustomersByTenantIdAndIds(tenantId.getId(), customerIds.stream().map(CustomerId::getId).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Customer saveCustomer(Customer customer) {
        return saveCustomer(customer, NameConflictStrategy.DEFAULT);
    }

    @Override
    @Transactional
    public Customer saveCustomer(Customer customer, NameConflictStrategy nameConflictStrategy) {
        return saveEntity(customer, () -> saveCustomer(customer, true, nameConflictStrategy));
    }

    private Customer saveCustomer(Customer customer, boolean doValidate) {
        return saveCustomer(customer, doValidate, NameConflictStrategy.DEFAULT);
    }

    private Customer saveCustomer(Customer customer, boolean doValidate, NameConflictStrategy nameConflictStrategy) {
        log.trace("Executing saveCustomer [{}]", customer);
        Customer oldCustomer = (customer.getId() != null) ? customerDao.findById(customer.getTenantId(), customer.getId().getId()) : null;
        if (nameConflictStrategy.policy() == NameConflictPolicy.UNIQUIFY && (oldCustomer == null || !oldCustomer.getTitle().equals(customer.getTitle()))) {
            uniquifyEntityName(customer, oldCustomer, customer::setTitle, EntityType.CUSTOMER, nameConflictStrategy);
        }
        if (doValidate) {
            customerValidator.validate(customer, Customer::getTenantId);
        }
        String oldCustomerTitle = oldCustomer != null ? oldCustomer.getTitle() : null;
        var evictEvent = new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), oldCustomerTitle);
        try {
            Customer savedCustomer = customerDao.saveAndFlush(customer.getTenantId(), customer);
            if (customer.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(savedCustomer.getTenantId(), savedCustomer.getOwnerId(), savedCustomer.getId());

                entityGroupService.createDefaultCustomerEntityGroups(customer.getTenantId(), savedCustomer);
                countService.publishCountEntityEvictEvent(savedCustomer.getTenantId(), EntityType.CUSTOMER);
            } else {
                if (oldCustomerTitle != null && !savedCustomer.getTitle().equals(oldCustomerTitle)) {
                    List<EdgeId> edgeIds = edgeService.findAllRelatedEdgeIds(savedCustomer.getTenantId(), savedCustomer.getId());
                    if (edgeIds != null) {
                        for (EdgeId edgeId : edgeIds) {
                            Edge edge = edgeService.findEdgeById(savedCustomer.getTenantId(), edgeId);
                            edgeService.renameEdgeAllGroups(savedCustomer.getTenantId(), edge, edge.getName(), oldCustomerTitle, savedCustomer.getTitle());
                        }
                    }
                    Optional<Customer> publicCustomerOpt = customerDao
                            .findPublicCustomerByTenantIdAndOwnerId(savedCustomer.getTenantId().getId(), savedCustomer.getId().getId());
                    publicCustomerOpt.ifPresent(c -> {
                        String title = "[" + savedCustomer.getTitle() + "] " + PUBLIC_CUSTOMER_SUFFIX;
                        var someCustomerOpt = findCustomerByTenantIdAndTitle(savedCustomer.getTenantId(), title);
                        if (someCustomerOpt.isPresent()) {
                            title = title + "_" + UUID.randomUUID();
                        }
                        c.setTitle(title);
                        saveCustomer(c, true);
                    });
                }
            }
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(savedCustomer.getTenantId())
                    .entityId(savedCustomer.getId())
                    .entity(savedCustomer)
                    .oldEntity(oldCustomer)
                    .created(customer.getId() == null)
                    .build());
            return savedCustomer;
        } catch (Exception e) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(e,
                    "customer_title_unq_key", CUSTOMER_UNIQUE_TITLE_EX_MSG,
                    "customer_external_id_unq_key", "Customer with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteCustomer(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        deleteCustomer(tenantId, customerId, true, false);
    }

    private void deleteCustomer(TenantId tenantId, CustomerId customerId, boolean deleteSubcustomers, boolean force) {
        if (!force && calculatedFieldService.referencedInAnyCalculatedField(tenantId, customerId)) {
            throw new DataValidationException("Can't delete customer that is referenced in calculated fields!");
        }
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent customer.");
            }
        }
        if (deleteSubcustomers) {
            try {
                List<CustomerId> customerIds = fetchSubcustomers(tenantId, customerId);
                for (CustomerId subCustomerId : customerIds) {
                    deleteCustomer(tenantId, subCustomerId, true, force);
                }
            } catch (Exception e) {
                log.error("Failed to delete subcustomers", e);
                throw new RuntimeException(e);
            }
        }
        whiteLabelingService.deleteWhiteLabeling(tenantId, customerId, WhiteLabelingType.LOGIN);
        whiteLabelingService.deleteWhiteLabeling(tenantId, customerId, WhiteLabelingType.GENERAL);
        domainService.deleteDomainsByTenantIdAndCustomerId(tenantId, customerId);
        dashboardService.deleteDashboardsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        entityViewService.deleteEntityViewsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        assetService.deleteAssetsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        deviceService.deleteDevicesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        edgeService.deleteEdgesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        agentService.deleteAgentsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        schedulerEventService.deleteSchedulerEventsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        blobEntityService.deleteBlobEntitiesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        reportService.deleteReportsByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        reportTemplateService.deleteReportTemplatesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        entityGroupService.deleteAllEntityGroups(tenantId, customerId);
        roleService.deleteRolesByTenantIdAndCustomerId(customer.getTenantId(), customerId);
        apiUsageStateService.deleteApiUsageStateByEntityId(customerId);
        customerDao.removeById(tenantId, customerId.getId());
        publishEvictEvent(new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.CUSTOMER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(customerId).build());
    }

    @Transactional
    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteCustomer(tenantId, (CustomerId) id, false, true);
    }

    private List<CustomerId> fetchSubcustomers(TenantId tenantId, CustomerId customerId) throws Exception {
        List<CustomerId> customerIds = new ArrayList<>();
        Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, customerId, EntityType.CUSTOMER, EntityGroup.GROUP_ALL_NAME);
        if (entityGroup.isPresent()) {
            List<EntityId> childCustomerIds = entityGroupService.findAllEntityIdsAsync(tenantId, entityGroup.get().getId(), new PageLink(Integer.MAX_VALUE)).get();
            childCustomerIds.forEach(entityId -> customerIds.add(new CustomerId(entityId.getId())));
        }
        return customerIds;
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        var publicCustomer = findPublicCustomer(tenantId, ownerId);
        if (publicCustomer != null) {
            return publicCustomer;
        }
        return createPublicCustomer(tenantId, ownerId);
    }

    @Override
    public Customer findPublicCustomer(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findPublicCustomer, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        var publicCustomerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantId.getId(), ownerId.getId());
        return publicCustomerOpt.orElse(null);
    }

    private Customer createPublicCustomer(TenantId tenantId, EntityId ownerId) {
        String title;
        boolean createSubCustomer = EntityType.CUSTOMER.equals(ownerId.getEntityType());
        if (createSubCustomer) {
            Optional<String> ownerNameOpt = entityService.fetchEntityName(tenantId, ownerId);
            String ownerTitle = ownerNameOpt.orElseThrow(
                    () -> new RuntimeException("Failed to fetch owner name for ownerId: " + ownerId.getId().toString()));
            title = "[" + ownerTitle + "] " + PUBLIC_CUSTOMER_SUFFIX;
            var someCustomerOpt = findCustomerByTenantIdAndTitle(tenantId, title);
            if (someCustomerOpt.isPresent()) {
                Customer someCustomer = someCustomerOpt.get();
                if (someCustomer.isPublic() && ownerId.equals(someCustomer.getOwnerId())) {
                    return someCustomer;
                }
                title = title + "_" + UUID.randomUUID();
            }
        } else {
            title = PUBLIC_CUSTOMER_SUFFIX;
        }
        var publicCustomer = new Customer();
        publicCustomer.setTenantId(tenantId);
        publicCustomer.setTitle(title);
        if (createSubCustomer) {
            publicCustomer.setParentCustomerId(new CustomerId(ownerId.getId()));
        }
        try {
            publicCustomer.setAdditionalInfo(PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON);
        } catch (IllegalArgumentException e) {
            throw new IncorrectParameterException("Unable to create public customer", e);
        }
        try {
            return saveCustomer(publicCustomer, false);
        } catch (DataValidationException e) {
            if (CUSTOMER_UNIQUE_TITLE_EX_MSG.equals(e.getMessage())) {
                var publicCustomerOpt = customerDao.findPublicCustomerByTenantIdAndOwnerId(tenantId.getId(), ownerId.getId());
                if (publicCustomerOpt.isPresent()) {
                    return publicCustomerOpt.get();
                }
            }
            throw new RuntimeException("Failed to create public customer.", e);
        }
    }

    @Override
    public EntityGroup findOrCreatePublicUserGroup(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicUserGroup, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        Customer publicCustomer = findOrCreatePublicCustomer(tenantId, ownerId);
        return entityGroupService.findOrCreatePublicUsersGroup(publicCustomer.getTenantId(), publicCustomer.getId());
    }

    @Override
    public Role findOrCreatePublicUserEntityGroupRole(TenantId tenantId, EntityId ownerId) {
        log.trace("Executing findOrCreatePublicUserRole, tenantId [{}], ownerId [{}]", tenantId, ownerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateEntityId(ownerId, id -> INCORRECT_OWNER_ID + id);
        Customer publicCustomer = findOrCreatePublicCustomer(tenantId, ownerId);
        return roleService.findOrCreatePublicUsersEntityGroupRole(publicCustomer.getTenantId(), publicCustomer.getId());
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validatePageLink(pageLink);
        return customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteCustomersByTenantId(tenantId);
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findCustomersByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, id -> "Incorrect entityGroupId " + id);
        validatePageLink(pageLink);
        return customerDao.findCustomersByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<Customer> findCustomersByEntityGroupIds(List<EntityGroupId> groupIds, List<CustomerId> additionalCustomerIds, PageLink pageLink) {
        log.trace("Executing findCustomersByEntityGroupId, groupIds [{}], additionalCustomerIds [{}], pageLink [{}]", groupIds, additionalCustomerIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return customerDao.findCustomersByEntityGroupIds(toUUIDs(groupIds), toUUIDs(additionalCustomerIds), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findTenantCustomerInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantCustomerInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findTenantCustomersByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Customer> findCustomersByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink) {
        return customerDao.findByTenantIdAndParentCustomerId(tenantId, parentCustomerId, pageLink);
    }

    @Override
    public PageData<CustomerId> findCustomerIdsByTenantIdAndParentCustomerId(TenantId tenantId, CustomerId parentCustomerId, PageLink pageLink) {
        log.trace("Executing findCustomerIdsByTenantIdAndParentCustomerId, tenantId [{}], parentCustomerId [{}], pageLink [{}]", tenantId, parentCustomerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        UUID parentCustomerUuid = parentCustomerId == null || parentCustomerId.isNullUid() ? null : parentCustomerId.getId();
        return customerDao.findIdsByTenantIdAndCustomerId(tenantId.getId(), parentCustomerUuid, pageLink);
    }

    @Override
    public PageData<CustomerInfo> findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return customerInfoDao.findCustomersByTenantIdAndCustomerIdIncludingSubCustomers(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public List<Customer> findCustomersByCustomMenuId(CustomMenuId customMenuId) {
        log.trace("Executing findCustomersByCustomMenuId, customMenuId [{}]", customMenuId);
        return customerDao.findCustomersByCustomMenuId(customMenuId);
    }

    @Override
    public void updateCustomersCustomMenuId(List<CustomerId> customerIds, CustomMenuId customMenuId) {
        log.trace("Executing updateCustomersCustomMenuId, customMenuId [{}]", customMenuId);
        customerDao.updateCustomersCustomMenuId(customerIds, customMenuId);
    }

    private final PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Customer> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()), false, true);
                }
            };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findCustomerById(tenantId, new CustomerId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findCustomerByIdAsync(tenantId, new CustomerId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return customerDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
