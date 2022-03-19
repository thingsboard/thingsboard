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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.model.JwtTokenPair;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

/*
 *
 * TODO [viacheslav]:
 *  - Configurable hardlock (user blocking) after a total of XX (10) unsuccessful attempts - on user level
 *
 * FIXME [viacheslav]:
 *  - Tests for 2FA
 *  - Swagger documentation
 *
 * */
// TODO [viacheslav]: maybe get rid of sessionId concept..

/*
 *
 *
 * TODO (later):
 *  - 2FA entries should be secured against code injection by code validation
 *  - ability to force users to use 2FA (maybe on log in, do not give them token pair but to give temporary
 *      token to configure 2FA account config); also will need to make users configure 2FA during activation and password setup...
 * */
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController extends BaseController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final JwtTokenFactory tokenFactory;


    @PostMapping("/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public void sendTwoFaVerificationCode() throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, true);
    }

    @PostMapping("/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtTokenPair checkTwoFaVerificationCode(@RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, true);
        if (verificationSuccess) {
            // FIXME [viacheslav]: log login action
            return tokenFactory.createTokenPair(user);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

}
