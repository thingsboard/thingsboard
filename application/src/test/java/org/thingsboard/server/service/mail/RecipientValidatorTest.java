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
package org.thingsboard.server.service.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest(classes = RecipientValidator.class)
class RecipientValidatorTest {

    @MockitoBean
    private TbTenantProfileCache tenantProfileCache;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserAuthDetailsCache userAuthDetailsCache;

    @Autowired
    private RecipientValidator recipientValidator;

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantProfileCache).isRestricted(tenantId);
    }

    private void givenUser(String email, boolean activated) {
        User user = new User(new UserId(UUID.randomUUID()));
        user.setTenantId(tenantId);
        user.setEmail(email);
        willReturn(user).given(userService).findUserByTenantIdAndEmail(tenantId, email);
        willReturn(new UserAuthDetails(user, activated)).given(userAuthDetailsCache).getUserAuthDetails(tenantId, user.getId());
    }

    private TbEmail emailTo(String to) {
        return TbEmail.builder().to(to).subject("s").body("b").build();
    }

    @Test
    void shouldAllowActivatedUserOfTheTenant() {
        givenUser("member@thingsboard.org", true);
        assertThatCode(() -> recipientValidator.validateRecipients(tenantId, emailTo("member@thingsboard.org")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUnknownAddress() {
        willReturn(null).given(userService).findUserByTenantIdAndEmail(tenantId, "victim@example.com");
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, emailTo("victim@example.com")))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("victim@example.com");
    }

    @Test
    void shouldRejectUserThatIsNotActivated() {
        givenUser("pending@thingsboard.org", false);
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, emailTo("pending@thingsboard.org")))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("pending@thingsboard.org");
    }

    @Test
    void shouldRejectAddressHiddenAfterCommaInTo() {
        givenUser("member@thingsboard.org", true);
        willReturn(null).given(userService).findUserByTenantIdAndEmail(tenantId, "victim@example.com");
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId,
                emailTo("member@thingsboard.org , victim@example.com")))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("victim@example.com");
    }

    @Test
    void shouldRejectAddressInCcAndBcc() {
        givenUser("member@thingsboard.org", true);
        willReturn(null).given(userService).findUserByTenantIdAndEmail(tenantId, "victim@example.com");

        TbEmail withCc = TbEmail.builder().to("member@thingsboard.org").cc("victim@example.com").subject("s").body("b").build();
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, withCc))
                .isInstanceOf(ThingsboardException.class);

        TbEmail withBcc = TbEmail.builder().to("member@thingsboard.org").bcc("victim@example.com").subject("s").body("b").build();
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, withBcc))
                .isInstanceOf(ThingsboardException.class);
    }

    @Test
    void shouldAllowAnythingForUnrestrictedTenant() {
        willReturn(false).given(tenantProfileCache).isRestricted(tenantId);
        assertThatCode(() -> recipientValidator.validateRecipients(tenantId, emailTo("anyone@example.com")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldForceSystemFromForRestrictedTenant() {
        TbEmail spoofed = TbEmail.builder().to("member@thingsboard.org").from("ceo@bank.example").subject("s").body("b").build();
        assertThat(recipientValidator.resolveFrom(tenantId, spoofed, "noreply@thingsboard.io"))
                .isEqualTo("noreply@thingsboard.io");
    }

    @Test
    void shouldKeepTenantFromForUnrestrictedTenant() {
        willReturn(false).given(tenantProfileCache).isRestricted(tenantId);
        TbEmail custom = TbEmail.builder().to("anyone@example.com").from("support@customer.example").subject("s").body("b").build();
        assertThat(recipientValidator.resolveFrom(tenantId, custom, "noreply@thingsboard.io"))
                .isEqualTo("support@customer.example");
    }

    @Test
    void shouldRejectMalformedRecipientWithoutMaskingItAsAnInternalFailure() {
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, emailTo("not-an-email")))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("not-an-email");
    }

    @Test
    void shouldFailClosedWhenLookupThrows() {
        willThrow(new RuntimeException("db is down")).given(userService).findUserByTenantIdAndEmail(tenantId, "member@thingsboard.org");
        assertThatThrownBy(() -> recipientValidator.validateRecipients(tenantId, emailTo("member@thingsboard.org")))
                .isInstanceOf(ThingsboardException.class);
    }

}
