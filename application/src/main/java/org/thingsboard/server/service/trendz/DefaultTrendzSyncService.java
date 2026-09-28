// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.trendz.TrendzSyncService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzSyncService implements TrendzSyncService {

    private static final String MIN_SUPPORTED_VERSION = "1.15.0";

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final TrendzSettingsService trendzSettingsService;
    private final SystemSecurityService systemSecurityService;
    private final TrendzClient trendzClient;

    @Value("${trendz.enabled:true}")
    private boolean trendzEnabled;

    @Value("${trendz.default_tb_url:}")
    private String defaultTbUrl;

    @Value("${trendz.default_trendz_url:}")
    private String defaultTrendzUrl;

    @Override
    public synchronized TrendzSettings performSync() {
        log.trace("Executing performSync");
        if (!trendzEnabled) {
            return saveTrendzSettings(null, null, null, 0L,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
        }

        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings();

        if (!isValidTrendzConfiguration(trendzSettings)) {
            trendzSettings = createDefaultTrendzSettings(trendzSettings);
        }

        String tbUrl = trendzSettings.configuration().tbUrl();
        String trendzUrl = trendzSettings.configuration().trendzUrl();

        if (tbUrl == null || trendzUrl == null) {
            return saveTrendzSettings(trendzUrl, tbUrl, null, 0L,
                    TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
        }

        log.trace("Starting Trendz synchronization. Trendz URL: {}, TB URL: {}", trendzUrl, tbUrl);

        long updatedTs = System.currentTimeMillis();

        ApiKey trendzApiKey = findOrCreateTrendzApiKey();

        TrendzInfo trendzInfo = validateTrendzConnectionInfo(trendzUrl, tbUrl, updatedTs);
        if (trendzInfo == null) {
            log.debug("Trendz validation failed, sync result is already in settings");
            return trendzSettingsService.findTrendzSettings();
        }

        String trendzVersion = trendzInfo.version();

        String externalTbUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, null);
        TrendzHealthcheckResult syncResult = trendzClient.processTrendzInitRequest(trendzUrl, tbUrl, externalTbUrl != null ? externalTbUrl : tbUrl, trendzApiKey.getValue(), null);
        if (syncResult == null) {
            log.debug("Failed to initiate synchronization with Trendz");
            return saveTrendzSettings(trendzUrl, tbUrl, trendzVersion, updatedTs,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.AVAILABLE);
        }

        TrendzSettings settings = saveTrendzSettings(trendzUrl, tbUrl, syncResult.version(), updatedTs, syncResult.type(), syncResult.status());
        if (syncResult.type() != TrendzSynchronizationResultType.SYNC_COMPLETED) {
            log.debug("Trendz sync failed. Status: {}, Message: {}", syncResult.type(), syncResult.message());
        }

        log.info("Trendz synchronization completed. Status: {}, Result: {}",
                settings.synchronizationResult().status(),
                settings.synchronizationResult().type());
        return settings;
    }

    @Override
    public void performSyncIfNeeded() {
        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings();
        if (isSyncedUp(trendzSettings)) {
            log.trace("Trendz is already synced up. Status: {}, Result: {}",
                    trendzSettings.synchronizationResult().status(), trendzSettings.synchronizationResult().type());
        } else {
            performSync();
        }
    }

    @Override
    public TrendzHealthcheckResult performHealthcheck() {
        log.trace("Executing performHealthcheck");
        if (!trendzEnabled) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.SYNC_DISABLED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_DISABLED.getMessage()
            );
        }

        TrendzSettings trendzSettings = trendzSettingsService.findTrendzSettings();
        if (!isSyncedUp(trendzSettings)) {
            return new TrendzHealthcheckResult(
                    null,
                    TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED.getMessage()
            );
        }

        String trendzUrl = trendzSettings.configuration().trendzUrl();
        JsonNode rawResponse = trendzClient.checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            return new TrendzHealthcheckResult(
                    trendzSettings.synchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE.getMessage()
            );
        }

        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            return new TrendzHealthcheckResult(
                    trendzSettings.synchronizationResult().version(),
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.NOT_AVAILABLE,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR.getMessage()
            );
        }

        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            return new TrendzHealthcheckResult(
                    trendzSettings.synchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION.getMessage()
            );
        }

        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || !trendzApiKey.isInternal()) {
            return new TrendzHealthcheckResult(
                    trendzSettings.synchronizationResult().version(),
                    TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID,
                    TrendzSynchronizationStatus.AVAILABLE,
                    TrendzSynchronizationResultType.TRENDZ_AUTH_INVALID.getMessage()
            );
        }

        return trendzClient.sendHealthcheckRequest(trendzUrl, trendzApiKey.getValue());
    }

    @Override
    public void performApiKeyRotationSync(ApiKey newApiKey, ApiKey oldApiKey) {
        log.trace("Executing performApiKeyRotationSync");
        if (!trendzEnabled) {
            return;
        }
        try {
            if (!TRENDZ_API_KEY_DESCRIPTION.equals(newApiKey.getDescription()) || !newApiKey.isInternal()) {
                return;
            }

            log.trace("Notifying Trendz about API key rotation. API Key ID: {}", newApiKey.getId());

            TrendzSettings settings = trendzSettingsService.findTrendzSettings();
            if (settings == null || settings.configuration() == null) {
                log.debug("Trendz settings not found, cannot notify about key rotation");
                return;
            }

            String trendzUrl = settings.configuration().trendzUrl();
            String tbUrl = settings.configuration().tbUrl();

            if (StringUtils.isEmpty(trendzUrl) || StringUtils.isEmpty(tbUrl)) {
                log.debug("Trendz URL or TB URL not configured, cannot notify about key rotation");
                return;
            }

            String externalTbUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, null);
            trendzClient.processTrendzInitRequest(trendzUrl, tbUrl, externalTbUrl != null ? externalTbUrl : tbUrl, newApiKey.getValue(), oldApiKey != null ? oldApiKey.getValue() : null);
        } catch (Exception e) {
            log.debug("Error notifying Trendz about API key rotation", e);
        }
    }

    private boolean isSyncedUp(TrendzSettings settings) {
        return settings != null
                && settings.synchronizationResult() != null
                && settings.synchronizationResult().status() != null
                && settings.synchronizationResult().status() != TrendzSynchronizationStatus.NOT_AVAILABLE;
    }

    private TrendzSettings createDefaultTrendzSettings(TrendzSettings prevVersion) {
        String trendzUrl;
        if (prevVersion != null && prevVersion.configuration() != null && prevVersion.configuration().trendzUrl() != null) {
            trendzUrl = prevVersion.configuration().trendzUrl();
        } else if (StringUtils.isNotBlank(defaultTrendzUrl)) {
            trendzUrl = defaultTrendzUrl;
        } else {
            trendzUrl = null;
        }

        String tbUrl;
        if (prevVersion != null && prevVersion.configuration() != null && prevVersion.configuration().tbUrl() != null) {
            tbUrl = prevVersion.configuration().tbUrl();
        } else if (StringUtils.isNotBlank(defaultTbUrl)) {
            tbUrl = defaultTbUrl;
        } else {
            tbUrl = null;
        }

        TrendzSettings settings = new TrendzSettings(
                new TrendzConfiguration(
                        trendzUrl, tbUrl
                ),
                new TrendzSynchronizationResult(
                        null, 0L, TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED, TrendzSynchronizationStatus.NOT_AVAILABLE
                )
        );

        trendzSettingsService.saveTrendzSettings(settings);
        return settings;
    }

    private TrendzInfo validateTrendzConnectionInfo(String trendzUrl, String tbUrl, long updatedTs) {
        // Step 1: Check if Trendz is reachable (get raw JSON response)
        JsonNode rawResponse = trendzClient.checkTrendzReachability(trendzUrl);
        if (rawResponse == null) {
            saveTrendzSettings(trendzUrl, tbUrl, null, updatedTs,
                    TrendzSynchronizationResultType.TRENDZ_URL_UNREACHABLE,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.debug("Trendz is not reachable at URL: {}", trendzUrl);
            return null;
        }

        // Step 2: Try to parse JSON response into the TrendzInfo structure
        TrendzInfo trendzInfo;
        try {
            trendzInfo = JacksonUtil.convertValue(rawResponse, TrendzInfo.class);
        } catch (Exception e) {
            saveTrendzSettings(trendzUrl, tbUrl, null, updatedTs,
                    TrendzSynchronizationResultType.SYNC_INTERNAL_ERROR,
                    TrendzSynchronizationStatus.NOT_AVAILABLE);
            log.debug("Trendz version info is not recognized from URL: {} - unexpected JSON structure", trendzUrl, e);
            return null;
        }

        // Step 3: Validate version field is present and the Trendz version is supported
        if (trendzInfo != null && !isVersionSupported(trendzInfo.version())) {
            saveTrendzSettings(trendzUrl, tbUrl, trendzInfo.version(), updatedTs,
                    TrendzSynchronizationResultType.TRENDZ_UNSUPPORTED_VERSION,
                    TrendzSynchronizationStatus.AVAILABLE);
            log.debug("Trendz version {} is not supported. Minimum required version: {}", trendzInfo.version(), MIN_SUPPORTED_VERSION);
            return null;
        }

        // All validations passed
        return trendzInfo;
    }

    private TrendzSettings createSettings(String trendzUrl, String tbUrl,
                                          String trendzVersion, Long updatedTs,
                                          TrendzSynchronizationResultType resultType,
                                          TrendzSynchronizationStatus status) {
        TrendzConfiguration config = new TrendzConfiguration(trendzUrl, tbUrl);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(trendzVersion, updatedTs, resultType, status);
        return new TrendzSettings(config, syncResult);
    }

    private boolean isVersionSupported(String version) {
        if (version == null || version.isBlank()) {
            log.debug("Version is null or empty, treating as unsupported");
            return false;
        }

        try {
            String cleanVersion = version.split("-")[0]; // Remove suffix if present
            String[] versionParts = cleanVersion.split("\\.");
            String[] minVersionParts = MIN_SUPPORTED_VERSION.split("\\.");

            for (int i = 0; i < Math.min(versionParts.length, minVersionParts.length); i++) {
                int current = Integer.parseInt(versionParts[i]);
                int required = Integer.parseInt(minVersionParts[i]);

                if (current > required) {
                    return true;
                } else if (current < required) {
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse version '{}': Invalid number format in version string", version, e);
            return false;
        } catch (Exception e) {
            log.warn("Failed to parse version '{}': {}", version, e.getMessage(), e);
            return false;
        }
    }

    private ApiKey findOrCreateTrendzApiKey() {
        PageLink pageLink = new PageLink(1, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC));
        User sysAdminUser = userService.findSysAdmins(pageLink).getData().get(0);

        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);

        if (trendzApiKey != null && trendzApiKey.isInternal()) {
            log.trace("Found existing Trendz API key: {}", trendzApiKey.getId());
            return trendzApiKey;
        }

        log.trace("Creating new Trendz internal API key with configured permissions");
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(sysAdminUser.getId());
        apiKeyInfo.setDescription(TRENDZ_API_KEY_DESCRIPTION);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setExpirationTime(0);
        apiKeyInfo.setInternal(true);
        apiKeyInfo.setPermissions(buildTrendzPermissions());

        return apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
    }

    private AuthorityPermissionsInfo buildTrendzPermissions() {
        Map<Authority, Map<Resource, Set<Operation>>> permissions = new HashMap<>();

        // TENANT_ADMIN + CUSTOMER_USER permissions (same for both)
        Map<Resource, Set<Operation>> tenantCustomerPermissions = new HashMap<>();
        tenantCustomerPermissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY, Operation.READ_CALCULATED_FIELD));
        tenantCustomerPermissions.put(Resource.ASSET, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY, Operation.READ_CALCULATED_FIELD));
        tenantCustomerPermissions.put(Resource.ALARM, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        tenantCustomerPermissions.put(Resource.CUSTOMER, Set.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY, Operation.READ_CALCULATED_FIELD));
        tenantCustomerPermissions.put(Resource.DASHBOARD, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE));

        tenantCustomerPermissions.put(Resource.TENANT, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.USER, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.WHITE_LABELING, Set.of(Operation.READ));
        tenantCustomerPermissions.put(Resource.DEVICE_PROFILE, Set.of(Operation.READ, Operation.READ_CALCULATED_FIELD));
        tenantCustomerPermissions.put(Resource.ASSET_PROFILE, Set.of(Operation.READ, Operation.READ_CALCULATED_FIELD));

        permissions.put(Authority.TENANT_ADMIN, tenantCustomerPermissions);
        permissions.put(Authority.CUSTOMER_USER, tenantCustomerPermissions);

        // SYS_ADMIN permissions
        Map<Resource, Set<Operation>> sysAdminPermissions = new HashMap<>();
        sysAdminPermissions.put(Resource.ADMIN_SETTINGS, Set.of(Operation.READ, Operation.WRITE));
        sysAdminPermissions.put(Resource.WIDGETS_BUNDLE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        sysAdminPermissions.put(Resource.WIDGET_TYPE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));
        sysAdminPermissions.put(Resource.TB_RESOURCE, Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE));

        permissions.put(Authority.SYS_ADMIN, sysAdminPermissions);

        AuthorityPermissionsInfo permissionsInfo = new AuthorityPermissionsInfo();
        permissionsInfo.setOperationsByResource(permissions);
        return permissionsInfo;
    }

    private TrendzSettings saveTrendzSettings(String trendzUrl, String tbUrl,
                                              String version, long updatedTs,
                                              TrendzSynchronizationResultType resultType,
                                              TrendzSynchronizationStatus status) {
        TrendzSettings settings = createSettings(trendzUrl, tbUrl, version, updatedTs, resultType, status);
        trendzSettingsService.saveTrendzSettings(settings);
        return settings;
    }

    private boolean isValidTrendzConfiguration(TrendzSettings settings) {
        return settings != null
                && settings.configuration() != null
                && settings.configuration().tbUrl() != null
                && settings.configuration().trendzUrl() != null;
    }

    private record TrendzInfo(@JsonProperty("version") String version,
                              @JsonProperty("artifact") String artifact,
                              @JsonProperty("name") String name,
                              @JsonProperty("cloud") Boolean cloud,
                              @JsonProperty("test") Boolean test,
                              @JsonProperty("time") String time
    ) implements Serializable {}

}
