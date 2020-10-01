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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientsParams;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class OAuth2Controller extends BaseController {
    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request) throws ThingsboardException {
        try {
            return oAuth2Service.getOAuth2Clients(request.getScheme(), request.getServerName());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public OAuth2ClientsParams getCurrentOAuth2Params() throws ThingsboardException {
        try {
            return oAuth2Service.findOAuth2Params();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2ClientsParams saveOAuth2Params(@RequestBody OAuth2ClientsParams oauth2Params) throws ThingsboardException {
        try {
            oAuth2Service.saveOAuth2Params(oauth2Params);
            return oAuth2Service.findOAuth2Params();
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
