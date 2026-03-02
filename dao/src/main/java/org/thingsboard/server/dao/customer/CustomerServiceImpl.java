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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.NameConflictPolicy;
import org.thingsboard.server.common.data.NameConflictStrategy;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("CustomerDaoService")
@Slf4j
public class CustomerServiceImpl extends AbstractCachedEntityService<CustomerCacheKey, Customer, CustomerCacheEvictEvent> implements CustomerService {

    public static final String PUBLIC_CUSTOMER_TITLE = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final JsonNode PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON = JacksonUtil.toJsonNode("{\"isPublic\":true}");
    public static final String CUSTOMER_UNIQUE_TITLE_EX_MSG = "Customer with such title already exists!";

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserService userService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DashboardService dashboardService;

    @Lazy
    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private DataValidator<Customer> customerValidator;

    @Autowired
    private EntityCountService countService;

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
        var evictEvent = new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), oldCustomer != null ? oldCustomer.getTitle() : null);
        try {
            Customer savedCustomer = customerDao.saveAndFlush(customer.getTenantId(), customer);
            if (!savedCustomer.isPublic()) {
                dashboardService.updateCustomerDashboards(savedCustomer.getTenantId(), savedCustomer.getId());
            }
            if (customer.getId() == null) {
                countService.publishCountEntityEvictEvent(savedCustomer.getTenantId(), EntityType.CUSTOMER);
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
        deleteEntity(tenantId, customerId, false);
    }

    @Transactional
    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force && calculatedFieldService.referencedInAnyCalculatedField(tenantId, id)) {
            throw new DataValidationException("Can't delete customer that is referenced in calculated fields!");
        }

        CustomerId customerId = (CustomerId) id;
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent customer.");
            }
        }
        dashboardService.unassignCustomerDashboards(tenantId, customerId);
        entityViewService.unassignCustomerEntityViews(customer.getTenantId(), customerId);
        assetService.unassignCustomerAssets(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        edgeService.unassignCustomerEdges(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        apiUsageStateService.deleteApiUsageStateByEntityId(customerId);
        customerDao.removeById(tenantId, customerId.getId());
        publishEvictEvent(new CustomerCacheEvictEvent(customer.getTenantId(), customer.getTitle(), null));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.CUSTOMER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(customerId).build());
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        var publicCustomer = findPublicCustomer(tenantId);
        if (publicCustomer != null) {
            return publicCustomer;
        }
        publicCustomer = new Customer();
        publicCustomer.setTenantId(tenantId);
        publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
        try {
            publicCustomer.setAdditionalInfo(PUBLIC_CUSTOMER_ADDITIONAL_INFO_JSON);
        } catch (IllegalArgumentException e) {
            throw new IncorrectParameterException("Unable to create public customer.", e);
        }
        try {
            return saveCustomer(publicCustomer, false);
        } catch (DataValidationException e) {
            if (CUSTOMER_UNIQUE_TITLE_EX_MSG.equals(e.getMessage())) {
                Optional<Customer> publicCustomerOpt = customerDao.findPublicCustomerByTenantId(tenantId.getId());
                if (publicCustomerOpt.isPresent()) {
                    return publicCustomerOpt.get();
                }
            }
            throw new RuntimeException("Failed to create public customer.", e);
        }
    }

    @Override
    public Customer findPublicCustomer(TenantId tenantId) {
        log.trace("Executing findPublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Optional<Customer> publicCustomerOpt = customerDao.findPublicCustomerByTenantId(tenantId.getId());
        return publicCustomerOpt.orElse(null);
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
    public List<Customer> findCustomersByTenantIdAndIds(TenantId tenantId, List<CustomerId> customerIds) {
        log.trace("Executing findCustomersByTenantIdAndIds, tenantId [{}], customerIds [{}]", tenantId, customerIds);
        return customerDao.findCustomersByTenantIdAndIds(tenantId.getId(), customerIds.stream().map(CustomerId::getId).collect(Collectors.toList()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteCustomersByTenantId(tenantId);
    }

    private final PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Customer> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()));
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
