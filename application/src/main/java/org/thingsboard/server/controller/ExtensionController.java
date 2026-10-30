// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.ExtensionQuota;
import org.thingsboard.server.common.data.extension.ExtensionResourceSizeInfo;
import org.thingsboard.server.common.data.extension.ExtensionSlugInfo;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ExtensionStatus;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.extension.TbExtensionService;
import org.thingsboard.server.service.extension.docs.ExtensionApiDocsService;
import org.thingsboard.server.service.extension.status.ExtensionStateDeriver;
import org.thingsboard.server.service.extension.status.ExtensionStatusService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@Validated
@RestController
@TbCoreComponent
@ConditionalOnExpression(ExtensionMode.WHEN_ENABLED)
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExtensionController extends BaseController {

    @Value("${extensions.routing.base-path:/api/extension/route}")
    private String routeBasePath;

    // "capacity:periodSeconds", per user. The UI dialog polls every 5 s while it is open, so the default
    // leaves room for one open dialog plus manual refreshes; anything beyond that is a script, not a
    // person. Empty disables the limit. Spring splits ${...:default} on the FIRST colon, so the default
    // here is the string "20:60".
    @Value("${extensions.logs.rate-limit:20:60}")
    private String logsRateLimit;

    // Same shape and reasoning as logsRateLimit, for the API docs fetch: it pulls a tenant-controlled body
    // through the router on every call, so one user must not be able to turn it into a load generator.
    @Value("${extensions.docs.rate-limit:20:60}")
    private String apiDocsRateLimit;

    private final ExtensionService extensionService;
    private final TbExtensionService tbExtensionService;
    private final Optional<ExtensionStatusService> statusService;
    private final Optional<ExtensionApiDocsService> apiDocsService;
    private final RateLimitService rateLimitService;

    @ApiOperation(value = "Create or update extension (saveExtension)",
            notes = "Creates a new extension when the `id` is absent in the body, otherwise updates the " +
                    "existing one. On create the body carries `name`, an optional `slugBase`, and " +
                    "`containerSpec`; the slug is allocated server-side and is immutable. On update the " +
                    "mutable fields are `name` and `containerSpec` (image/tag/env/replicas/resources). " +
                    "Returns 202 Accepted with the persisted entity (PROVISIONING state on create); " +
                    "poll `/status` for the live state." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/extension")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Extension saveExtension(@RequestBody @Valid Extension extension) throws Exception {
        SecurityUser user = getCurrentUser();
        extension.setTenantId(user.getTenantId());
        if (extension.getId() == null) {
            extension.setExternalId(null); // server-managed; never honor a client-supplied externalId on create
        }
        checkEntity(extension.getId(), extension, Resource.EXTENSION);
        return tbExtensionService.save(extension, user);
    }

    @ApiOperation(value = "Get extension by id (getExtensionById)",
            notes = "Fetches the full extension entity by its id, including `containerSpec.env` " +
                    "(which may contain secrets) so that an update can round-trip the spec. " +
                    "The list endpoint and audit logs omit `env`." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/extension/{extensionId}")
    public Extension getExtensionById(@Parameter(description = "Extension id", required = true)
                                      @PathVariable UUID extensionId) throws ThingsboardException {
        return checkExtensionId(new ExtensionId(extensionId), Operation.READ);
    }

    @ApiOperation(value = "Get tenant extensions (getExtensions)",
            notes = "Returns a page of the tenant's extensions (light projection). " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/tenant/extensions")
    public PageData<ExtensionInfo> getExtensions(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'contains' filter on name.") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "state"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.EXTENSION, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<ExtensionInfo> extensions = extensionService.findExtensionInfosByTenantId(user.getTenantId(), pageLink);
        enrichWithStatus(user.getTenantId(), extensions.getData());
        return extensions;
    }

    // Resolved for the whole page at once. Calling describe() per row costs two orchestrator round-trips
    // per extension on every node that does not own the row (the status poller only caches what it owns),
    // so a page of 50 became ~100 uncached API calls. Any row the batch cannot answer for keeps the state
    // already carried by the DB row instead of failing the page.
    private void enrichWithStatus(TenantId tenantId, List<ExtensionInfo> page) {
        if (page.isEmpty()) {
            return;
        }
        Map<ExtensionId, ExtensionStatus> statuses = statusService
                .map(svc -> svc.describeAll(tenantId, page))
                .orElse(Map.of());
        for (ExtensionInfo info : page) {
            ExtensionStatus status = statuses.getOrDefault(info.getId(), toStatus(info));
            info.setUrl(status.getUrl());
            info.setReadyReplicas(status.getReadyReplicas());
            info.setDesiredReplicas(status.getDesiredReplicas());
        }
    }

    @ApiOperation(value = "Get tenant extension quota (getExtensionQuota)",
            notes = "Returns the effective extension limits of the tenant (extension count, total CPU, total " +
                    "memory), how much of each is already taken, and how many more extensions of every " +
                    "resource tier still fit. A `null` limit means unlimited. CPU and memory count every " +
                    "extension that is not STOPPED; the extension count covers all states." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/tenant/extensions/quota")
    public ExtensionQuota getExtensionQuota() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.EXTENSION, Operation.READ);
        return extensionService.getQuota(user.getTenantId());
    }

    @ApiOperation(value = "Get extension resource sizes (getExtensionResourceSizes)",
            notes = "Returns the catalog of predefined container resource tiers and the cpu/memory each one " +
                    "resolves to. Pass a tier name as `containerSpec.resources.size` when saving an extension. " +
                    "The list is static (independent of any single extension)." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/extension/resourceSizes")
    public List<ExtensionResourceSizeInfo> getExtensionResourceSizes() {
        return Arrays.stream(ResourceSize.values()).map(ExtensionResourceSizeInfo::of).toList();
    }

    @ApiOperation(value = "Delete extension (deleteExtension)",
            notes = "Deletes the extension and undeploys it asynchronously; kubernetes teardown is " +
                    "retried and any leftovers are removed by the periodic orphan sweep. Returns 202 Accepted." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/extension/{extensionId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteExtension(@Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.DELETE);
        tbExtensionService.delete(extension, user);
    }

    @ApiOperation(value = "Get extension status (getExtensionStatus)",
            notes = "Returns a live status snapshot. Works from any node." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/extension/{extensionId}/status")
    public ExtensionStatus getExtensionStatus(@Parameter(description = "Extension id", required = true)
                                              @PathVariable UUID extensionId) throws ThingsboardException {
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.READ);
        return statusService
                .map(svc -> svc.describe(extension.getTenantId(), extension))
                .orElseGet(() -> toStatus(extension));
    }

    @ApiOperation(value = "Restart extension (restartExtension)",
            notes = "Rolls the extension's pods with the same image. Returns 202 Accepted. A STOPPED " +
                    "extension is scaled to zero and has nothing to roll, so it is rejected — use " +
                    "`/start` instead." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/extension/{extensionId}/restart")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void restartExtension(@Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.WRITE);
        tbExtensionService.restart(extension, user);
    }

    @ApiOperation(value = "Stop extension (stopExtension)",
            notes = "Scales the extension to zero replicas, keeping the entity (state=STOPPED). Returns 202 Accepted." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/extension/{extensionId}/stop")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void stopExtension(@Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.WRITE);
        tbExtensionService.stop(extension, user);
    }

    @ApiOperation(value = "Start extension (startExtension)",
            notes = "Resumes a stopped extension back to its desired replica count. Returns 202 Accepted." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/extension/{extensionId}/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startExtension(@Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.WRITE);
        tbExtensionService.start(extension, user);
    }

    @ApiOperation(value = "Get extension logs (getExtensionLogs)",
            notes = "Tails recent pod logs. Requires Kubernetes orchestration. Rate limited per user " +
                    "(`extensions.logs.rate-limit`); over the limit the call returns 429. Responses are " +
                    "served from a short-lived server-side cache, so two calls within " +
                    "`extensions.logs.cache-ttl-ms` may return the same snapshot, and the tail covers at " +
                    "most `extensions.logs.max-pods` pods." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/extension/{extensionId}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getExtensionLogs(@Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId,
                                   @Parameter(description = "Number of log lines to tail (1..2000).")
                                   @RequestParam(required = false, defaultValue = "200") int lines) throws ThingsboardException {
        if (lines < 1 || lines > 2000) {
            throw new ThingsboardException("Parameter 'lines' must be between 1 and 2000!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.READ);
        // Per user, not per tenant: this bounds one person's client (or script), while leaving a tenant's
        // other admins unaffected by it. Checked AFTER the permission check so an unauthorized caller
        // cannot consume another user's budget, and before any orchestrator work is queued.
        if (!rateLimitService.checkRateLimit(LimitedApi.EXTENSION_LOGS, getCurrentUser().getId(), logsRateLimit)) {
            throw new TbRateLimitsException("Too many extension log requests. Please retry shortly.");
        }
        return statusService
                .map(svc -> svc.tailLogs(extension.getTenantId(), extension, lines))
                .orElse("Log streaming is not available (no extension orchestration backend is active).");
    }

    @ApiOperation(value = "Get extension OpenAPI specification (getExtensionApiDocs)",
            notes = "Fetches the extension's own OpenAPI specification (`/v3/api-docs` or `/openapi.json`) " +
                    "through the extensions router, re-parsed and re-serialized server-side. The caller's " +
                    "credential is forwarded, so the router authorizes the fetch exactly like a data-plane " +
                    "call. Backs the 'API docs' page of the platform UI. Requires " +
                    "`extensions.routing.router-internal-url` to be configured." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/extension/{extensionId}/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode getExtensionApiDocs(
            @Parameter(description = "Extension id", required = true) @PathVariable UUID extensionId,
            @Parameter(hidden = true) @RequestHeader(name = "X-Authorization", required = false) String authorization) throws ThingsboardException {
        Extension extension = checkExtensionId(new ExtensionId(extensionId), Operation.READ);
        // After the permission check (an unauthorized caller must not consume another user's budget) and
        // before the upstream fetch is started.
        if (!rateLimitService.checkRateLimit(LimitedApi.EXTENSION_API_DOCS, getCurrentUser().getId(), apiDocsRateLimit)) {
            throw new TbRateLimitsException("Too many extension API docs requests. Please retry shortly.");
        }
        return apiDocsService
                .orElseThrow(() -> new ThingsboardException("Extension API docs are not available on this node.",
                        ThingsboardErrorCode.GENERAL))
                .fetchApiDocs(extension, authorization);
    }

    // Gateway authentication target for the extension data plane. The router (nginx) calls this via
    // `auth_request` before proxying /api/extension/route/{slug}/... to the extension pod. Spring Security already
    // requires a valid X-Authorization (JWT or ApiKey) for /api/**, so an anonymous caller is rejected
    // with 401 before reaching here; we additionally enforce that the caller's tenant owns the slug.
    // A deny MUST be 401/403 — nginx auth_request turns any other status (404/500) into a 500 for the
    // client — so we return 403 (without revealing whether the slug exists) and never a 404.
    //
    // The authority requirement is load-bearing and NOT redundant with the filter chain: /api/** is only
    // mapped to .authenticated(), and the half-authenticated tokens the platform issues — a
    // PRE_VERIFICATION_TOKEN (password accepted, second factor not yet supplied) and an
    // MFA_CONFIGURATION_TOKEN — carry a populated tenantId, so they would satisfy the ownership check
    // below and pass the gate. Everywhere else those tokens are confined by method security, so this
    // endpoint must do the same or 2FA is bypassable for data-plane access.
    //
    // The authority is not proof of a real account either: /api/auth/login/public is permitAll and mints a
    // CUSTOMER_USER SecurityUser carrying the tenant's tenantId from a public dashboard's customer UUID alone
    // (AbstractAuthenticationProvider.authenticateByPublicId). Such a principal would satisfy both the authority
    // and the ownership check below, so any tenant with a published public dashboard would hand anonymous
    // internet callers an authorized ticket into its extension pods. Public principals are therefore rejected
    // outright — the data plane is for authenticated tenant/customer users only.
    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/ext-auth")
    public ResponseEntity<Void> authorizeExtensionRequest(
            @Parameter(hidden = true) @RequestHeader(name = "X-Ext-Slug", required = false) String slug) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        UserPrincipal principal = user.getUserPrincipal();
        if (principal == null || principal.getType() == UserPrincipal.Type.PUBLIC_ID) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (StringUtils.isBlank(slug)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Extension extension = extensionService.findBySlug(slug);
        if (extension == null || !extension.getTenantId().equals(user.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }

    // Name-to-slug lookup for dashboard widgets. A widget has to build
    // /api/extension/route/{slug}/... , but the slug is environment-specific — allocated per instance
    // and, in public cloud, carrying a random suffix — so a dashboard that hardcodes it stops being
    // portable between dev/test/prod. The name is stable, so the widget holds the name and resolves the
    // slug once on load.
    //
    // CUSTOMER_USER is included on purpose: the list endpoint above is TENANT_ADMIN only, which left a
    // customer-facing dashboard with no way to discover the slug at all. Access to the extension itself
    // is unchanged — /api/ext-auth still gates every data-plane call on tenant ownership.
    //
    // Public principals are rejected exactly as in authorizeExtensionRequest: a published dashboard mints
    // a CUSTOMER_USER SecurityUser from a customer UUID alone, and such a caller cannot reach the data
    // plane anyway, so handing it slugs would only leak the tenant's extension inventory to the internet.
    //
    // Returns the slug and nothing else — a caller that only needs to address the data plane must not
    // receive the container spec (it carries env and a registry secret reference), the state, or the id.
    @ApiOperation(value = "Get extension slug by name (getExtensionSlugByName)",
            notes = "Resolves an extension name to its routing slug within the current tenant. Intended for " +
                    "dashboard widgets, which hold the stable name and need the environment-specific slug to " +
                    "call `/api/extension/route/{slug}/...`. Returns only the slug. Available for users with " +
                    "'TENANT_ADMIN' or 'CUSTOMER_USER' authority; public (unauthenticated dashboard) " +
                    "principals are rejected.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/extension/slug")
    public ExtensionSlugInfo getExtensionSlugByName(
            @Parameter(description = "Extension name, unique within the tenant.", required = true)
            // required = false on purpose: with Spring's default the missing-parameter case throws
            // MissingServletRequestParameterException, which the error handler reports as a 500. A caller
            // that simply forgot the parameter made a bad request, so let it fall through to the blank
            // check below and answer 400 like every other malformed input. (Verified on EKS 2026-08-11.)
            @RequestParam(required = false) String name) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        UserPrincipal principal = user.getUserPrincipal();
        if (principal == null || principal.getType() == UserPrincipal.Type.PUBLIC_ID) {
            throw new ThingsboardException("Public access to extensions is not allowed!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (StringUtils.isBlank(name)) {
            throw new ThingsboardException("Extension name should be specified!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        // Tenant comes from the security context, never from the request: this lookup must not be able to
        // reach across tenants no matter what the caller sends.
        Extension extension = extensionService.findByTenantIdAndName(user.getTenantId(), name);
        if (extension == null) {
            throw new ThingsboardException("Extension with name '" + name + "' is not deployed!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return new ExtensionSlugInfo(extension.getSlug());
    }

    private Extension checkExtensionId(ExtensionId extensionId, Operation operation) throws ThingsboardException {
        return checkEntityId(extensionId, extensionService::findExtensionById, operation);
    }

    private ExtensionStatus toStatus(Extension extension) {
        ExtensionStatus status = new ExtensionStatus();
        status.setExtensionId(extension.getId());
        status.setSlug(extension.getSlug());
        status.setState(extension.getState());
        ExtensionContainerSpec spec = extension.getContainerSpec();
        status.setDesiredReplicas(spec != null ? spec.getReplicas() : null);
        status.setReadyReplicas(null); // populated from the cluster once orchestration is enabled
        status.setUrl(routeBasePath + "/" + extension.getSlug());
        // Never surface an error on an ACTIVE extension; gate the stored error on state.
        String effectiveError = ExtensionStateDeriver.effectiveError(extension.getState(), null, extension.getLastError());
        status.setLastError(effectiveError);
        status.setLastErrorAt(effectiveError != null ? extension.getLastErrorAt() : null);
        status.setSnapshotAt(System.currentTimeMillis());
        if (extension.getState() == ExtensionState.STOPPED) {
            status.setDesiredReplicas(0);
        }
        return status;
    }

}
