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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = UserDataValidator.class)
class UserDataValidatorTest {

    @MockitoBean
    UserDao userDao;
    @MockitoBean
    UserService userService;
    @MockitoBean
    CustomerDao customerDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    TbTenantProfileCache tenantProfileCache;
    @MockitoSpyBean
    UserDataValidator validator;

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    private final UserId userId = new UserId(UUID.fromString("1a2b3c4d-0000-4000-8000-000000000001"));

    private User existing;

    @BeforeEach
    void setUp() {
        existing = user("old@thingsboard.org");
        willReturn(existing).given(userDao).findById(tenantId, userId.getId());
    }

    private User user(String email) {
        User user = new User(userId);
        user.setTenantId(tenantId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail(email);
        // validateUpdate dereferences old.getCustomerId(); User leaves it null by default.
        user.setCustomerId(new CustomerId(ModelConstants.NULL_UUID));
        return user;
    }

    @Test
    void shouldRejectEmailChangeForRestrictedTenant() {
        willReturn(true).given(tenantProfileCache).isRestricted(tenantId);
        assertThatThrownBy(() -> validator.validateUpdate(tenantId, user("new@thingsboard.org")))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update user email!");
    }

    @Test
    void shouldAllowUnchangedEmailForRestrictedTenant() {
        willReturn(true).given(tenantProfileCache).isRestricted(tenantId);
        assertThatCode(() -> validator.validateUpdate(tenantId, user("old@thingsboard.org")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowEmailChangeForUnrestrictedTenant() {
        willReturn(false).given(tenantProfileCache).isRestricted(tenantId);
        assertThatCode(() -> validator.validateUpdate(tenantId, user("new@thingsboard.org")))
                .doesNotThrowAnyException();
    }

}
