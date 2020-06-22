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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistration;
import org.thingsboard.server.dao.oauth2.OAuth2ClientRegistrationService;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class OAuth2Controller extends BaseController {
    private static final String REGISTRATION_ID = "registrationId";

    @Autowired
    private OAuth2Service oauth2Service;
    @Autowired
    private OAuth2ClientRegistrationService oAuth2ClientRegistrationService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config/{" + REGISTRATION_ID + "}", method = RequestMethod.GET)
    @ResponseBody
    public OAuth2ClientRegistration getClientRegistrationById(@PathVariable(REGISTRATION_ID) String registrationId) throws ThingsboardException {
        try {
            return oauth2Service.getClientRegistrationByRegistrationId(registrationId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseBody
    public OAuth2ClientRegistration saveClientRegistration(@RequestBody OAuth2ClientRegistration clientRegistration) throws ThingsboardException {
        try {
            return oAuth2ClientRegistrationService.saveClientRegistration(clientRegistration);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
