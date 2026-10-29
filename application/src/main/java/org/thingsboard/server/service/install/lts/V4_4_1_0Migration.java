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
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

/**
 * Migration for version {@code 4.4.1.0}.
 * <p>
 * {@link MigrationService} selects migrations from the injected {@link Migration} beans, not from the on-disk
 * {@code data/upgrade/lts/<version>/} directories, so this bean is what makes the runner discover version
 * {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}.
 * <p>
 * It needs a slot of its own rather than {@code 4.4.0.0}: that version is already released, and
 * {@link Version#isInRange} is exclusive at the lower bound, so an installation already on {@code 4.4.0.0}
 * would never pick up a change added there.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code MigrationIntegrationTest}.
 * <p>
 * Each thing this version migrates lives in its own private method called from {@link #apply()}, with the constants
 * it needs declared inside it, so a further change means one more method and one more line in {@code apply()}.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class V4_4_1_0Migration implements Migration {

    private final ApiKeyService apiKeyService;

    @Override
    public String getVersion() {
        return "4.4.1.0";
    }

    /**
     * Runs in the same transaction as the version's schema SQL on the no-downtime path, and outside any transaction
     * on the offline path (see {@link Migration#apply}). Every step below must therefore be light enough to sit in
     * a schema transaction, and idempotent, since a crash before the version is recorded re-runs it.
     */
    @Override
    public void apply() {
        grantAiModelOperationsToTrendzApiKey();
    }

    /**
     * Grants operations on {@link Resource#AI_MODEL} to the internal Trendz API key: {@link Operation#READ} and
     * {@link Operation#INVOKE} for tenant administrators, only {@link Operation#INVOKE} for customer users.
     * <p>
     * Trendz lists AI models for tenant administrators and sends chat requests to them for both authorities through that key, but keys minted before this version were
     * created from a {@code buildTrendzPermissions()} that did not include the resource, so every AI model endpoint
     * answered 403. New keys get it from {@code DefaultTrendzSyncService.buildTrendzPermissions()}, which is the source
     * of truth this method mirrors -- keep the two in step.
     * <p>
     * Expressed against the {@link Resource} and {@link Operation} enums rather than as SQL over the permissions JSON,
     * so renaming an enum constant breaks the build instead of silently matching nothing.
     * <p>
     * Idempotent: an operation already present is never added twice, and the key is saved only when something actually
     * changed, so a re-run is a no-op.
     */
    private void grantAiModelOperationsToTrendzApiKey() {
        // SYS_ADMIN is deliberately absent: AI models are tenant entities.
        Map<Authority, Set<Operation>> aiModelOperationsByAuthority = Map.of(
                Authority.TENANT_ADMIN, EnumSet.of(Operation.READ, Operation.INVOKE),
                Authority.CUSTOMER_USER, EnumSet.of(Operation.INVOKE));

        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || trendzApiKey.getPermissions() == null
            || trendzApiKey.getPermissions().getOperationsByResource() == null) {
            log.debug("No internal Trendz API key with stored permissions, nothing to grant");
            return;
        }

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                new HashMap<>(trendzApiKey.getPermissions().getOperationsByResource());
        boolean changed = false;

        for (var entry : aiModelOperationsByAuthority.entrySet()) {
            Authority authority = entry.getKey();
            Set<Operation> aiModelOperations = entry.getValue();
            Map<Resource, Set<Operation>> byResource = byAuthority.get(authority);
            if (byResource == null) {
                continue;
            }
            // Copy rather than mutate in place: the deserialized maps and sets carry no mutability guarantee.
            Map<Resource, Set<Operation>> updatedByResource = new HashMap<>(byResource);
            Set<Operation> operations = updatedByResource.get(Resource.AI_MODEL);
            if (operations != null && operations.containsAll(aiModelOperations)) {
                continue;
            }
            // An authority the key covers gets the resource even if it was never granted: AI models are new to the key.
            Set<Operation> updated = EnumSet.copyOf(aiModelOperations);
            if (operations != null) {
                updated.addAll(operations);
            }
            updatedByResource.put(Resource.AI_MODEL, updated);
            changed = true;
            byAuthority.put(authority, updatedByResource);
        }

        if (!changed) {
            log.debug("Internal Trendz API key already grants {} on {}", aiModelOperationsByAuthority, Resource.AI_MODEL);
            return;
        }

        trendzApiKey.getPermissions().setOperationsByResource(byAuthority);
        // Preserves the key's value, so Trendz keeps authenticating with the secret it already holds, and evicts the
        // key from the cache, which would otherwise keep serving the old permissions for the rest of its TTL.
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, trendzApiKey);
        log.info("Granted {} on {} to the internal Trendz API key", aiModelOperationsByAuthority, Resource.AI_MODEL);
    }
}
