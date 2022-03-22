/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.JwtTokenPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;

/*
 * FIXME [viacheslav]: Swagger documentation
 * */
@RestController
@RequestMapping("/api/auth/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthController extends BaseController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final JwtTokenFactory tokenFactory;
    private final SystemSecurityService systemSecurityService;
    private final UserService userService;


    @PostMapping("/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public void sendTwoFaVerificationCode() throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, true);
    }

    @PostMapping("/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtTokenPair checkTwoFaVerificationCode(@RequestParam String verificationCode, HttpServletRequest servletRequest) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, true);
        if (verificationSuccess) {
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, null);
            user = new SecurityUser(userService.findUserById(user.getTenantId(), user.getId()), true, user.getUserPrincipal());
            return tokenFactory.createTokenPair(user);
        } else {
            ThingsboardException error = new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, error);
            throw error;
        }
    }

}
