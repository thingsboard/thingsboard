/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionProfile;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

@RestController
@RequestMapping("/api")
public class ProvisionProfileController extends BaseController {

    private static final String PROFILE_ID = "profileId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/provision", method = RequestMethod.POST)
    @ResponseBody
    public ProvisionProfile saveProvisionProfile(@RequestBody(required = false) ProvisionProfile profile) throws ThingsboardException {
        try {
            profile.setTenantId(getTenantId());

            Operation operation = profile.getId() == null ? Operation.CREATE : Operation.WRITE;

            accessControlService.checkPermission(getCurrentUser(), Resource.PROVISION_PROFILE, operation,
                    profile.getId(), profile);

            ProvisionProfile savedProfile = checkNotNull(deviceProvisionService.saveProvisionProfile(profile));

            logEntityAction(savedProfile.getId(), savedProfile,
                    savedProfile.getCustomerId(),
                    profile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.PROVISION_PROFILE), profile,
                    null, profile.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/provision", params = {"key", "secret"}, method = RequestMethod.GET)
    @ResponseBody
    public ProvisionProfile getProvisionProfile(@RequestParam String key, @RequestParam String secret) throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId();
            ProvisionProfile profile = checkNotNull(deviceProvisionService.findProvisionProfileByKey(tenantId, key));
            accessControlService.checkPermission(getCurrentUser(), Resource.PROVISION_PROFILE, Operation.READ, profile.getId(), profile);
            if (profile.getTenantId().equals(tenantId) && profile.getCredentials().getProvisionProfileSecret().equals(secret)) {
                return profile;
            }
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/{profileId}/provision", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteProfile(@PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        try {
            ProvisionProfileId provisionProfileId = new ProvisionProfileId(toUUID(strProfileId));
            ProvisionProfile profile = checkProfileId(provisionProfileId, Operation.DELETE);
            deviceProvisionService.deleteProfile(getTenantId(), provisionProfileId);

            logEntityAction(provisionProfileId, profile,
                    profile.getCustomerId(),
                    ActionType.DELETED, null, strProfileId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.PROVISION_PROFILE),
                    null,
                    null,
                    ActionType.DELETED, e, strProfileId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/profile/{profileId}", method = RequestMethod.POST)
    @ResponseBody
    public ProvisionProfile assignProfileToCustomer(@PathVariable("customerId") String strCustomerId,
                                                    @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(PROFILE_ID, strProfileId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId, Operation.READ);

            ProvisionProfileId provisionProfileId = new ProvisionProfileId(toUUID(strProfileId));
            checkProfileId(provisionProfileId, Operation.ASSIGN_TO_CUSTOMER);

            ProvisionProfile savedProfile = checkNotNull(deviceProvisionService.assignProfileToCustomer(getCurrentUser().getTenantId(), provisionProfileId, customerId));

            logEntityAction(provisionProfileId, savedProfile,
                    savedProfile.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strProfileId, strCustomerId, customer.getName());

            return savedProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.PROVISION_PROFILE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strProfileId, strCustomerId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/profile/{profileId}", method = RequestMethod.DELETE)
    @ResponseBody
    public ProvisionProfile unassignProfileFromCustomer(@PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        try {
            ProvisionProfileId provisionProfileId = new ProvisionProfileId(toUUID(strProfileId));
            ProvisionProfile profile = checkProfileId(provisionProfileId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (profile.getCustomerId() == null || profile.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Profile isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(profile.getCustomerId(), Operation.READ);

            ProvisionProfile savedProfile = checkNotNull(deviceProvisionService.unassignProfileFromCustomer(getCurrentUser().getTenantId(), provisionProfileId));

            logEntityAction(provisionProfileId, savedProfile,
                    savedProfile.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strProfileId, customer.getId().toString(), customer.getName());

            return savedProfile;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.PROVISION_PROFILE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strProfileId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/profiles", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<ProvisionProfile> getTenantProfiles(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(deviceProvisionService.findProfilesByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
