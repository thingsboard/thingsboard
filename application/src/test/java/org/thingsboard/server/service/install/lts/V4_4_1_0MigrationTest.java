// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.pat.ApiKeyService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

@ExtendWith(MockitoExtension.class)
class V4_4_1_0MigrationTest {

    private static final Set<Operation> WIDGET_OPERATIONS = Set.of(Operation.READ, Operation.CREATE, Operation.WRITE, Operation.DELETE);

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private V4_4_1_0Migration migration;

    @Test
    void revokesAdminSettingsAndKeepsTheRest() {
        Map<Resource, Set<Operation>> sysAdminPermissions = new HashMap<>();
        sysAdminPermissions.put(Resource.ADMIN_SETTINGS, Set.of(Operation.READ, Operation.WRITE));
        sysAdminPermissions.put(Resource.WIDGETS_BUNDLE, WIDGET_OPERATIONS);
        givenTrendzApiKey(sysAdminPermissions);

        migration.apply();

        ArgumentCaptor<ApiKeyInfo> saved = ArgumentCaptor.forClass(ApiKeyInfo.class);
        verify(apiKeyService).saveApiKey(eq(TenantId.SYS_TENANT_ID), saved.capture());
        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                saved.getValue().getPermissions().getOperationsByResource();

        assertFalse(byAuthority.get(Authority.SYS_ADMIN).containsKey(Resource.ADMIN_SETTINGS));
        assertEquals(WIDGET_OPERATIONS, byAuthority.get(Authority.SYS_ADMIN).get(Resource.WIDGETS_BUNDLE),
                "the other sysadmin permissions must survive");
        assertEquals(Set.of(Operation.READ), byAuthority.get(Authority.TENANT_ADMIN).get(Resource.DEVICE),
                "other authorities must be left alone");
    }

    /**
     * {@link Migration#apply()} re-runs when a node crashes before the version is recorded, so a key that already
     * lacks the resource must not be written again.
     */
    @Test
    void alreadyRevokedKeyIsNotSavedAgain() {
        givenTrendzApiKey(Map.of(Resource.WIDGETS_BUNDLE, WIDGET_OPERATIONS));

        migration.apply();

        verify(apiKeyService, never()).saveApiKey(any(), any());
    }

    /**
     * An empty permission set for an authority makes the key fall back to the user's full permissions, so stripping
     * the last resource would widen the key rather than narrow it.
     */
    @Test
    void lastSysAdminPermissionIsNotStripped() {
        givenTrendzApiKey(Map.of(Resource.ADMIN_SETTINGS, Set.of(Operation.READ, Operation.WRITE)));

        migration.apply();

        verify(apiKeyService, never()).saveApiKey(any(), any());
    }

    private void givenTrendzApiKey(Map<Resource, Set<Operation>> sysAdminPermissions) {
        ApiKey apiKey = new ApiKey(new ApiKeyId(UUID.randomUUID()));
        apiKey.setTenantId(TenantId.SYS_TENANT_ID);
        apiKey.setInternal(true);
        apiKey.setDescription(TRENDZ_API_KEY_DESCRIPTION);

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = new HashMap<>();
        byAuthority.put(Authority.SYS_ADMIN, Map.copyOf(sysAdminPermissions));
        byAuthority.put(Authority.TENANT_ADMIN, Map.of(Resource.DEVICE, Set.of(Operation.READ)));

        AuthorityPermissionsInfo permissions = new AuthorityPermissionsInfo();
        permissions.setOperationsByResource(byAuthority);
        apiKey.setPermissions(permissions);

        when(apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION))
                .thenReturn(apiKey);
    }
}
