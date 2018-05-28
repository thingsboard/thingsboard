/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api")
@Slf4j
public class AuthController extends BaseController {



    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MailService mailService;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/auth/user", method = RequestMethod.GET)
    public @ResponseBody User getUser() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            return userService.findUserById(securityUser.getId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/auth/changePassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void changePassword (
            @RequestBody JsonNode changePasswordRequest) throws ThingsboardException {
        try {
            String currentPassword = changePasswordRequest.get("currentPassword").asText();
            String newPassword = changePasswordRequest.get("newPassword").asText();
            SecurityUser securityUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(securityUser.getId());
            if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
                throw new ThingsboardException("Current password doesn't match!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            userCredentials.setPassword(passwordEncoder.encode(newPassword));
            userService.saveUserCredentials(userCredentials);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @RequestMapping(value = "/noauth/activate", params = { "activateToken" }, method = RequestMethod.GET)
    public ResponseEntity<String> checkActivateToken(
            @RequestParam(value = "activateToken") String activateToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(activateToken);
        if (userCredentials != null) {
            String createURI = "/login/createPassword";
            try {
                URI location = new URI(createURI + "?activateToken=" + activateToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", createURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }
    
    @RequestMapping(value = "/noauth/resetPasswordByEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void requestResetPasswordByEmail (
            @RequestBody JsonNode resetPasswordByEmailRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String email = resetPasswordByEmailRequest.get("email").asText();
            UserCredentials userCredentials = userService.requestPasswordReset(email);
            String baseUrl = constructBaseUrl(request);
            String resetUrl = String.format("%s/api/noauth/resetPassword?resetToken=%s", baseUrl,
                    userCredentials.getResetToken());
            
            mailService.sendResetPasswordEmail(resetUrl, email);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @RequestMapping(value = "/noauth/resetPassword", params = { "resetToken" }, method = RequestMethod.GET)
    public ResponseEntity<String> checkResetToken(
            @RequestParam(value = "resetToken") String resetToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        String resetURI = "/login/resetPassword";
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(resetToken);
        if (userCredentials != null) {
            try {
                URI location = new URI(resetURI + "?resetToken=" + resetToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", resetURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }
    
    @RequestMapping(value = "/noauth/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode activateUser(
            @RequestBody JsonNode activateRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String activateToken = activateRequest.get("activateToken").asText();
            String password = activateRequest.get("password").asText();
            String encodedPassword = passwordEncoder.encode(password);
            UserCredentials credentials = userService.activateUserCredentials(activateToken, encodedPassword);
            User user = userService.findUserById(credentials.getUserId());
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
            String baseUrl = constructBaseUrl(request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();

            try {
                mailService.sendAccountActivatedEmail(loginUrl, email);
            } catch (Exception e) {
                log.info("Unable to send account activation email [{}]", e.getMessage());
            }

            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenObject = objectMapper.createObjectNode();
            tokenObject.put("token", accessToken.getToken());
            tokenObject.put("refreshToken", refreshToken.getToken());
            return tokenObject;
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @RequestMapping(value = "/noauth/resetPassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JsonNode resetPassword(
            @RequestBody JsonNode resetPasswordRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String resetToken = resetPasswordRequest.get("resetToken").asText();
            String password = resetPasswordRequest.get("password").asText();
            UserCredentials userCredentials = userService.findUserCredentialsByResetToken(resetToken);
            if (userCredentials != null) {
                String encodedPassword = passwordEncoder.encode(password);
                userCredentials.setPassword(encodedPassword);
                userCredentials.setResetToken(null);
                userCredentials = userService.saveUserCredentials(userCredentials);
                User user = userService.findUserById(userCredentials.getUserId());
                UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
                SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), principal);
                String baseUrl = constructBaseUrl(request);
                String loginUrl = String.format("%s/login", baseUrl);
                String email = user.getEmail();
                mailService.sendPasswordWasResetEmail(loginUrl, email);

                JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
                JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);

                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode tokenObject = objectMapper.createObjectNode();
                tokenObject.put("token", accessToken.getToken());
                tokenObject.put("refreshToken", refreshToken.getToken());
                return tokenObject;
            } else {
                throw new ThingsboardException("Invalid reset token!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
