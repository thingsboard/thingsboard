/**
 * Copyright © 2016-2020 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
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
    private static final String REGISTRATION_ID = "registrationId";

    @Autowired
    private OAuth2Service oauth2Service;

    // TODO ask why POST
    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request) throws ThingsboardException {
        try {
            return oauth2Service.getOAuth2Clients(request.getServerName());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/currentOAuth2Configuration", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public OAuth2ClientsParams getCurrentOAuth2ClientsParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkOAuth2ConfigPermissions(Operation.READ);
            OAuth2ClientsParams oAuth2ClientsParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                oAuth2ClientsParams = oauth2Service.getSystemOAuth2ClientsParams(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                oAuth2ClientsParams = oauth2Service.getTenantOAuth2ClientsParams(getCurrentUser().getTenantId());
            }
            return oAuth2ClientsParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/oAuth2Configuration", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2ClientsParams saveLoginWhiteLabelParams(@RequestBody OAuth2ClientsParams oAuth2ClientsParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkOAuth2ConfigPermissions(Operation.WRITE);
            OAuth2ClientsParams savedOAuth2ClientsParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                savedOAuth2ClientsParams = oauth2Service.saveSystemOAuth2ClientsParams(oAuth2ClientsParams);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                savedOAuth2ClientsParams = oauth2Service.saveTenantOAuth2ClientsParams(getCurrentUser().getTenantId(), oAuth2ClientsParams);
            }
            return savedOAuth2ClientsParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/oauth2/isOAuth2ConfigurationAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isOAuth2ConfigurationAllowed() throws ThingsboardException {
        try {
            return oauth2Service.isOAuth2ClientRegistrationAllowed(getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    private void checkOAuth2ConfigPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION, operation);
    }
}
