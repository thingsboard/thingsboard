// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.trendz.TrendzSyncService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/trendz")
public class TrendzController extends BaseController {

    private static final String TRENDZ_PUBLIC_SYNC_ENDPOINT_RATE_LIMIT = "1:60";

    private final TrendzSyncService trendzSyncService;
    private final TrendzSettingsService trendzSettingsService;

    private final RateLimitService rateLimitService;

    @ApiOperation(value = "Get Trendz configuration (getTrendzConfig)",
            notes = "Retrieves Trendz configuration (URLs). Returns trendzUrl and tbUrl." + SYSTEM_AUTHORITY_PARAGRAPH)
    @GetMapping("/config")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public TrendzConfiguration getTrendzConfig(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        TrendzSettings settings = trendzSettingsService.findTrendzSettings();
        return settings != null ? settings.configuration() : new TrendzConfiguration(null, null);
    }

    @ApiOperation(value = "Save Trendz configuration (saveTrendzConfig)",
            notes = "Saves Trendz configuration (URLs only, without triggering synchronization). " +
                    "Request body example:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"trendzUrl\": \"https://trendz.domain.com\",\n" +
                    "  \"tbUrl\": \"https://thingsboard.domain.com\"\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END + SYSTEM_AUTHORITY_PARAGRAPH)
    @PostMapping("/config")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public TrendzConfiguration saveTrendzConfig(@RequestBody TrendzConfiguration config,
                                                @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings();
        if (trendzSettings != null && trendzSettings.configuration() != null && trendzSettings.configuration().equals(config)) {
            return trendzSettings.configuration();
        }
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE);
        TrendzSettings newSettings = new TrendzSettings(config, syncResult);
        trendzSettingsService.saveTrendzSettings(newSettings);
        return config;
    }

    @ApiOperation(value = "Get Trendz synchronization result (getTrendzSyncResult)",
            notes = "Retrieves Trendz synchronization result and status. " +
                    "Returns trendzVersion, updatedTs, resultType, and status." +
                    AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @GetMapping("/sync")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzSynchronizationResult getTrendzSyncResult() {
        TrendzSettings settings = trendzSettingsService.findTrendzSettings();
        if (settings != null && settings.synchronizationResult() != null) {
            return settings.synchronizationResult();
        }
        return new TrendzSynchronizationResult(null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE);
    }

    @ApiOperation(value = "Perform Trendz healthcheck (performTrendzHealthcheck)",
            notes = "Performs healthcheck for Trendz integration. " +
                    "Returns version, type, status, and message. " +
                    "Can only be performed if Trendz is already synchronized and integration is enabled." +
                    AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @GetMapping("/healthcheck")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzHealthcheckResult performTrendzHealthcheck() {
        return trendzSyncService.performHealthcheck();
    }

    @ApiOperation(value = "Connect to Trendz (connectToTrendz)",
            notes = "Initiates synchronization with Trendz (Connect button action). " +
                    "Uses Trendz configuration from settings or falls back to environment variables. " +
                    "Generates API key, saves configuration, checks Trendz version, and performs initial sync. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PostMapping("/connect")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public TrendzSynchronizationResult connectToTrendz(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TrendzSettings result = trendzSyncService.performSync();
        return result.synchronizationResult();
    }

    @ApiOperation(value = "Public connect to Trendz (publicConnectToTrendz)",
            notes = "Initiates synchronization with Trendz if Trendz is not synced yet. " +
                    "Uses Trendz configuration from settings or falls back to environment variables. " +
                    "Generates API key, saves configuration, checks Trendz version, and performs initial sync.")
    @PostMapping("/public/connect")
    public void publicConnectToTrendz() {
        try {
            if (rateLimitService.checkRateLimit(LimitedApi.TRENDZ_PUBLIC_SYNC, (Object) TenantId.SYS_TENANT_ID, TRENDZ_PUBLIC_SYNC_ENDPOINT_RATE_LIMIT)) {
                trendzSyncService.performSyncIfNeeded();
            } else {
                log.warn("Failed to perform Trendz public synchronization: rate limit");
            }
        } catch (Exception e) {
            log.error("Failed to perform Trendz public synchronization.", e);
        }
    }

}
