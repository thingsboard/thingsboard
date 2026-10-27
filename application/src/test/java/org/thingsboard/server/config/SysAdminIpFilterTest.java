// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "security.sysadmin_allowlist.allowed_addresses=127.0.0.1,::1,203.0.113.0/24"
})
public class SysAdminIpFilterTest extends AbstractControllerTest {

    private static final String BLOCKED_IP = "10.0.0.9";
    private static final String ALLOWED_IP = "203.0.113.5";

    @Autowired
    private FilterRegistrationBean<ClientAddressCaptureFilter> clientAddressCaptureFilter;

    @Autowired
    private org.springframework.web.context.WebApplicationContext webAppContext;

    @Test
    public void sysAdminTokenRejectedFromOutsideList() throws Exception {
        loginSysAdmin();
        mockMvc.perform(authenticated(get("/api/admin/securitySettings"), BLOCKED_IP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(ThingsboardErrorCode.IP_NOT_ALLOWED.getErrorCode()))
                .andExpect(jsonPath("$.message").value("Access from your network location is not permitted"));
    }

    @Test
    public void sysAdminTokenAcceptedFromInsideList() throws Exception {
        loginSysAdmin();
        mockMvc.perform(authenticated(get("/api/admin/securitySettings"), ALLOWED_IP))
                .andExpect(status().isOk());
    }

    @Test
    public void tenantAdminUnaffectedFromOutsideList() throws Exception {
        loginTenantAdmin();
        mockMvc.perform(authenticated(get("/api/auth/user"), BLOCKED_IP))
                .andExpect(status().isOk());
    }

    @Test
    public void sysAdminApiKeyRejectedFromOutsideList() throws Exception {
        loginSysAdmin();
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("sysadmin PAT for allowlist test");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(currentUserId);
        ApiKey apiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        MockHttpServletRequestBuilder blocked = get("/api/auth/user")
                .header(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER, "ApiKey " + apiKey.getValue());
        mockMvc.perform(blocked.with(request -> {
                    request.setRemoteAddr(BLOCKED_IP);
                    return request;
                }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(ThingsboardErrorCode.IP_NOT_ALLOWED.getErrorCode()));

        MockHttpServletRequestBuilder allowed = get("/api/auth/user")
                .header(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER, "ApiKey " + apiKey.getValue());
        mockMvc.perform(allowed.with(request -> {
                    request.setRemoteAddr(ALLOWED_IP);
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    public void spoofedXffDoesNotBypass() throws Exception {
        loginSysAdmin();
        // full production-like stack: ForwardedHeaderFilter rewrites getRemoteAddr() from the forged
        // header, then the capture filter unwraps back to the raw socket address
        MockMvc fullStackMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .apply(springSecurity())
                .addFilters(new ForwardedHeaderFilter(), clientAddressCaptureFilter.getFilter())
                .build();
        fullStackMvc.perform(authenticated(get("/api/admin/securitySettings"), BLOCKED_IP)
                        .header("X-Forwarded-For", ALLOWED_IP))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(ThingsboardErrorCode.IP_NOT_ALLOWED.getErrorCode()));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder builder, String remoteAddr) {
        builder.header(ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER, "Bearer " + token);
        return builder.with(request -> {
            request.setRemoteAddr(remoteAddr);
            return request;
        });
    }

}
