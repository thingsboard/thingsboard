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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientsDomainParams;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class OAuth2Controller extends BaseController {
    private static final String CLIENT_REGISTRATION_ID = "clientRegistrationId";
    private static final String DOMAIN = "domain";

    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request) throws ThingsboardException {
        try {
            return oAuth2Service.getOAuth2Clients(request.getServerName());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<OAuth2ClientsDomainParams> getCurrentClientsParams() throws ThingsboardException {
        try {
            return oAuth2Service.findDomainsParams();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public List<OAuth2ClientsDomainParams> saveClientParams(@RequestBody List<OAuth2ClientsDomainParams> domainsParams) throws ThingsboardException {
        try {
            return oAuth2Service.saveDomainsParams(domainsParams);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config/{clientRegistrationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClientRegistration(@PathVariable(CLIENT_REGISTRATION_ID) String strClientRegistrationId) throws ThingsboardException {
        checkParameter(CLIENT_REGISTRATION_ID, strClientRegistrationId);
        try {
            OAuth2ClientRegistrationId clientRegistrationId = new OAuth2ClientRegistrationId(toUUID(strClientRegistrationId));
            oAuth2Service.deleteClientRegistrationById(clientRegistrationId);

            logEntityAction(clientRegistrationId,
                    null,
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


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config/domain/{domain}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClientRegistrationForDomain(@PathVariable(DOMAIN) String domain) throws ThingsboardException {
        checkParameter(DOMAIN, domain);
        try {
            oAuth2Service.deleteClientRegistrationsByDomain(domain);

            logEntityAction(emptyId(EntityType.OAUTH2_CLIENT_REGISTRATION), null,
                    null,
                    ActionType.DELETED, null, domain);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OAUTH2_CLIENT_REGISTRATION),
                    null,
                    null,
                    ActionType.DELETED, e, domain);

            throw handleException(e);
        }
    }
}
