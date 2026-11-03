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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.dao.trendz.TrendzSyncService.TRENDZ_API_KEY_DESCRIPTION;

/**
 * Migration for version {@code 4.4.1.0}.
 * <p>
 * {@link MigrationService} selects migrations from the injected {@link Migration} beans, not from the
 * on-disk {@code data/upgrade/lts/<version>/} directories. So this bean is what makes the runner discover
 * version {@code 4.4.1.0} and execute its {@code data/upgrade/lts/4.4.1.0/schema_update.sql}, which adds the
 * {@code idx_calculated_field_tenant_id_id} index used by tenant-scoped calculated field paging.
 * <p>
 * It needs a slot of its own rather than {@code 4.4.0.0}: that version is already released, and
 * {@link Version#isInRange} is exclusive at the lower bound, so an installation already on {@code 4.4.0.0}
 * would never pick up a change added there.
 * <p>
 * The dir/bean consistency (both ways) is guarded by a test in {@code MigrationIntegrationTest}.
 * <p>
 * Each thing this version migrates lives in its own private method called from {@link #apply()}, as in
 * {@link V4_3_1_5Migration}.
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
        revokeAdminSettingsFromTrendzApiKey();
    }

    /**
     * Takes {@link Resource#ADMIN_SETTINGS} away from the internal Trendz API key.
     * <p>
     * Read access let the key fetch the JWT signing key, and with it mint a token for any user, sysadmin included;
     * write access let it replace that key. The one settings call Trendz makes, {@code GET /api/trendz/config}, admits
     * the key by identity now, and new keys no longer get the resource from
     * {@code DefaultTrendzSyncService.buildTrendzPermissions()} -- keep the two in step.
     * <p>
     * An authority left with no permissions at all is not stripped: an empty permission set makes the key fall back
     * to the user's full permissions, which would widen the grant instead of narrowing it.
     * <p>
     * Idempotent: the key is saved only when something actually changed, so a re-run is a no-op.
     */
    private void revokeAdminSettingsFromTrendzApiKey() {
        ApiKey trendzApiKey = apiKeyService.findInternalApiKeyByDescription(TenantId.SYS_TENANT_ID, TRENDZ_API_KEY_DESCRIPTION);
        if (trendzApiKey == null || trendzApiKey.getPermissions() == null
            || trendzApiKey.getPermissions().getOperationsByResource() == null) {
            log.debug("No internal Trendz API key with stored permissions, nothing to revoke");
            return;
        }

        Map<Authority, Map<Resource, Set<Operation>>> byAuthority =
                new HashMap<>(trendzApiKey.getPermissions().getOperationsByResource());
        boolean changed = false;

        for (Map.Entry<Authority, Map<Resource, Set<Operation>>> entry : byAuthority.entrySet()) {
            Map<Resource, Set<Operation>> byResource = entry.getValue();
            if (byResource == null || !byResource.containsKey(Resource.ADMIN_SETTINGS)) {
                continue;
            }
            // Copy rather than mutate in place: the deserialized maps carry no mutability guarantee.
            Map<Resource, Set<Operation>> updatedByResource = new HashMap<>(byResource);
            updatedByResource.remove(Resource.ADMIN_SETTINGS);
            if (updatedByResource.isEmpty()) {
                log.warn("Internal Trendz API key holds nothing but {} for {}, left as is", Resource.ADMIN_SETTINGS, entry.getKey());
                continue;
            }
            entry.setValue(updatedByResource);
            changed = true;
        }

        if (!changed) {
            log.debug("Internal Trendz API key already holds no {}", Resource.ADMIN_SETTINGS);
            return;
        }

        trendzApiKey.getPermissions().setOperationsByResource(byAuthority);
        // Preserves the key's value, so Trendz keeps authenticating with the secret it already holds, and evicts the
        // key from the cache, which would otherwise keep serving the old permissions for the rest of its TTL.
        apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, trendzApiKey);
        log.info("Revoked {} from the internal Trendz API key", Resource.ADMIN_SETTINGS);
    }
}
