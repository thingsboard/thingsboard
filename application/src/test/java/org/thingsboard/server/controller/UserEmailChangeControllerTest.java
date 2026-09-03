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
package org.thingsboard.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EmailChangeRequest;
import org.thingsboard.server.common.data.EmailChangeResult;
import org.thingsboard.server.common.data.EmailChangeStatus;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.EmailVerificationCode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class UserEmailChangeControllerTest extends AbstractControllerTest {

    @Autowired
    @Qualifier("EmailVerificationCache")
    private TbTransactionalCache<UserId, EmailVerificationCode> emailVerificationCache;

    @Before
    public void stubVerificationMail() throws Exception {
        // Nothing in the repo stubs this one; without it the real sender runs against localhost:25.
        Mockito.doNothing().when(mailService).sendTwoFaVerificationEmail(any(), anyString(), anyString(), anyInt());
    }

    private Tenant createRestrictedTenant() throws Exception {
        loginSysAdmin();
        TenantProfile profile = new TenantProfile();
        profile.setName("Free");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        profile.setProfileData(profileData);
        profile = doPost("/api/tenantProfile", profile, TenantProfile.class);

        Tenant tenant = new Tenant();
        tenant.setTitle("Restricted tenant " + System.currentTimeMillis());
        tenant.setTenantProfileId(profile.getId());
        return saveTenant(tenant);
    }

    private User loginRestrictedTenantAdmin(Tenant tenant, String email) throws Exception {
        User admin = new User();
        admin.setAuthority(Authority.TENANT_ADMIN);
        admin.setTenantId(tenant.getId());
        admin.setEmail(email);
        return createUserAndLogin(admin, "testPassword1");
    }

    @Test
    public void testEmailChangeAppliesImmediatelyForUnrestrictedTenant() throws Exception {
        loginTenantAdmin();

        EmailChangeResult result = doPostWithResponse("/api/user/email",
                new EmailChangeRequest("renamed.tenant.admin@thingsboard.org"), EmailChangeResult.class);

        assertThat(result.status()).isEqualTo(EmailChangeStatus.SUCCESS);
    }

    @Test
    public void testEmailChangeRequiresVerificationForRestrictedTenant() throws Exception {
        Tenant tenant = createRestrictedTenant();
        User admin = loginRestrictedTenantAdmin(tenant, "restricted.change@thingsboard.org");

        EmailChangeResult result = doPostWithResponse("/api/user/email",
                new EmailChangeRequest("restricted.changed@thingsboard.org"), EmailChangeResult.class);
        assertThat(result.status()).isEqualTo(EmailChangeStatus.VERIFICATION_REQUIRED);
        assertThat(result.ttlSeconds()).isPositive();

        // The email must not have changed yet.
        assertThat(doGet("/api/user/" + admin.getId().getId(), User.class).getEmail())
                .isEqualTo("restricted.change@thingsboard.org");

        String code = emailVerificationCache.get(admin.getId()).get().code();

        doPost("/api/user/email/verify?verificationCode=" + code).andExpect(status().isOk());

        loginSysAdmin();
        assertThat(doGet("/api/user/" + admin.getId().getId(), User.class).getEmail())
                .isEqualTo("restricted.changed@thingsboard.org");
    }

    @Test
    public void testEmailChangeRejectsWrongCode() throws Exception {
        Tenant tenant = createRestrictedTenant();
        loginRestrictedTenantAdmin(tenant, "restricted.wrongcode@thingsboard.org");

        doPostWithResponse("/api/user/email",
                new EmailChangeRequest("restricted.wrongcode.new@thingsboard.org"), EmailChangeResult.class);

        doPost("/api/user/email/verify?verificationCode=000000")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Verification code is incorrect")));
    }

    @Test
    public void testEmailChangeFailureCapDiscardsThePendingCode() throws Exception {
        Tenant tenant = createRestrictedTenant();
        loginRestrictedTenantAdmin(tenant, "restricted.cap@thingsboard.org");

        doPostWithResponse("/api/user/email",
                new EmailChangeRequest("restricted.cap.new@thingsboard.org"), EmailChangeResult.class);

        for (int i = 0; i < 5; i++) {
            doPost("/api/user/email/verify?verificationCode=000000").andExpect(status().isBadRequest());
        }
        doPost("/api/user/email/verify?verificationCode=000000")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("No pending email change")));
    }

    @Test
    public void testEmailChangeResendIsThrottled() throws Exception {
        Tenant tenant = createRestrictedTenant();
        loginRestrictedTenantAdmin(tenant, "restricted.resend@thingsboard.org");

        doPostWithResponse("/api/user/email",
                new EmailChangeRequest("restricted.resend.new@thingsboard.org"), EmailChangeResult.class);

        doPost("/api/user/email", new EmailChangeRequest("restricted.resend.other@thingsboard.org"))
                .andExpect(status().isTooManyRequests())
                .andExpect(statusReason(containsString("already been sent")));
    }

    @Test
    public void testEmailChangeThroughSaveUserIsForbiddenForRestrictedTenant() throws Exception {
        Tenant tenant = createRestrictedTenant();
        User admin = loginRestrictedTenantAdmin(tenant, "restricted.saveuser@thingsboard.org");

        admin.setEmail("restricted.saveuser.new@thingsboard.org");
        doPost("/api/user", admin)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't update user email!")));
    }

}
