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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EmailChangeRequest;
import org.thingsboard.server.common.data.EmailChangeResult;
import org.thingsboard.server.common.data.EmailChangeStatus;
import org.thingsboard.server.common.data.SystemParams;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.EmailVerificationCode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.mail.RecipientValidator;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class UserEmailChangeControllerTest extends AbstractControllerTest {

    @Autowired
    @Qualifier("EmailVerificationCache")
    private TbTransactionalCache<UserId, EmailVerificationCode> emailVerificationCache;

    @Autowired
    private RecipientValidator recipientValidator;

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

        // The defining invariant of this flow: the code goes to the new address, and only the new address.
        Mockito.verify(mailService).sendTwoFaVerificationEmail(eq(tenant.getId()), eq("restricted.changed@thingsboard.org"), eq(code), anyInt());

        // The outdating check truncates to whole seconds, so the invalidation event must land in a
        // later second than the token's issuedAt, or the old token would still compare as fresh.
        TimeUnit.SECONDS.sleep(1);
        doPost("/api/user/email/verify?verificationCode=" + code).andExpect(status().isOk());

        // The JWT subject is the email, so the old session must no longer work.
        doGet("/api/auth/user").andExpect(status().isUnauthorized());

        loginSysAdmin();
        assertThat(doGet("/api/user/" + admin.getId().getId(), User.class).getEmail())
                .isEqualTo("restricted.changed@thingsboard.org");
    }

    @Test
    public void testVerifiedEmailChangeMovesTheRecipientAllowance() throws Exception {
        Tenant tenant = createRestrictedTenant();
        final TenantId tenantId = tenant.getId();
        User admin = loginRestrictedTenantAdmin(tenant, "moving.old@thingsboard.org");

        doPostWithResponse("/api/user/email",
                new EmailChangeRequest("moving.new@thingsboard.org"), EmailChangeResult.class);
        String code = emailVerificationCache.get(admin.getId()).get().code();
        doPost("/api/user/email/verify?verificationCode=" + code).andExpect(status().isOk());

        TbEmail toOld = TbEmail.builder().to("moving.old@thingsboard.org").subject("s").body("b").build();
        TbEmail toNew = TbEmail.builder().to("moving.new@thingsboard.org").subject("s").body("b").build();
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, toOld))
                .isInstanceOf(ThingsboardException.class);
        assertThatCode(() -> recipientValidator.validateRecipients(tenantId, toNew))
                .doesNotThrowAnyException();
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
    public void testEmailChangeFailureCapBlocksResendWithoutResettingExpiry() throws Exception {
        Tenant tenant = createRestrictedTenant();
        User admin = loginRestrictedTenantAdmin(tenant, "restricted.cap@thingsboard.org");

        doPostWithResponse("/api/user/email",
                new EmailChangeRequest("restricted.cap.new@thingsboard.org"), EmailChangeResult.class);

        for (int i = 0; i < 5; i++) {
            doPost("/api/user/email/verify?verificationCode=000000").andExpect(status().isBadRequest());
        }
        // Cap reached: a 6th attempt is rejected outright, without comparing the code, and the pending
        // entry is kept rather than discarded - discarding it would let a re-request reset the counter.
        doPost("/api/user/email/verify?verificationCode=000000")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Too many failed attempts")));

        EmailVerificationCode capped = emailVerificationCache.get(admin.getId()).get();
        assertThat(capped.failedAttempts()).isEqualTo(5);

        // Backdate the entry past the resend throttle window instead of sleeping in the test, to prove
        // the refusal below comes from the cap itself and not merely from the resend throttle.
        long backdatedTimestamp = capped.timestamp() - TimeUnit.SECONDS.toMillis(61);
        emailVerificationCache.put(admin.getId(), new EmailVerificationCode(capped.code(), capped.newEmail(),
                backdatedTimestamp, capped.failedAttempts()));

        // A capped re-request must be refused outright: not mailed, and not allowed to touch the entry's
        // timestamp - otherwise the lockout restarts on every resend instead of reaching natural expiry.
        doPost("/api/user/email", new EmailChangeRequest("restricted.cap.new2@thingsboard.org"))
                .andExpect(status().isTooManyRequests())
                .andExpect(statusReason(containsString("Too many failed attempts")));

        // Only the very first request (before the cap was ever reached) should have sent mail.
        Mockito.verify(mailService, Mockito.times(1))
                .sendTwoFaVerificationEmail(eq(tenant.getId()), anyString(), anyString(), anyInt());

        EmailVerificationCode afterRefusedResend = emailVerificationCache.get(admin.getId()).get();
        assertThat(afterRefusedResend.timestamp()).isEqualTo(backdatedTimestamp);
        assertThat(afterRefusedResend.failedAttempts()).isEqualTo(5);
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

    @Test
    public void testEmailChangeRejectsPublicCustomer() throws Exception {
        loginTenantAdmin();

        Customer customer = new Customer();
        customer.setTitle("Public customer " + System.currentTimeMillis());
        customer = doPost("/api/customer", customer, Customer.class);

        Device device = new Device();
        device.setName("Public device " + System.currentTimeMillis());
        device.setCustomerId(customer.getId());
        device = doPost("/api/device", device, Device.class);
        device = doPost("/api/customer/public/device/" + device.getUuidId(), Device.class);

        String publicId = device.getCustomerId().toString();

        resetTokens();
        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        // A public dashboard session must never be able to trigger a mailed verification code.
        doPost("/api/user/email", new EmailChangeRequest("public.customer.new@thingsboard.org"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSystemParamsCarriesRestrictedFlag() throws Exception {
        Tenant tenant = createRestrictedTenant();
        loginRestrictedTenantAdmin(tenant, "restricted.sysparams@thingsboard.org");

        assertThat(doGet("/api/system/params", SystemParams.class).isRestrictedTenantProfile()).isTrue();

        loginTenantAdmin();
        assertThat(doGet("/api/system/params", SystemParams.class).isRestrictedTenantProfile()).isFalse();
    }

}
