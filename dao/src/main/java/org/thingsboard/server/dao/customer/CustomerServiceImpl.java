/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.CustomerEntity;
import org.thingsboard.server.dao.model.TenantEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;
@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private static final String PUBLIC_CUSTOMER_TITLE = "Public";

    @Autowired
    private CustomerDao customerDao;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TenantDao tenantDao;
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Override
    public Customer findCustomerById(CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        CustomerEntity customerEntity = customerDao.findById(customerId.getId());
        return getData(customerEntity);
    }

    @Override
    public Customer saveCustomer(Customer customer) {
        log.trace("Executing saveCustomer [{}]", customer);
        customerValidator.validate(customer);
        CustomerEntity customerEntity = customerDao.save(customer);
        return getData(customerEntity);
    }

    @Override
    public void deleteCustomer(CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        dashboardService.unassignCustomerDashboards(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);               
        customerDao.removeById(customerId.getId());
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect customerId " + tenantId);
        Optional<CustomerEntity> publicCustomerEntity = customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), PUBLIC_CUSTOMER_TITLE);
        if (publicCustomerEntity.isPresent()) {
            return getData(publicCustomerEntity.get());
        } else {
            Customer publicCustomer = new Customer();
            publicCustomer.setTenantId(tenantId);
            publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
            try {
                publicCustomer.setAdditionalInfo(new ObjectMapper().readValue("{ \"isPublic\": true }", JsonNode.class));
            } catch (IOException e) {
                throw new IncorrectParameterException("Unable to create public customer.", e);
            }
            CustomerEntity customerEntity = customerDao.save(publicCustomer);
            return getData(customerEntity);
        }
    }

    @Override
    public TextPageData<Customer> findCustomersByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<CustomerEntity> customerEntities = customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
        List<Customer> customers = convertDataList(customerEntities);
        return new TextPageData<Customer>(customers, pageLink);
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customersByTenantRemover.removeEntitites(tenantId);
    }
    
    private DataValidator<Customer> customerValidator =
            new DataValidator<Customer>() {

                @Override
                protected void validateCreate(Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                throw new DataValidationException("Customer with such title already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                if (!c.getId().equals(customer.getUuidId())) {
                                    throw new DataValidationException("Customer with such title already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Customer customer) {
                    if (StringUtils.isEmpty(customer.getTitle())) {
                        throw new DataValidationException("Customer title should be specified!");
                    }
                    if (customer.getTitle().equals(PUBLIC_CUSTOMER_TITLE)) {
                        throw new DataValidationException("'Public' title for customer is system reserved!");
                    }
                    if (!StringUtils.isEmpty(customer.getEmail())) {
                        validateEmail(customer.getEmail());
                    }
                    if (customer.getTenantId() == null) {
                        throw new DataValidationException("Customer should be assigned to tenant!");
                    } else {
                        TenantEntity tenant = tenantDao.findById(customer.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Customer is referencing to non-existent tenant!");
                        }
                    }
                }
    };

    private PaginatedRemover<TenantId, CustomerEntity> customersByTenantRemover =
            new PaginatedRemover<TenantId, CustomerEntity>() {
        
        @Override
        protected List<CustomerEntity> findEntities(TenantId id, TextPageLink pageLink) {
            return customerDao.findCustomersByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(CustomerEntity entity) {
            deleteCustomer(new CustomerId(entity.getId()));
        }
    };
}
