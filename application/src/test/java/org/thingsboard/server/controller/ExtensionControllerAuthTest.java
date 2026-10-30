// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionSlugInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.service.entitiy.extension.TbExtensionService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for the gateway-auth target {@code GET /api/ext-auth}. The platform security chain already
 * rejects an anonymous/invalid credential with 401 for /api/**, so the direct-call cases assert the
 * tenant-ownership decision: 200 for the owning tenant, 403 otherwise (never 404/500 — nginx auth_request
 * requires 401/403). The last two cases cover the authority requirement, which those cannot reach.
 */
class ExtensionControllerAuthTest {

    private final ExtensionService extensionService = mock(ExtensionService.class);
    private final ExtensionController controller =
            new ExtensionController(extensionService, mock(TbExtensionService.class), Optional.empty(), Optional.empty(),
                    mock(RateLimitService.class));
    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @BeforeEach
    void authenticateAsTenant() {
        authenticateAs(new UserPrincipal(UserPrincipal.Type.USER_NAME, "tenant@thingsboard.org"));
    }

    private void authenticateAs(UserPrincipal principal) {
        SecurityUser user = mock(SecurityUser.class);
        when(user.getTenantId()).thenReturn(tenantId);
        when(user.getUserPrincipal()).thenReturn(principal);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Extension extensionOwnedBy(TenantId owner) {
        Extension ext = new Extension();
        ext.setTenantId(owner);
        return ext;
    }

    @Test
    void allowsWhenCallerTenantOwnsSlug() throws Exception {
        when(extensionService.findBySlug("acme-9f3a")).thenReturn(extensionOwnedBy(tenantId));
        assertThat(controller.authorizeExtensionRequest("acme-9f3a").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void deniesWhenSlugOwnedByAnotherTenant() throws Exception {
        when(extensionService.findBySlug("acme-9f3a"))
                .thenReturn(extensionOwnedBy(new TenantId(UUID.randomUUID())));
        assertThat(controller.authorizeExtensionRequest("acme-9f3a").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deniesWhenSlugUnknown() throws Exception {
        when(extensionService.findBySlug("ghost-0000")).thenReturn(null);
        assertThat(controller.authorizeExtensionRequest("ghost-0000").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deniesWhenSlugHeaderMissing() throws Exception {
        assertThat(controller.authorizeExtensionRequest(null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // /api/auth/login/public is permitAll and mints a CUSTOMER_USER carrying the tenant's tenantId from a
    // public dashboard's customer UUID alone, so the authority + ownership pair below is satisfied by an
    // anonymous caller. Such a principal must never reach a tenant's extension pods.
    @Test
    void deniesPublicPrincipalEvenForAnOwnedSlug() throws Exception {
        authenticateAs(new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, UUID.randomUUID().toString()));
        when(extensionService.findBySlug("acme-9f3a")).thenReturn(extensionOwnedBy(tenantId));

        assertThat(controller.authorizeExtensionRequest("acme-9f3a").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Fail closed: a principal-less SecurityUser must not be treated as a regular user.
    @Test
    void deniesWhenPrincipalIsMissing() throws Exception {
        authenticateAs(null);
        when(extensionService.findBySlug("acme-9f3a")).thenReturn(extensionOwnedBy(tenantId));

        assertThat(controller.authorizeExtensionRequest("acme-9f3a").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // The cases above call the controller directly, so method security never runs in them. These two
    // cover the annotation itself, which is the actual gate: /api/** is mapped to .authenticated() only,
    // so without an authority requirement a PRE_VERIFICATION_TOKEN (password accepted, second factor not
    // yet supplied) or an MFA_CONFIGURATION_TOKEN — both carrying a real tenantId — passes the ownership
    // check above and clears the gate, bypassing 2FA for data-plane access.
    @Test
    void gatewayAuthEndpointRequiresAnAuthority() throws Exception {
        PreAuthorize preAuthorize = ExtensionController.class
                .getMethod("authorizeExtensionRequest", String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).as("GET /api/ext-auth must carry an authority requirement").isNotNull();
        assertThat(preAuthorize.value()).contains("TENANT_ADMIN").contains("CUSTOMER_USER");
    }

    // This endpoint was the only mapped method in the controller without an authority requirement, which
    // is precisely why it was missed. Guard the whole class so a future endpoint cannot repeat it.
    @Test
    void everyMappedEndpointCarriesAnAuthorityRequirement() {
        List<Method> mapped = Arrays.stream(ExtensionController.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.isAnnotationPresent(GetMapping.class)
                        || m.isAnnotationPresent(PostMapping.class)
                        || m.isAnnotationPresent(DeleteMapping.class)
                        || m.isAnnotationPresent(RequestMapping.class))
                .toList();

        // Without this the check below passes vacuously if the annotation filter ever stops matching.
        assertThat(mapped).as("mapped endpoints found by reflection").hasSizeGreaterThanOrEqualTo(10);
        assertThat(mapped.stream().filter(m -> !m.isAnnotationPresent(PreAuthorize.class)).map(Method::getName).toList())
                .as("mapped endpoints without @PreAuthorize").isEmpty();
    }

    // ------------------------------------------------------------------
    // GET /api/extension/slug — name-to-slug lookup for dashboard widgets
    // ------------------------------------------------------------------

    private Extension extensionNamed(String name, String slug, TenantId owner) {
        Extension ext = extensionOwnedBy(owner);
        ext.setName(name);
        ext.setSlug(slug);
        return ext;
    }

    @Test
    void resolvesNameToSlugWithinCallerTenant() throws Exception {
        when(extensionService.findByTenantIdAndName(tenantId, "Acme Billing"))
                .thenReturn(extensionNamed("Acme Billing", "acme-billing-9f3a", tenantId));

        assertThat(controller.getExtensionSlugByName("Acme Billing"))
                .isEqualTo(new ExtensionSlugInfo("acme-billing-9f3a"));
    }

    // The tenant must come from the security context, never from the request, so the lookup cannot be
    // pointed at another tenant no matter what the caller sends.
    @Test
    void slugLookupIsScopedToTheCallerTenant() {
        TenantId otherTenant = new TenantId(UUID.randomUUID());
        when(extensionService.findByTenantIdAndName(otherTenant, "Acme Billing"))
                .thenReturn(extensionNamed("Acme Billing", "acme-billing-9f3a", otherTenant));
        when(extensionService.findByTenantIdAndName(tenantId, "Acme Billing")).thenReturn(null);

        assertThatThrownBy(() -> controller.getExtensionSlugByName("Acme Billing"))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.ITEM_NOT_FOUND);
    }

    // Same control as authorizeExtensionRequest: a published dashboard mints a CUSTOMER_USER principal
    // from a customer UUID alone. It cannot reach the data plane, so it must not learn slugs either.
    @Test
    void deniesPublicPrincipalForSlugLookup() {
        authenticateAs(new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, UUID.randomUUID().toString()));
        when(extensionService.findByTenantIdAndName(tenantId, "Acme Billing"))
                .thenReturn(extensionNamed("Acme Billing", "acme-billing-9f3a", tenantId));

        assertThatThrownBy(() -> controller.getExtensionSlugByName("Acme Billing"))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.PERMISSION_DENIED);
    }

    @Test
    void deniesSlugLookupWhenPrincipalIsMissing() {
        authenticateAs(null);

        assertThatThrownBy(() -> controller.getExtensionSlugByName("Acme Billing"))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.PERMISSION_DENIED);
    }

    @Test
    void unknownNameIsNotFound() {
        when(extensionService.findByTenantIdAndName(tenantId, "Ghost")).thenReturn(null);

        assertThatThrownBy(() -> controller.getExtensionSlugByName("Ghost"))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    void blankNameIsBadRequest() {
        assertThatThrownBy(() -> controller.getExtensionSlugByName("   "))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

    // A caller that omits ?name= altogether is making the same mistake as one that sends it empty, so it
    // must get the same 400. This only holds while the parameter stays `required = false`: with Spring's
    // default the request never reaches the method and the caller sees a 500 instead (observed on the
    // EKS public-cloud run, 2026-08-11).
    @Test
    void missingNameParameterIsBadRequestNotServerError() {
        assertThatThrownBy(() -> controller.getExtensionSlugByName(null))
                .isInstanceOf(ThingsboardException.class)
                .extracting(e -> ((ThingsboardException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

    @Test
    void nameParameterIsNotRequiredBySpringSoTheControllerCanAnswer400() throws NoSuchMethodException {
        RequestParam requestParam = ExtensionController.class
                .getMethod("getExtensionSlugByName", String.class)
                .getParameters()[0]
                .getAnnotation(RequestParam.class);
        assertThat(requestParam).isNotNull();
        assertThat(requestParam.required())
                .as("name must be optional to Spring so a missing param yields 400, not 500")
                .isFalse();
    }

    // The point of this endpoint: the list endpoint is TENANT_ADMIN only, which left a customer-facing
    // dashboard with no way to discover the slug. Method security is the real gate, so pin the annotation.
    @Test
    void slugLookupIsAvailableToCustomerUsers() throws Exception {
        PreAuthorize preAuthorize = ExtensionController.class
                .getMethod("getExtensionSlugByName", String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).as("GET /api/extension/slug must carry an authority requirement").isNotNull();
        assertThat(preAuthorize.value()).contains("TENANT_ADMIN").contains("CUSTOMER_USER");
    }
}
