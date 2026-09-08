/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DaoSqlTest
public class OAuth2ClientMapperTest extends AbstractControllerTest {

    @Autowired
    private BasicOAuth2ClientMapper basicOAuth2ClientMapper;
    @Autowired
    private UserService userService;

    @Test
    public void testShouldFindUserOfOwnTenant() throws Exception {
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(TENANT_ADMIN_EMAIL);

        SecurityUser securityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient);
        assertThat(securityUser.getTenantId()).isEqualTo(tenantId);
        assertThat(securityUser.getAuthority()).isEqualTo(Authority.TENANT_ADMIN);
    }

    @Test
    public void testShouldNotFindUserOfAnotherTenant() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // the email attribute is controlled by the identity provider behind the client
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(DIFFERENT_TENANT_ADMIN_EMAIL);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));
        assertThat(exception.getMessage()).isEqualTo("User not found: " + DIFFERENT_TENANT_ADMIN_EMAIL);

        User differentTenantAdmin = userService.findUserByEmail(TenantId.SYS_TENANT_ID, DIFFERENT_TENANT_ADMIN_EMAIL);
        assertThat(differentTenantAdmin.getTenantId()).isEqualTo(differentTenantId);

        loginSysAdmin();
        deleteDifferentTenant();
    }

    @Test
    public void testShouldNotFindSysAdmin() throws Exception {
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(SYS_ADMIN_EMAIL);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));
        assertThat(exception.getMessage()).isEqualTo("User not found: " + SYS_ADMIN_EMAIL);

        User sysAdmin = userService.findUserByEmail(TenantId.SYS_TENANT_ID, SYS_ADMIN_EMAIL);
        assertThat(sysAdmin.getAuthority()).isEqualTo(Authority.SYS_ADMIN);
    }

    @Test
    public void testShouldCreateUserInClientTenant() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // a custom mapper endpoint may return any tenant id; the client's own tenant must win
        String email = "userA@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setTenantId(differentTenantId);

        basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient);

        User created = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        assertThat(created.getTenantId()).isEqualTo(tenantId);

        loginSysAdmin();
        deleteDifferentTenant();
    }

}
