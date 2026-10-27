// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionQuota;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;

public interface ExtensionService extends EntityDaoService {

    Extension saveExtension(Extension extension);

    Extension findExtensionById(TenantId tenantId, ExtensionId extensionId);

    /**
     * Global, non-tenant-scoped lookup by the immutable routing slug. Used only by gateway routing/auth
     * (e.g. the /api/ext-auth check) to resolve a slug to its owning extension and tenant; must never be
     * exposed as a tenant-scoped read.
     */
    Extension findBySlug(String slug);

    /**
     * Tenant-scoped lookup by the human-readable name, which is unique within a tenant
     * ({@code extension_tenant_name_unq_key}). Unlike {@link #findBySlug(String)} this one is safe to
     * expose to tenant-scoped reads: it can only ever resolve inside the caller's own tenant.
     */
    Extension findByTenantIdAndName(TenantId tenantId, String name);

    /**
     * Bulk read by id, used by the status collector: one query per poll pass instead of a
     * {@link #findExtensionById(TenantId, ExtensionId)} per registered extension. Deliberately NOT
     * tenant-scoped — the caller passes ids it already owns and must verify the tenant of every row it
     * gets back, exactly like {@link #findBySlug(String)}. Never expose it to a tenant-scoped read.
     */
    List<Extension> findExtensionsByIds(Collection<ExtensionId> extensionIds);

    PageData<ExtensionInfo> findExtensionInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Extension> findAllExtensions(PageLink pageLink);

    void deleteExtension(TenantId tenantId, ExtensionId extensionId);

    void deleteExtensionsByTenantId(TenantId tenantId);

    void updateState(TenantId tenantId, ExtensionId extensionId, ExtensionState state, String lastError);

    // Resuming a STOPPED extension consumes quota again, so the capacity check and the flip out of STOPPED
    // must be one guarded unit: checked separately, two concurrent starts both pass on the same headroom.
    void startWithinQuota(TenantId tenantId, Extension extension);

    // Conditional variant of updateState for callers that decided on a snapshot taken before a slow
    // orchestrator call (the status poller, the reconciler's failure path). Writes only while the row
    // still holds expectedState, so a user start/stop committed inside that window is not silently
    // overwritten; returns false when the decision was stale and nothing was written.
    boolean updateStateIf(TenantId tenantId, ExtensionId extensionId, ExtensionState expectedState,
                          ExtensionState newState, String lastError);

    /**
    /**
     * Reports the effective extension limits of the tenant together with the used and the still free
     * part, including how many more extensions of every resource tier would fit. Read-only view over
     * the same numbers the quota checks use.
     */
    ExtensionQuota getQuota(TenantId tenantId);

}
