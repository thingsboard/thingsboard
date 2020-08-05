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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientsParams;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class OAuth2Controller extends BaseController {
    public static final String CLIENT_REGISTRATION_ID = "clientRegistrationId";
    private static final String REGISTRATION_ID = "registrationId";

    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request) throws ThingsboardException {
        try {
            return oAuth2Service.getOAuth2Clients(request.getServerName());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public OAuth2ClientsParams getCurrentClientsParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkOAuth2ConfigPermissions(Operation.READ);
            List<OAuth2ClientRegistration> clientRegistrations = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                clientRegistrations = oAuth2Service.findClientRegistrationsByTenantId(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                clientRegistrations = oAuth2Service.findClientRegistrationsByTenantId(getCurrentUser().getTenantId());
            } else {
                throw new IllegalStateException("Authority " + authority + " cannot get client registrations.");
            }
            return new OAuth2ClientsParams(clientRegistrations);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2ClientRegistration saveClientRegistration(@RequestBody OAuth2ClientRegistration clientRegistration) throws ThingsboardException {
        try {
            clientRegistration.setTenantId(getCurrentUser().getTenantId());
            checkEntity(clientRegistration.getId(), clientRegistration, Resource.OAUTH2_CONFIGURATION);
            return oAuth2Service.saveClientRegistration(clientRegistration);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/config/{clientRegistrationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClientRegistration(@PathVariable(CLIENT_REGISTRATION_ID) String strClientRegistrationId) throws ThingsboardException {
        checkParameter(CLIENT_REGISTRATION_ID, strClientRegistrationId);
        try {
            OAuth2ClientRegistrationId clientRegistrationId = new OAuth2ClientRegistrationId(toUUID(strClientRegistrationId));
            OAuth2ClientRegistration clientRegistration = checkOAuth2ClientRegistrationId(clientRegistrationId, Operation.DELETE);
            oAuth2Service.deleteClientRegistrationById(getCurrentUser().getTenantId(), clientRegistrationId);

            logEntityAction(clientRegistrationId, clientRegistration,
                    null,
                    ActionType.DELETED, null, strClientRegistrationId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.OAUTH2_CLIENT_REGISTRATION),
                    null,
                    null,
                    ActionType.DELETED, e, strClientRegistrationId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/config/isAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isOAuth2ConfigurationAllowed() throws ThingsboardException {
        try {
            return oAuth2Service.isOAuth2ClientRegistrationAllowed(getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkOAuth2ConfigPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION, operation);
    }

    private void checkOAuth2ConfigPermissions(Operation operation, OAuth2ClientRegistration clientRegistration) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION, operation, clientRegistration.getId(), clientRegistration);
    }
}
