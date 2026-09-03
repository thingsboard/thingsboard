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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultMailServiceRateLimitTest {

    @Mock
    private RateLimitService rateLimitService;
    @InjectMocks
    private DefaultMailService mailService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    private void givenRateLimitConfig(String config) {
        ReflectionTestUtils.setField(mailService, "perTenantRateLimitConfig", config);
    }

    @Test
    void shouldThrowWhenRateLimitExceeded() {
        givenRateLimitConfig("2:600");
        given(rateLimitService.checkRateLimit(eq(LimitedApi.EMAILS), any(Object.class), eq("2:600"))).willReturn(false);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(mailService, "checkRateLimit", tenantId))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void shouldPassWhenWithinRateLimit() {
        givenRateLimitConfig("2:600");
        given(rateLimitService.checkRateLimit(eq(LimitedApi.EMAILS), any(Object.class), eq("2:600"))).willReturn(true);

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(mailService, "checkRateLimit", tenantId))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldSkipRateLimitWhenNotConfigured() {
        givenRateLimitConfig("");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(mailService, "checkRateLimit", tenantId))
                .doesNotThrowAnyException();
        verify(rateLimitService, never()).checkRateLimit(any(), any(Object.class), any());
    }

    @Test
    void shouldSkipRateLimitForSysTenant() {
        givenRateLimitConfig("2:600");

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(mailService, "checkRateLimit", TenantId.SYS_TENANT_ID))
                .doesNotThrowAnyException();
        verify(rateLimitService, never()).checkRateLimit(any(), any(Object.class), any());
    }

}
