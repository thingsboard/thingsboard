// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRequest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "security.sysadmin_allowlist.allowed_addresses=127.0.0.1,::1,203.0.113.0/24"
})
public class AuthAllowlistTest extends AbstractControllerTest {

    private static final String BLOCKED_IP = "10.0.0.9";
    private static final String ALLOWED_IP = "203.0.113.5";

    @Test
    public void sysAdminLoginFromBlockedIpLooksLikeBadCredentials() throws Exception {
        // correct password, blocked IP: the response is byte-identical to a wrong-password response
        mockMvc.perform(loginRequest(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD, BLOCKED_IP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.errorCode").value(ThingsboardErrorCode.AUTHENTICATION.getErrorCode()));
    }

    @Test
    public void sysAdminLoginFromBlockedIpDoesNotIncrementFailedAttempts() throws Exception {
        // wrong-password attempts from a blocked IP must not advance the lockout counter
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(loginRequest(SYS_ADMIN_EMAIL, "wrong-password-" + i, BLOCKED_IP))
                    .andExpect(status().isUnauthorized());
        }
        loginSysAdmin(); // from the default (allowed) loopback address: must not be locked out
    }

    @Test
    public void sysAdminLoginFromAllowedIpSucceeds() throws Exception {
        mockMvc.perform(loginRequest(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD, ALLOWED_IP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void tenantLoginFromBlockedIpSucceeds() throws Exception {
        mockMvc.perform(loginRequest(TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD, BLOCKED_IP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void sysAdminRefreshFromBlockedIpRejected() throws Exception {
        loginSysAdmin();
        mockMvc.perform(refreshRequest(refreshToken, BLOCKED_IP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(ThingsboardErrorCode.IP_NOT_ALLOWED.getErrorCode()));
        // same refresh token from an allowed address still works: the token itself was not invalidated
        mockMvc.perform(refreshRequest(refreshToken, ALLOWED_IP))
                .andExpect(status().isOk());
    }

    @Test
    public void tenantRefreshFromBlockedIpSucceeds() throws Exception {
        loginTenantAdmin();
        mockMvc.perform(refreshRequest(refreshToken, BLOCKED_IP))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder loginRequest(String username, String password, String remoteAddr) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(new LoginRequest(username, password)))
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                });
    }

    private MockHttpServletRequestBuilder refreshRequest(String refreshToken, String remoteAddr) {
        return post("/api/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JacksonUtil.toString(new RefreshTokenRequest(refreshToken)))
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                });
    }

}
