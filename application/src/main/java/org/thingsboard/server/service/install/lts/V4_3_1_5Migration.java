// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

/**
 * LTS migration for version {@code 4.3.1.5}.
 * <p>
 * {@link LtsMigrationService} selects migrations from the injected {@link LtsMigration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories, so this bean is what makes the runner discover version
 * {@code 4.3.1.5} at all.
 * <p>
 * There is no {@code data/upgrade/lts/4.3.1.5/} directory: everything this version changes is programmatic, so the
 * version is listed in {@code SQL_LESS_ALLOWED} in {@code LtsMigrationIntegrationTest}, which otherwise fails a bean
 * that has no matching directory. Should this version later need DDL, add the directory and drop that entry.
 * <p>
 * Each thing this version migrates lives in its own private method called from {@link #apply()}, with the constants
 * it needs declared inside it, so a further change means one more method and one more line in {@code apply()}.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_3_1_5Migration implements LtsMigration {

    private final ApiKeyService apiKeyService;

    @Override
    public String getVersion() {
        return "4.3.1.5";
    }

    /**
     * Runs in the same transaction as the version's schema SQL on the no-downtime path, and outside any transaction
     * on the offline path (see {@link LtsMigration#apply}). Every step below must therefore be light enough to sit in
     * a schema transaction, and idempotent, since a crash before the version is recorded re-runs it.
     */
    @Override
    public void apply() {
        grantCalculatedFieldReadToTrendzApiKey();
    }

    /**
     * Grants {@link Operation#READ_CALCULATED_FIELD} to the internal Trendz API key.
     * <p>
     * Trendz reads calculated fields through that key, but keys minted before this version were created from a
     * {@code buildTrendzPermissions()} that did not include the operation, so every calculated field endpoint answered
     * 403. New keys get it from {@code DefaultTrendzSyncService.buildTrendzPermissions()}, which is the source of
     * truth this method mirrors -- keep the two in step.
     * <p>
     * Expressed against the {@link Resource} and {@link Operation} enums rather than as SQL over the permissions JSON,
     * so renaming an enum constant breaks the build instead of silently matching nothing.
     * <p>
     * Idempotent: an operation already present is never added twice, and the key is saved only when something actually
     * changed, so a re-run is a no-op.
     */
    private void grantCalculatedFieldReadToTrendzApiKey() {
        // The resources Trendz already reads that can carry calculated fields.
        List<Resource> calculatedFieldResources = List.of(
                Resource.DEVICE, Resource.DEVICE_PROFILE, Resource.ASSET, Resource.ASSET_PROFILE, Resource.CUSTOMER);
        // SYS_ADMIN is deliberately absent: none of the resources above appear in the key's SYS_ADMIN permissions.
        List<Authority> authorities = List.of(Authority.TENANT_ADMIN, Authority.CUSTOMER_USER);

        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || trendzApiKey.getPermissions() == null
            || trendzApiKey.getPermissions().getOperationsByResource() == null) {
            log.debug("No internal Trendz API key with stored permissions, nothing to grant");
            return;
        }

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                new HashMap<>(trendzApiKey.getPermissions().getOperationsByResource());
        boolean changed = false;

        for (Authority authority : authorities) {
            Map<Resource, Set<Operation>> byResource = byAuthority.get(authority);
            if (byResource == null) {
                continue;
            }
            // Copy rather than mutate in place: the deserialized maps and sets carry no mutability guarantee.
            Map<Resource, Set<Operation>> updatedByResource = new HashMap<>(byResource);
            for (Resource resource : calculatedFieldResources) {
                Set<Operation> operations = updatedByResource.get(resource);
                // A resource the key was never granted at all stays ungranted: this widens an existing grant,
                // it does not invent one.
                if (operations == null || operations.contains(Operation.READ_CALCULATED_FIELD)) {
                    continue;
                }
                Set<Operation> updated = EnumSet.of(Operation.READ_CALCULATED_FIELD);
                updated.addAll(operations);
                updatedByResource.put(resource, updated);
                changed = true;
            }
            byAuthority.put(authority, updatedByResource);
        }

        if (!changed) {
            log.debug("Internal Trendz API key already grants {}", Operation.READ_CALCULATED_FIELD);
            return;
        }

        trendzApiKey.getPermissions().setOperationsByResource(byAuthority);
        // Preserves the key's value, so Trendz keeps authenticating with the secret it already holds, and evicts the
        // key from the cache, which would otherwise keep serving the old permissions for the rest of its TTL.
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, trendzApiKey);
        log.info("Granted {} on {} to the internal Trendz API key", Operation.READ_CALCULATED_FIELD, calculatedFieldResources);
    }
}
