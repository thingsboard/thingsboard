/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.TimePaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private TenantDao tenantDao;
    
    @Autowired
    private CustomerDao customerDao;

    @Override
    public Dashboard findDashboardById(DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findById(dashboardId.getId());
    }

    @Override
    public ListenableFuture<Dashboard> findDashboardByIdAsync(DashboardId dashboardId) {
        log.trace("Executing findDashboardByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findByIdAsync(dashboardId.getId());
    }

    @Override
    public DashboardInfo findDashboardInfoById(DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findById(dashboardId.getId());
    }

    @Override
    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findByIdAsync(dashboardId.getId());
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        dashboardValidator.validate(dashboard);
        return dashboardDao.save(dashboard);
    }
    
    @Override
    public Dashboard assignDashboardToCustomer(DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        Customer customer = customerDao.findById(customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
        }
        if (!customer.getTenantId().getId().equals(dashboard.getTenantId().getId())) {
            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
        }
        if (dashboard.addAssignedCustomer(customer)) {
            try {
                createRelation(new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to create dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        Customer customer = customerDao.findById(customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent customer!");
        }
        if (dashboard.removeAssignedCustomer(customer)) {
            try {
                deleteRelation(new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (ExecutionException | InterruptedException e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    private Dashboard updateAssignedCustomer(DashboardId dashboardId, Customer customer) {
        Dashboard dashboard = findDashboardById(dashboardId);
        if (dashboard.updateAssignedCustomer(customer)) {
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    @Override
    public void deleteDashboard(DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        deleteEntityRelations(dashboardId);
        dashboardDao.removeById(dashboardId.getId());
    }

    @Override
    public TextPageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DashboardInfo> dashboards = dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(dashboards, pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDashboardsRemover.removeEntities(tenantId);
    }

    @Override
    public ListenableFuture<TimePageData<DashboardInfo>> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        ListenableFuture<List<DashboardInfo>> dashboards = dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);

        return Futures.transform(dashboards, new Function<List<DashboardInfo>, TimePageData<DashboardInfo>>() {
            @Nullable
            @Override
            public TimePageData<DashboardInfo> apply(@Nullable List<DashboardInfo> dashboards) {
                return new TimePageData<>(dashboards, pageLink);
            }
        });
    }

    @Override
    public void unassignCustomerDashboards(CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboards from non-existent customer!");
        }
        new CustomerDashboardsUnassigner(customer).removeEntities(customer);
    }

    @Override
    public void updateCustomerDashboards(CustomerId customerId) {
        log.trace("Executing updateCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't update dashboards for non-existent customer!");
        }
        new CustomerDashboardsUpdater(customer).removeEntities(customer);
    }

    private DataValidator<Dashboard> dashboardValidator =
            new DataValidator<Dashboard>() {
                @Override
                protected void validateDataImpl(Dashboard dashboard) {
                    if (StringUtils.isEmpty(dashboard.getTitle())) {
                        throw new DataValidationException("Dashboard title should be specified!");
                    }
                    if (dashboard.getTenantId() == null) {
                        throw new DataValidationException("Dashboard should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(dashboard.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
                        }
                    }
                }
    };
    
    private PaginatedRemover<TenantId, DashboardInfo> tenantDashboardsRemover =
            new PaginatedRemover<TenantId, DashboardInfo>() {
        
        @Override
        protected List<DashboardInfo> findEntities(TenantId id, TextPageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(DashboardInfo entity) {
            deleteDashboard(new DashboardId(entity.getUuidId()));
        }
    };
    
    private class CustomerDashboardsUnassigner extends TimePaginatedRemover<Customer, DashboardInfo> {
        
        private Customer customer;

        CustomerDashboardsUnassigner(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected List<DashboardInfo> findEntities(Customer customer, TimePageLink pageLink) {
            try {
                return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get dashboards by tenantId [{}] and customerId [{}].", customer.getTenantId().getId(), customer.getId().getId());
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(DashboardInfo entity) {
            unassignDashboardFromCustomer(new DashboardId(entity.getUuidId()), this.customer.getId());
        }
        
    }

    private class CustomerDashboardsUpdater extends TimePaginatedRemover<Customer, DashboardInfo> {

        private Customer customer;

        CustomerDashboardsUpdater(Customer customer) {
            this.customer = customer;
        }

        @Override
        protected List<DashboardInfo> findEntities(Customer customer, TimePageLink pageLink) {
            try {
                return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get dashboards by tenantId [{}] and customerId [{}].", customer.getTenantId().getId(), customer.getId().getId());
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(DashboardInfo entity) {
            updateAssignedCustomer(new DashboardId(entity.getUuidId()), this.customer);
        }

    }

}
