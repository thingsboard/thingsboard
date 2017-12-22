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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Application;
import org.thingsboard.server.common.data.ApplicationRulesWrapper;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApplicationController extends BaseController {

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/application", method = RequestMethod.POST)
    @ResponseBody
    public Application saveApplication(@RequestBody Application application) throws ThingsboardException {
        try{
            application.setTenantId(getCurrentUser().getTenantId());
            Application savedApplication = checkNotNull(applicationService.saveApplication(application));
            return savedApplication;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/application/{applicationId}", method = RequestMethod.GET)
    @ResponseBody
    public Application getApplicationById(@PathVariable("applicationId") String strApplicationId) throws ThingsboardException {
        checkParameter("applicationId", strApplicationId);
        try {
            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            return checkApplicationId(applicationId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/application/{applicationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteApplication(@PathVariable("applicationId") String strApplicationId) throws ThingsboardException {
        checkParameter("applicationId", strApplicationId);
        try {
            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            checkApplicationId(applicationId);
            applicationService.deleteApplication(applicationId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/applications", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Application> getTenantApplications(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(applicationService.findApplicationsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/applications/{deviceType}", method = RequestMethod.GET)
    @ResponseBody
    public List<Application> getDeviceTypeApplications(@PathVariable("deviceType") String deviceType)  throws ThingsboardException {
        checkParameter("deviceType", deviceType);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(applicationService.findApplicationsByDeviceType(tenantId, deviceType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/application/{applicationId}", method = RequestMethod.POST)
    @ResponseBody
    public Application assignApplicationToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable("applicationId") String strApplicationId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter("applicationId", strApplicationId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);

            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            checkApplicationId(applicationId);

            return checkNotNull(applicationService.assignApplicationToCustomer(applicationId, customerId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/application/{applicationId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Application unassignApplicationFromCustomer(@PathVariable("applicationId") String strApplicationId) throws ThingsboardException {
        checkParameter("applicationId", strApplicationId);
        try {
            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            Application application = checkApplicationId(applicationId);
            if (application.getCustomerId() == null || application.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Application isn't assigned to any customer!");
            }
            return checkNotNull(applicationService.unassignApplicationFromCustomer(applicationId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardType}/{dashboardId}/application/{applicationId}", method = RequestMethod.POST)
    @ResponseBody
    public Application assignDashboardToApplication(
            @PathVariable("dashboardType") String dashboardType,
            @PathVariable("dashboardId") String strDashboardId,
            @PathVariable("applicationId") String strApplicationId) throws ThingsboardException {

        checkParameter("dashboardType", dashboardType);
        checkParameter("dashboardId", strDashboardId);
        checkParameter("applicationId", strApplicationId);
        try {
            DashboardId dashboardId =  new DashboardId(toUUID(strDashboardId));
            checkDashboardId(dashboardId);

            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            checkApplicationId(applicationId);

            return checkNotNull(applicationService.assignDashboardToApplication(applicationId, dashboardId, dashboardType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardType}/application/{applicationId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Application unassignDashboardFromApplication(@PathVariable("dashboardType") String dashboardType, @PathVariable("applicationId") String strApplicationId) throws ThingsboardException {
        checkParameter("applicationId", strApplicationId);
        checkParameter("dashboardType", dashboardType);
        try {
            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            Application application = checkApplicationId(applicationId);
            if(dashboardType.equals("mini")) {
                if (application.getMiniDashboardId() == null || application.getMiniDashboardId().getId().equals(ModelConstants.NULL_UUID)) {
                    throw new IncorrectParameterException("No mini dashboard assigned to an application!");
                }
                return checkNotNull(applicationService.unassignDashboardFromApplication(applicationId, dashboardType));
            } else if(dashboardType.equals("main")) {
                if (application.getDashboardId() == null || application.getDashboardId().getId().equals(ModelConstants.NULL_UUID)) {
                    throw new IncorrectParameterException("No dashboard assigned to an application!");
                }
                return checkNotNull(applicationService.unassignDashboardFromApplication(applicationId, dashboardType));
            } else {
                throw new IncorrectParameterException("Incorrect Dashboard Type for an application");
            }

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/app/assignRules", method = RequestMethod.POST,consumes = "application/json")
    public Application assignRulesToApplication(@RequestBody ApplicationRulesWrapper applicationRulesWrapper) throws ThingsboardException {
        checkParameter("applicationId", applicationRulesWrapper.getApplicationId());
        try {
            ApplicationId applicationId = new ApplicationId(toUUID(applicationRulesWrapper.getApplicationId()));
            checkApplicationId(applicationId);
            List<RuleId> ruleIds = Collections.emptyList();
            if (applicationRulesWrapper != null) {
                ruleIds = applicationRulesWrapper.getRules().stream().map(r -> new RuleId(toUUID(r))).collect(Collectors.toList());
            }
            return checkNotNull(applicationService.assignRulesToApplication(applicationId, ruleIds));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/app/{applicationId}/deviceTypes", method = RequestMethod.POST,consumes = "application/json")
    public Application assignDeviceTypesToApplication(@PathVariable("applicationId") String strApplicationId,
                                                      @RequestBody List<String> deviceTypes) throws ThingsboardException {
        checkParameter("applicationId", strApplicationId);
        try {
            log.error("Got application [{}] with deviceTypes [{}]", strApplicationId, deviceTypes);
            ApplicationId applicationId = new ApplicationId(toUUID(strApplicationId));
            checkApplicationId(applicationId);
            return checkNotNull(applicationService.assignDeviceTypesToApplication(applicationId, deviceTypes));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
