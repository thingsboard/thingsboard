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
package org.thingsboard.server.dao.dashboard;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
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
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.TimePaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        if (dashboard.addAssignedCustomer(customerId, customer.getTitle())) {
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
        if (dashboard.removeAssignedCustomer(customerId)) {
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

    private Dashboard updateAssignedCustomerTitle(DashboardId dashboardId, CustomerId customerId, String customerTitle) {
        Dashboard dashboard = findDashboardById(dashboardId);
        if (dashboard.updateAssignedCustomer(customerId, customerTitle)) {
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    private void deleteRelation(EntityRelation dashboardRelation) throws ExecutionException, InterruptedException {
        log.debug("Deleting Dashboard relation: {}", dashboardRelation);
        relationService.deleteRelationAsync(dashboardRelation).get();
    }

    private void createRelation(EntityRelation dashboardRelation) throws ExecutionException, InterruptedException {
        log.debug("Creating Dashboard relation: {}", dashboardRelation);
        relationService.saveRelationAsync(dashboardRelation).get();
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
    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, tenantId [{}], customerId [{}]", tenantId, customerId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        new CustomerDashboardsUnassigner(tenantId, customerId).removeEntities(customerId);
    }

    @Override
    public void updateCustomerDashboards(TenantId tenantId, CustomerId customerId, String customerTitle) {
        log.trace("Executing updateCustomerDashboards, tenantId [{}], customerId [{}], customerTitle [{}]", tenantId, customerId, customerTitle);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validateString(customerTitle, "Incorrect customerTitle " + customerTitle);
        new CustomerDashboardsUpdater(tenantId, customerId, customerTitle).removeEntities(customerId);
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
    
    private class CustomerDashboardsUnassigner extends TimePaginatedRemover<CustomerId, DashboardInfo> {
        
        private TenantId tenantId;
        private CustomerId customerId;
        
        CustomerDashboardsUnassigner(TenantId tenantId, CustomerId customerId) {
            this.tenantId = tenantId;
            this.customerId = customerId;
        }

        @Override
        protected List<DashboardInfo> findEntities(CustomerId id, TimePageLink pageLink) {
            try {
                return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get dashboards by tenantId [{}] and customerId [{}].", tenantId, id);
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(DashboardInfo entity) {
            unassignDashboardFromCustomer(new DashboardId(entity.getUuidId()), this.customerId);
        }
        
    }

    private class CustomerDashboardsUpdater extends TimePaginatedRemover<CustomerId, DashboardInfo> {

        private TenantId tenantId;
        private CustomerId customerId;
        private String customerTitle;

        CustomerDashboardsUpdater(TenantId tenantId, CustomerId customerId, String customerTitle) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.customerTitle = customerTitle;
        }

        @Override
        protected List<DashboardInfo> findEntities(CustomerId id, TimePageLink pageLink) {
            try {
                return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink).get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to get dashboards by tenantId [{}] and customerId [{}].", tenantId, id);
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void removeEntity(DashboardInfo entity) {
            updateAssignedCustomerTitle(new DashboardId(entity.getUuidId()), this.customerId, this.customerTitle);
        }

    }

}
