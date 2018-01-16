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
package org.thingsboard.server.dao.application;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.rule.RuleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_DEVICE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class ApplicationServiceImpl extends AbstractEntityService implements ApplicationService{

    @Autowired
    private ApplicationDao applicationDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private RuleDao ruleDao;

    @Override
    public Application saveApplication(Application application) {
        log.trace("Executing saveApplication [{}]", application);
        applicationValidator.validate(application);
        return applicationDao.save(application);
    }

    @Override
    public Application findApplicationById(ApplicationId applicationId) {
        log.trace("Executing findApplicationById [{}]", applicationId);
        validateId(applicationId, "Incorrect applicationId " + applicationId);
        return applicationDao.findById(applicationId.getId());
    }

    @Override
    public void deleteApplication(ApplicationId applicationId) {
        log.trace("Executing deleteApplication [{}]", applicationId);
        validateId(applicationId, "Incorrect applicationId " + applicationId);

        applicationDao.removeById(applicationId.getId());
    }


    @Override
    public TextPageData<Application> findApplicationsByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findApplicationsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Application> applications = applicationDao.findApplicationsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(applications, pageLink);
    }

    @Override
    public List<Application> findApplicationsByDeviceType(TenantId tenantId, String deviceType){
        log.trace("Executing findApplicationsByDeviceType,  tenantId [{}], device Type [{}]", tenantId, deviceType);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        return applicationDao.findApplicationByDeviceType(tenantId.getId(), deviceType);
    }

    @Override
    public List<String> findApplicationByRuleId(TenantId tenantId, RuleId ruleId) {
        log.trace("Executing findApplicationByRuleId,  tenantId [{}], ruleId [{}]", tenantId, ruleId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(ruleId, "Incorrect ruleId " + ruleId);
        return applicationDao.findApplicationByRuleId(tenantId.getId(), ruleId.getId());
    }

    @Override
    public List<String> findApplicationByDashboardId(TenantId tenantId, DashboardId dashboardId){
        log.trace("Executing findApplicationByDashboardId,  tenantId [{}], dashboardId [{}]", tenantId, dashboardId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
        return applicationDao.findApplicationsByDashboardId(tenantId.getId(), dashboardId.getId());
    }

    @Override
    public Application assignApplicationToCustomer(ApplicationId applicationId, CustomerId customerId) {
        Application application = findApplicationById(applicationId);
        application.setCustomerId(customerId);
        return saveApplication(application);
    }

    @Override
    public Application unassignApplicationFromCustomer(ApplicationId applicationId) {
        Application application = findApplicationById(applicationId);
        application.setCustomerId(null);
        return saveApplication(application);
    }

    @Override
    public Application assignDashboardToApplication(ApplicationId applicationId, DashboardId dashboardId, String dashboardType) {
        Application application = findApplicationById(applicationId);
        if(dashboardType.equals("mini")) {
            application.setMiniDashboardId(dashboardId);
        } else if(dashboardType.equals("main")) {
            application.setDashboardId(dashboardId);
        } else {
            throw new IncorrectParameterException("Incorrect dashboard type: "+dashboardType);
        }

        return saveApplication(application);
    }

    @Override
    public Application unassignDashboardFromApplication(ApplicationId applicationId, String dashboardType) {
        Application application = findApplicationById(applicationId);
        if(dashboardType.equals("mini")) {
            application.setMiniDashboardId(null);
        } else if(dashboardType.equals("main")) {
            application.setDashboardId(null);
        } else {
            throw new IncorrectParameterException("Incorrect dashboard type: "+dashboardType);
        }

        return saveApplication(application);
    }

    @Override
    public Application assignRulesToApplication(ApplicationId applicationId, List<RuleId> ruleIdList) {
        Application application = findApplicationById(applicationId);
        application.setRules(ruleIdList);
        return saveApplication(application);
    }

    @Override
    public Application assignDeviceTypesToApplication(ApplicationId applicationId, List<String> deviceTypes) {
        Application application = findApplicationById(applicationId);
        if(deviceTypes != null && !deviceTypes.isEmpty()){
            application.setDeviceTypes(deviceTypes);
            return saveApplication(application);
        }
        return application;
    }


    private DataValidator<Application> applicationValidator =
            new DataValidator<Application>() {

                @Override
                protected void validateCreate(Application application) {
                    applicationDao.findApplicationByTenantIdAndName(application.getTenantId().getId(), application.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Application with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Application application) {
                    applicationDao.findApplicationByTenantIdAndName(application.getTenantId().getId(), application.getName()).ifPresent(
                            d -> {
                                if (!d.getUuidId().equals(application.getUuidId())) {
                                    throw new DataValidationException("Application with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Application application) {
                    if (StringUtils.isEmpty(application.getName())) {
                        throw new DataValidationException("Application name should be specified!");
                    }
                    if (application.getTenantId() == null) {
                        throw new DataValidationException("Application should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(application.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Application is referencing to non-existent tenant!");
                        }
                    }
                    if (application.getCustomerId() == null) {
                        application.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!application.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(application.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign application to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(application.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign application to customer from different tenant!");
                        }
                    }

                    if(application.getDashboardId() == null) {
                        application.setDashboardId(new DashboardId(NULL_UUID));
                    } else if(!application.getDashboardId().getId().equals(NULL_UUID)) {
                        Dashboard dashboard = dashboardDao.findById(application.getDashboardId().getId());
                        if(dashboard == null) {
                            throw new DataValidationException("Can't assign application to non-existent dashboard!");
                        }
                    }

                    if(application.getRules() == null || application.getRules().isEmpty()) {
                        application.setRules(Arrays.asList(new RuleId(NULL_UUID)));
                    } else if(!application.getRules().get(0).getId().equals(NULL_UUID)) {
                        for(RuleId ruleId: application.getRules()) {
                            RuleMetaData ruleMetaData = ruleDao.findById(ruleId);
                            if(ruleMetaData == null) {
                              throw new DataValidationException("Can't assign application to non-existent rule!");
                            }
                        }
                    }

                    if(application.getDeviceTypes() == null || application.getDeviceTypes().isEmpty()) {
                        application.setDeviceTypes(Arrays.asList(NULL_DEVICE_TYPE));
                    }

                    if(application.getMiniDashboardId() == null) {
                        application.setMiniDashboardId(new DashboardId(NULL_UUID));
                    } else if(!application.getMiniDashboardId().getId().equals(NULL_UUID)) {
                        Dashboard miniDashboard = dashboardDao.findById(application.getMiniDashboardId().getId());
                        if(miniDashboard == null) {
                            throw new DataValidationException("Can't assign application to non-existent mini dashboard!");
                        }
                    }
                }
            };
}
