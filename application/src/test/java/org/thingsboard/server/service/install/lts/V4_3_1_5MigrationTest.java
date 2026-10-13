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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

@ExtendWith(MockitoExtension.class)
class V4_3_1_5MigrationTest {

    private static final List<Resource> CF_RESOURCES = List.of(
            Resource.DEVICE, Resource.DEVICE_PROFILE, Resource.ASSET, Resource.ASSET_PROFILE, Resource.CUSTOMER);

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private V4_3_1_5Migration migration;

    @Test
    void grantsCalculatedFieldReadWithoutDroppingExistingOperations() {
        givenTrendzApiKey(legacyPermissions());

        migration.apply();

        ArgumentCaptor<ApiKeyInfo> saved = ArgumentCaptor.forClass(ApiKeyInfo.class);
        verify(apiKeyService).saveApiKey(eq(TenantId.SYS_TENANT_ID), saved.capture());
        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                saved.getValue().getPermissions().getOperationsByResource();

        for (Authority authority : List.of(Authority.TENANT_ADMIN, Authority.CUSTOMER_USER)) {
            for (Resource resource : CF_RESOURCES) {
                assertTrue(byAuthority.get(authority).get(resource).contains(Operation.READ_CALCULATED_FIELD),
                        "expected READ_CALCULATED_FIELD for " + authority + " on " + resource);
            }
        }
        assertEquals(Set.of(Operation.READ, Operation.READ_TELEMETRY, Operation.READ_CALCULATED_FIELD),
                byAuthority.get(Authority.TENANT_ADMIN).get(Resource.DEVICE),
                "the operations the key already had must survive");
    }

    /**
     * {@link LtsMigration#apply()} re-runs when a node crashes before the version is recorded, so a key that already
     * carries the operation must not be written again.
     */
    @Test
    void alreadyGrantedKeyIsNotSavedAgain() {
        Map<Resource, Set<Operation>> permissions = legacyPermissions();
        CF_RESOURCES.forEach(resource -> {
            Set<Operation> granted = new HashSet<>(permissions.get(resource));
            granted.add(Operation.READ_CALCULATED_FIELD);
            permissions.put(resource, granted);
        });
        givenTrendzApiKey(permissions);

        migration.apply();

        verify(apiKeyService, never()).saveApiKey(any(), any());
    }

    private void givenTrendzApiKey(Map<Resource, Set<Operation>> tenantCustomerPermissions) {
        ApiKey apiKey = new ApiKey(new ApiKeyId(UUID.randomUUID()));
        apiKey.setTenantId(TenantId.SYS_TENANT_ID);
        apiKey.setInternal(true);
        apiKey.setDescription(TRENDZ_API_KEY_DESCRIPTION);

        // Separate map instances per authority, the way a key deserialized from the permissions column arrives --
        // buildTrendzPermissions() shares one instance, but that sharing does not survive the JSON round trip.
        Map<Authority, Map<Resource, Set<Operation>>> byAuthority = new HashMap<>();
        byAuthority.put(Authority.TENANT_ADMIN, new HashMap<>(tenantCustomerPermissions));
        byAuthority.put(Authority.CUSTOMER_USER, new HashMap<>(tenantCustomerPermissions));

        AuthorityPermissionsInfo permissions = new AuthorityPermissionsInfo();
        permissions.setOperationsByResource(byAuthority);
        apiKey.setPermissions(permissions);

        when(apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION))
                .thenReturn(apiKey);
    }

    // What a key created before 4.3.1.5 holds on the calculated-field resources. Immutable inner sets on purpose:
    // the migration must copy what it reads rather than mutate it.
    private Map<Resource, Set<Operation>> legacyPermissions() {
        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.READ_TELEMETRY));
        permissions.put(Resource.ASSET, Set.of(Operation.READ, Operation.READ_TELEMETRY));
        permissions.put(Resource.CUSTOMER, Set.of(Operation.READ, Operation.READ_TELEMETRY));
        permissions.put(Resource.DEVICE_PROFILE, Set.of(Operation.READ));
        permissions.put(Resource.ASSET_PROFILE, Set.of(Operation.READ));
        return permissions;
    }
}
