// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.setup.SystemSetupService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemSetupFilterTest {

    private static final String USER_ID = "e5f8c6a0-1111-4a2b-9c3d-000000000001";

    @Mock
    private SystemSetupService systemSetupService;

    private SystemSetupFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new SystemSetupFilter(systemSetupService, new ThingsboardErrorResponseHandler());
    }

    @Test
    public void locksManagementApiWith423AndSetupStateInBody() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestFor("/api/tenant/tenants"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.LOCKED.value());
        JsonNode body = JacksonUtil.toJsonNode(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.LOCKED.value());
        assertThat(body.get("errorCode").asInt()).isEqualTo(ThingsboardErrorCode.SETUP_INCOMPLETE.getErrorCode());
        assertThat(body.get("setupState").asText()).isEqualTo(SystemSetupState.LICENSE_REQUIRED.name());
    }

    /**
     * Derived from the enum rather than listing states, because the filter's rule is "anything other than READY
     * is locked" - a state added to the enum later has to be covered by this test the day it appears, not the
     * day someone remembers to extend a hardcoded list.
     */
    @Test
    public void locksNonWhitelistedApisInAnyIncompleteState() throws Exception {
        List<SystemSetupState> incompleteStates = Arrays.stream(SystemSetupState.values())
                .filter(state -> state != SystemSetupState.READY)
                .toList();
        assertThat(incompleteStates).isNotEmpty();
        for (SystemSetupState state : incompleteStates) {
            when(systemSetupService.getState()).thenReturn(state);
            // The analytics proxy is the second protected entry point, and the only one whose lock nothing
            // else here would notice going missing.
            for (String path : List.of("/api/tenant/tenants", "/api/admin/licenseUsageInfo",
                    "/api/ws/plugins/telemetry", "/apiTrendz/query")) {
                assertThat(passesThrough(path)).as("%s while %s", path, state).isFalse();
            }
        }
    }

    /**
     * The /api/noauth/** entries in the allowlist are listed individually rather than as one wildcard,
     * specifically so that a /api/noauth/** path nothing forces an administrator through - account activation,
     * self-registration submission, the forgot-my-password email flow - stays locked. Pinned here rather than
     * just asserted absent from the source list, since a wildcard regression would make this the only kind of
     * test that could ever catch it.
     */
    @Test
    public void locksUnrelatedNoauthApisWhileLocked() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("GET", "/api/noauth/activate")).as("check activation token").isFalse();
        assertThat(passesThrough("POST", "/api/noauth/activate")).as("account activation").isFalse();
        assertThat(passesThrough("POST", "/api/noauth/signup")).as("self-registration submission").isFalse();
        assertThat(passesThrough("POST", "/api/noauth/resetPasswordByEmail")).as("password reset email").isFalse();
        assertThat(passesThrough("POST", "/api/noauth/activateByEmailCode")).as("activate by email code").isFalse();
    }

    /**
     * The two custom-translation endpoints stay locked. They are read-only and belong to the ready path, which
     * is why they were exempt at first - and is exactly the reasoning the allowlist's own rule rejects, since
     * nothing forces an administrator through either one to reach a completed sign-in. The front end asks for
     * custom translations only once the instance reports a completed setup, and otherwise - and on any failure
     * - loads the bundled static locale files instead, so locking them costs a locked instance nothing. Pinned
     * here so the exemption is not quietly reinstated on the grounds that it looks harmless.
     */
    @Test
    public void locksTheCustomTranslationEndpointsWhileLocked() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("GET", "/api/noauth/translation/login/en_US")).as("login translations").isFalse();
        assertThat(passesThrough("GET", "/api/translation/full/en_US")).as("signed-in translations").isFalse();
    }

    /**
     * A sign-in the platform refuses to complete until the administrator changes an expired password: the login
     * call answers with a reset token instead of a session and the browser is sent to the reset screen, which
     * reads the password policy and then submits the new password. Both have to work while locked, or a system
     * administrator whose password expired has no in-application way back in.
     * <p>
     * GET on the very same path as the submit endpoint is the landing page of the optional forgot-my-password
     * email, which nothing forces anyone through - the method restriction is what keeps the two apart.
     */
    @Test
    public void allowsTheForcedExpiredPasswordChangeButNotTheOptionalPasswordResetEmailFlow() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("POST", "/api/noauth/resetPassword")).as("submit new password").isTrue();
        assertThat(passesThrough("GET", "/api/noauth/userPasswordPolicy")).as("password policy").isTrue();

        assertThat(passesThrough("GET", "/api/noauth/resetPassword")).as("reset email landing").isFalse();
        assertThat(passesThrough("POST", "/api/noauth/resetPasswordByEmail")).as("request reset email").isFalse();
    }

    /**
     * A sign-in the platform refuses to complete until the administrator enrols in two-factor authentication.
     * These live under /api/2fa, with no /auth/ segment, so the /api/auth/2fa/** exemption does not reach them.
     * What stays locked is everything an already-enrolled user may optionally do: PUT and DELETE on the account
     * config path template, and the platform-wide two-factor settings.
     */
    @Test
    public void allowsMandatoryTwoFactorEnrolmentButNotOptionalTwoFactorManagement() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("GET", "/api/2fa/providers")).as("available providers").isTrue();
        assertThat(passesThrough("GET", "/api/2fa/account/settings")).as("account settings").isTrue();
        assertThat(passesThrough("POST", "/api/2fa/account/config/generate")).as("generate config").isTrue();
        assertThat(passesThrough("POST", "/api/2fa/account/config/submit")).as("submit config").isTrue();
        assertThat(passesThrough("POST", "/api/2fa/account/config")).as("verify and save config").isTrue();

        assertThat(passesThrough("PUT", "/api/2fa/account/config")).as("update config").isFalse();
        assertThat(passesThrough("DELETE", "/api/2fa/account/config")).as("delete config").isFalse();
        assertThat(passesThrough("GET", "/api/2fa/settings")).as("read platform settings").isFalse();
        assertThat(passesThrough("POST", "/api/2fa/settings")).as("save platform settings").isFalse();
    }

    /**
     * The sign-in flow reads exactly one user - the caller's own record, by identifier. Everything else hanging
     * off the /api/user/ prefix is management-plane surface that the lock is supposed to close, and an
     * /api/user/* pattern would have opened all of it, since a single * matches any one segment.
     */
    @Test
    public void locksTheRestOfTheUserApiWhileAllowingTheSignInReadOfASingleUser() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("GET", "/api/user/" + USER_ID)).as("the caller's own record").isTrue();

        for (String path : List.of("/api/user/users", "/api/user/settings", "/api/user/tokenAccessEnabled",
                "/api/user/devices", "/api/user/customers", "/api/user/assets", "/api/user/dashboards",
                "/api/user/entityViews", "/api/user/edges")) {
            assertThat(passesThrough(path)).as(path).isFalse();
        }
    }

    /**
     * The HTTP device transport keeps ingesting while the management plane is locked. Pinned here and not in an
     * end-to-end controller test: /api/v1/** is matched by its own higher-precedence SecurityFilterChain, so the
     * FilterChainProxy that MockMvc's springSecurity() setup installs never routes those requests through this
     * filter at all. In a real deployment they reach it through the servlet container's own registration of this
     * filter bean, which MockMvc has no equivalent of - leaving this the only place the exemption can be pinned.
     */
    @Test
    public void keepsTheDeviceApiOpenWhileTheManagementPlaneIsLocked() throws Exception {
        for (String path : List.of("/api/v1/ACCESS_TOKEN/telemetry", "/api/v1/ACCESS_TOKEN/attributes",
                "/api/v1/ACCESS_TOKEN/rpc")) {
            assertThat(passesThrough("POST", path)).as(path).isTrue();
        }
        // Deliberately no stubbed state: the exemption is decided before the state is ever consulted, and
        // verifying that is stronger than asserting the same paths pass under some particular state.
        verifyNoInteractions(systemSetupService);
    }

    @Test
    public void allowsWhitelistedApisWhileLocked() throws Exception {
        for (String path : List.of(
                // First-time setup and the development-mode notice probe.
                "/api/noauth/setup/state", "/api/noauth/setup/license", "/api/noauth/setup/claim",
                "/api/noauth/setup/complete", "/api/noauth/system/development",
                // The rest of what the login page itself loads before anyone signs in - already unauthenticated
                // by design, so exempting these grants nothing new.
                "/api/noauth/whiteLabel/loginWhiteLabelParams",
                "/api/noauth/whiteLabel/loginLogo/tenant/logo", "/api/noauth/whiteLabel/loginFavicon/system/icon",
                "/api/noauth/selfRegistration/signUpSelfRegistrationParams", "/api/noauth/oauth2Clients",
                // The authentication flow: sign in, refresh, current-user, 2FA, sign out.
                "/api/auth/login", "/api/auth/token", "/api/auth/user", "/api/auth/2fa/verification/send",
                "/api/auth/logout",
                // The interstitials a sign-in can be forced through before a session is ever issued: mandatory
                // two-factor enrolment, and an expired password that has to be changed first.
                "/api/2fa/providers", "/api/2fa/account/settings", "/api/2fa/account/config/generate",
                "/api/2fa/account/config/submit", "/api/noauth/userPasswordPolicy",
                // The read-only session bootstrap a successful sign-in completes with.
                "/api/system/params", "/api/permissions/allowedPermissions",
                // The actual remedy.
                "/api/admin/license/key", "/api/admin/license/preview",
                "/api/admin/nonProduction/confirm", "/api/admin/license/clear",
                // Public by design, and carved out of the locked analytics proxy above.
                "/apiTrendz/publicApi/dashboard")) {
            assertThat(passesThrough(path)).as(path).isTrue();
        }
        // As with the device API: an allowlisted path is waved through without the filter asking what state the
        // instance is in, so no state is stubbed and the absence of any interaction is the assertion.
        verifyNoInteractions(systemSetupService);
    }

    /**
     * GET /api/user/{userId}, GET /api/whiteLabel/whiteLabelParams and GET /api/customMenu each share their
     * exact path template with a mutating sibling (DELETE a user, POST saves white-labeling config, POST
     * creates a custom menu) that must stay locked. Pins both halves: the GET the login bootstrap needs
     * passes, and the write on the identical path does not - regression coverage for exactly the "grant a
     * read, accidentally grant the write on the same path too" mistake this method-restriction exists to
     * prevent.
     */
    @Test
    public void allowsTheSessionBootstrapReadOnASharedPathButStillLocksItsMutatingSibling() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("GET", "/api/user/" + USER_ID)).as("GET user").isTrue();
        assertThat(passesThrough("DELETE", "/api/user/" + USER_ID)).as("DELETE user").isFalse();

        assertThat(passesThrough("GET", "/api/whiteLabel/whiteLabelParams")).as("GET whiteLabelParams").isTrue();
        assertThat(passesThrough("POST", "/api/whiteLabel/whiteLabelParams")).as("POST whiteLabelParams").isFalse();

        assertThat(passesThrough("GET", "/api/customMenu")).as("GET customMenu").isTrue();
        assertThat(passesThrough("POST", "/api/customMenu")).as("POST customMenu").isFalse();
    }

    /**
     * A CORS preflight asks whether the request that follows would be allowed; it carries no credentials and
     * performs nothing, and the request it asks about is checked on its own merits when it arrives. Locking one
     * is worse than pointless: an error response carries no CORS headers, so a browser on another origin sees a
     * network failure rather than the lock and signs the user out instead of showing them the recovery screen.
     * <p>
     * Deliberately does not stub {@code getState()} - the strict-stub check would fail an unused stub, so
     * leaving it out and verifying no interaction is itself the assertion that the preflight is waved through
     * without the filter ever asking what state the instance is in.
     */
    @Test
    public void letsACorsPreflightThroughWithoutConsultingTheSetupState() throws Exception {
        MockHttpServletRequest request = requestFor("OPTIONS", "/api/tenant/tenants");
        request.addHeader(HttpHeaders.ORIGIN, "https://another.example.com");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(systemSetupService);
    }

    /**
     * The other half of the preflight exemption: what makes a request a preflight is the Origin and the
     * requested method, not the verb. A bare OPTIONS is neither, and stays locked - otherwise the exemption
     * would be a method-shaped bypass anyone could spell.
     */
    @Test
    public void stillLocksABareOptionsRequestThatIsNotAPreflight() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.LICENSE_REQUIRED);

        assertThat(passesThrough("OPTIONS", "/api/tenant/tenants")).isFalse();
    }

    @Test
    public void allowsNonApiPathsWhileLocked() throws Exception {
        for (String path : List.of("/", "/index.html", "/home/dashboards", "/assets/main.js")) {
            assertThat(passesThrough(path)).as(path).isTrue();
        }
        // Nothing outside the API is ever locked, whatever the state - hence no stub and no interaction.
        verifyNoInteractions(systemSetupService);
    }

    @Test
    public void passesManagementApiThroughWhenReady() throws Exception {
        when(systemSetupService.getState()).thenReturn(SystemSetupState.READY);

        assertThat(passesThrough("/api/tenant/tenants")).isTrue();
    }

    private boolean passesThrough(String path) throws Exception {
        return passesThrough("GET", path);
    }

    private boolean passesThrough(String method, String path) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestFor(method, path), new MockHttpServletResponse(), chain);
        return chain.getRequest() != null;
    }

    private MockHttpServletRequest requestFor(String path) {
        return requestFor("GET", path);
    }

    private MockHttpServletRequest requestFor(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

}
