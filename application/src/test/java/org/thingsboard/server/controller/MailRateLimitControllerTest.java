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

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.EmailChangeRequest;
import org.thingsboard.server.common.data.EmailChangeResult;
import org.thingsboard.server.common.data.EmailChangeStatus;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.mail.MailSenderInternalExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = "mail.per_tenant_rate_limits=1:600")
public class MailRateLimitControllerTest extends AbstractControllerTest {

    // Stubbing the mail service itself would skip the rate limit check along with the send, so only the
    // SMTP hop is stubbed out: the real DefaultMailService templated path still runs in full.
    @MockitoSpyBean
    private MailSenderInternalExecutorService mailSenderExecutor;

    @Before
    public void stubSmtpHop() {
        Mockito.doReturn(Futures.immediateFuture(null)).when(mailSenderExecutor).submit(any(Runnable.class));
    }

    @Test
    public void testTemplatedMailIsRateLimitedPerTenant() throws Exception {
        loginSysAdmin();
        TenantProfile profile = new TenantProfile();
        profile.setName("Free");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        profile.setProfileData(profileData);
        profile = doPost("/api/tenantProfile", profile, TenantProfile.class);

        Tenant tenant = new Tenant();
        tenant.setTitle("Rate limited tenant");
        tenant.setTenantProfileId(profile.getId());
        tenant = saveTenant(tenant);

        User firstAdmin = createTenantAdmin(tenant, "ratelimit.first@thingsboard.org");
        loginSysAdmin();
        User secondAdmin = createTenantAdmin(tenant, "ratelimit.second@thingsboard.org");

        // The tenant's single token goes to the first admin's verification code.
        login(firstAdmin.getEmail(), "testPassword1");
        EmailChangeResult result = doPostWithResponse("/api/user/email",
                new EmailChangeRequest("ratelimit.first.new@thingsboard.org"), EmailChangeResult.class);
        assertThat(result.status()).isEqualTo(EmailChangeStatus.VERIFICATION_REQUIRED);

        // The bucket is per tenant, so the second admin's send is refused even though it is their first.
        login(secondAdmin.getEmail(), "testPassword1");
        doPost("/api/user/email", new EmailChangeRequest("ratelimit.second.new@thingsboard.org"))
                .andExpect(status().isTooManyRequests())
                .andExpect(statusReason(containsString("emails sending")));

        loginSysAdmin();
        doDelete("/api/tenant/" + tenant.getId().getId()).andExpect(status().isOk());
    }

    private User createTenantAdmin(Tenant tenant, String email) throws Exception {
        User admin = new User();
        admin.setAuthority(Authority.TENANT_ADMIN);
        admin.setTenantId(tenant.getId());
        admin.setEmail(email);
        return createUserAndLogin(admin, "testPassword1");
    }

}
