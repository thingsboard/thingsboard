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
package org.thingsboard.server.dao.tenant;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;
import static org.thingsboard.server.dao.service.Validator.validateId;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.TenantEntity;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.rule.RuleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

@Service
@Slf4j
public class TenantServiceImpl extends AbstractEntityService implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private PluginService pluginService;

    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        TenantEntity tenantEntity = tenantDao.findById(tenantId.getId());
        return getData(tenantEntity);
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId tenantId) {
        log.trace("Executing TenantIdAsync [{}]", tenantId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        ListenableFuture<TenantEntity> tenantEntity = tenantDao.findByIdAsync(tenantId.getId());
        return Futures.transform(tenantEntity, (Function<? super TenantEntity, ? extends Tenant>) input -> getData(input));
    }

    @Override
    public Tenant saveTenant(Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        tenantValidator.validate(tenant);
        TenantEntity tenantEntity = tenantDao.save(tenant);
        return getData(tenantEntity);
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        userService.deleteTenantAdmins(tenantId);
        ruleService.deleteRulesByTenantId(tenantId);
        pluginService.deletePluginsByTenantId(tenantId);
        tenantDao.removeById(tenantId.getId());
        deleteEntityRelations(tenantId);
    }

    @Override
    public TextPageData<Tenant> findTenants(TextPageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<TenantEntity> tenantEntities = tenantDao.findTenantsByRegion(DEFAULT_TENANT_REGION, pageLink);
        List<Tenant> tenants = convertDataList(tenantEntities);
        return new TextPageData<Tenant>(tenants, pageLink);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntitites(DEFAULT_TENANT_REGION);
    }

    private DataValidator<Tenant> tenantValidator =
            new DataValidator<Tenant>() {
                @Override
                protected void validateDataImpl(Tenant tenant) {
                    if (StringUtils.isEmpty(tenant.getTitle())) {
                        throw new DataValidationException("Tenant title should be specified!");
                    }
                    if (!StringUtils.isEmpty(tenant.getEmail())) {
                        validateEmail(tenant.getEmail());
                    }
                }
    };

    private PaginatedRemover<String, TenantEntity> tenantsRemover =
            new PaginatedRemover<String, TenantEntity>() {

        @Override
        protected List<TenantEntity> findEntities(String region, TextPageLink pageLink) {
            return tenantDao.findTenantsByRegion(region, pageLink);
        }

        @Override
        protected void removeEntity(TenantEntity entity) {
            deleteTenant(new TenantId(entity.getId()));
        }
    };
}
