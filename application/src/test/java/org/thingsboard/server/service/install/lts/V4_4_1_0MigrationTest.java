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
import java.util.List;
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

    private static final List<Authority> AUTHORITIES = List.of(Authority.TENANT_ADMIN, Authority.CUSTOMER_USER);

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private V4_4_1_0Migration migration;

    @Test
    void grantsAiModelReadAndInvokeWithoutDroppingExistingOperations() {
        givenTrendzApiKey(byAuthority(legacyPermissions(), AUTHORITIES));

        migration.apply();

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = savedPermissions();
        for (Authority authority : AUTHORITIES) {
            assertEquals(Set.of(Operation.READ, Operation.INVOKE), byAuthority.get(authority).get(Resource.AI_MODEL),
                    "expected READ and INVOKE on AI_MODEL for " + authority);
        }
        assertEquals(Set.of(Operation.READ, Operation.READ_TELEMETRY),
                byAuthority.get(Authority.TENANT_ADMIN).get(Resource.DEVICE),
                "the operations the key already had must survive");
    }

    @Test
    void keepsOperationsTheKeyAlreadyHadOnAiModel() {
        Map<Resource, Set<Operation>> permissions = legacyPermissions();
        permissions.put(Resource.AI_MODEL, Set.of(Operation.READ, Operation.WRITE));
        givenTrendzApiKey(byAuthority(permissions, AUTHORITIES));

        migration.apply();

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = savedPermissions();
        for (Authority authority : AUTHORITIES) {
            assertEquals(Set.of(Operation.READ, Operation.WRITE, Operation.INVOKE), byAuthority.get(authority).get(Resource.AI_MODEL),
                    "expected the existing AI_MODEL operations plus INVOKE for " + authority);
        }
    }

    /**
     * {@link Migration#apply()} re-runs when a node crashes before the version is recorded, so a key that already
     * carries the operations must not be written again.
     */
    @Test
    void alreadyGrantedKeyIsNotSavedAgain() {
        Map<Resource, Set<Operation>> permissions = legacyPermissions();
        permissions.put(Resource.AI_MODEL, Set.of(Operation.READ, Operation.INVOKE));
        givenTrendzApiKey(byAuthority(permissions, AUTHORITIES));

        migration.apply();

        verify(apiKeyService, never()).saveApiKey(any(), any());
    }

    @Test
    void authorityTheKeyDoesNotCoverIsNotAdded() {
        givenTrendzApiKey(byAuthority(legacyPermissions(), List.of(Authority.TENANT_ADMIN)));

        migration.apply();

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = savedPermissions();
        assertEquals(Set.of(Operation.READ, Operation.INVOKE), byAuthority.get(Authority.TENANT_ADMIN).get(Resource.AI_MODEL));
        assertFalse(byAuthority.containsKey(Authority.CUSTOMER_USER), "an authority the key never covered must stay absent");
    }

    @Test
    void missingTrendzApiKeyIsNoOp() {
        when(apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION))
                .thenReturn(null);

        migration.apply();

        verify(apiKeyService, never()).saveApiKey(any(), any());
    }

    private Map<Authority, Map<Resource, Set<Operation>>> savedPermissions() {
        ArgumentCaptor<ApiKeyInfo> saved = ArgumentCaptor.forClass(ApiKeyInfo.class);
        verify(apiKeyService).saveApiKey(eq(TenantId.SYS_TENANT_ID), saved.capture());
        return saved.getValue().getPermissions().getOperationsByResource();
    }

    private void givenTrendzApiKey(Map<Authority, Map<Resource, Set<Operation>>> byAuthority) {
        ApiKey apiKey = new ApiKey(new ApiKeyId(UUID.randomUUID()));
        apiKey.setTenantId(TenantId.SYS_TENANT_ID);
        apiKey.setInternal(true);
        apiKey.setDescription(TRENDZ_API_KEY_DESCRIPTION);

        AuthorityPermissionsInfo permissions = new AuthorityPermissionsInfo();
        permissions.setOperationsByResource(byAuthority);
        apiKey.setPermissions(permissions);

        when(apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION))
                .thenReturn(apiKey);
    }

    // Separate map instances per authority, the way a key deserialized from the permissions column arrives --
    // buildTrendzPermissions() shares one instance, but that sharing does not survive the JSON round trip.
    private Map<Authority, Map<Resource, Set<Operation>>> byAuthority(Map<Resource, Set<Operation>> permissions, List<Authority> authorities) {
        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = new HashMap<>();
        authorities.forEach(authority -> byAuthority.put(authority, new HashMap<>(permissions)));
        return byAuthority;
    }

    // What a key created before 4.4.1.0 holds. Immutable inner sets on purpose: the migration must copy what it reads
    // rather than mutate it.
    private Map<Resource, Set<Operation>> legacyPermissions() {
        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.READ_TELEMETRY));
        permissions.put(Resource.ASSET, Set.of(Operation.READ, Operation.READ_TELEMETRY));
        permissions.put(Resource.DEVICE_PROFILE, Set.of(Operation.READ));
        return permissions;
    }
}
