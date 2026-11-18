// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.id.TenantId;
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
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.trendz.TrendzSyncService;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

@DaoSqlTest
public class TrendzControllerTest extends AbstractControllerTest {

    @MockitoBean
    private TrendzSyncService trendzSyncService;

    @Autowired
    private TrendzSettingsService trendzSettingsService;

    @Autowired
    private ApiKeyService apiKeyService;

    private static final String TB_URL = "https://tb.example.com";
    private static final String TRENDZ_URL = "https://trendz.example.com";
    private static final String TRENDZ_VERSION = "1.15.0";

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @After
    public void tearDown() {
        TrendzSettings settings = trendzSettingsService.findTrendzSettings();
        if (settings != null) {
            trendzSettingsService.deleteTrendzSettings();
        }
    }

    @Test
    public void testGetTrendzConfig_asSysAdmin() throws Exception {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzConfiguration result = doGet("/api/trendz/config", TrendzConfiguration.class);

        assertThat(result).isNotNull();
        assertThat(result.trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(result.tbUrl()).isEqualTo(TB_URL);
    }

    @Test
    public void testGetTrendzConfig_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        doGet("/api/trendz/config").andExpect(status().isForbidden());
    }

    @Test
    public void testGetTrendzConfig_asTrendzApiKeyWithoutAdminSettings() throws Exception {
        trendzSettingsService.saveTrendzSettings(new TrendzSettings(new TrendzConfiguration(TRENDZ_URL, TB_URL), null));
        ApiKey apiKey = saveSysAdminInternalApiKey(TRENDZ_API_KEY_DESCRIPTION);
        resetTokens();
        setApiKey(apiKey.getValue());
        try {
            TrendzConfiguration result = doGetWithApiKey("/api/trendz/config", TrendzConfiguration.class);
            assertThat(result.trendzUrl()).isEqualTo(TRENDZ_URL);

            // Admitted by identity to this one endpoint, the key still reads no admin settings.
            doGetWithApiKey("/api/admin/settings/general").andExpect(status().isForbidden());
            doGetWithApiKey("/api/admin/settings/mail").andExpect(status().isForbidden());
        } finally {
            resetApiKey();
            apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, apiKey, true);
        }
    }

    @Test
    public void testGetTrendzConfig_asOtherInternalApiKey_forbidden() throws Exception {
        ApiKey apiKey = saveSysAdminInternalApiKey("Some other internal API key");
        resetTokens();
        setApiKey(apiKey.getValue());
        try {
            doGetWithApiKey("/api/trendz/config").andExpect(status().isForbidden());
        } finally {
            resetApiKey();
            apiKeyService.deleteApiKey(TenantId.SYS_TENANT_ID, apiKey, true);
        }
    }

    // The sysadmin part of what DefaultTrendzSyncService grants the Trendz key, minus ADMIN_SETTINGS.
    private ApiKey saveSysAdminInternalApiKey(String description) {
        AuthorityPermissionsInfo permissions = new AuthorityPermissionsInfo();
        permissions.setOperationsByResource(Map.of(Authority.SYS_ADMIN,
                Map.of(Resource.WIDGETS_BUNDLE, Set.of(Operation.READ))));
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(currentUserId);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setInternal(true);
        apiKeyInfo.setPermissions(permissions);
        return apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
    }

    @Test
    public void testSaveTrendzConfig_asSysAdmin() {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);

        TrendzConfiguration result = doPost("/api/trendz/config", config, TrendzConfiguration.class);

        assertThat(result).isNotNull();
        assertThat(result.trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(result.tbUrl()).isEqualTo(TB_URL);

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings();
        assertThat(savedSettings).isNotNull();
        assertThat(savedSettings.configuration().trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(savedSettings.configuration().tbUrl()).isEqualTo(TB_URL);
    }

    @Test
    public void testSaveTrendzConfig_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);

        doPost("/api/trendz/config", config).andExpect(status().isForbidden());
    }

    @Test
    public void testGetTrendzSync_asSysAdmin_whenNotExists_thenNull() throws Exception {
        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isNull();
        assertEquals(0, result.updatedTs());
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.NOT_AVAILABLE);
    }

    @Test
    public void testGetTrendzSync_asSysAdmin() throws Exception {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_COMPLETED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.SYNCED);
    }

    @Test
    public void testGetTrendzSync_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
    }

    @Test
    public void testPerformTrendzHealthcheck_asSysAdmin() throws Exception {
        TrendzHealthcheckResult healthcheckResult = new TrendzHealthcheckResult(
                TRENDZ_VERSION,
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED,
                "Healthcheck passed"
        );

        when(trendzSyncService.performHealthcheck()).thenReturn(healthcheckResult);

        TrendzHealthcheckResult result = doGet("/api/trendz/healthcheck", TrendzHealthcheckResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
        assertThat(result.message()).isEqualTo("Healthcheck passed");
    }

    @Test
    public void testPerformTrendzHealthcheck_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        TrendzHealthcheckResult healthcheckResult = new TrendzHealthcheckResult(
                TRENDZ_VERSION,
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED,
                "Healthcheck passed"
        );

        when(trendzSyncService.performHealthcheck()).thenReturn(healthcheckResult);

        TrendzHealthcheckResult result = doGet("/api/trendz/healthcheck", TrendzHealthcheckResult.class);

        assertThat(result).isNotNull();
    }

    @Test
    public void testConnectToTrendz_asSysAdmin() {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        when(trendzSyncService.performSync()).thenReturn(settings);

        TrendzSynchronizationResult result = doPost("/api/trendz/connect", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_COMPLETED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.SYNCED);
    }

    @Test
    public void testConnectToTrendz_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        doPost("/api/trendz/connect").andExpect(status().isForbidden());
    }

    @Test
    public void testPublicConnectToTrendz() throws Exception {
        logout();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        when(trendzSyncService.performSync())
                .thenReturn(settings);

        String result = doPost("/api/trendz/public/connect", String.class);
        assertTrue(result.isEmpty());
    }
}
