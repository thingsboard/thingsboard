// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ExtensionDao extends TenantEntityDao<Extension>, ExportableEntityDao<ExtensionId, Extension> {

    Extension findByTenantIdAndId(TenantId tenantId, ExtensionId extensionId);

    Extension findBySlug(String slug);

    /** Bulk read by primary key, not tenant-scoped; see ExtensionRepository.findByIdIn. */
    List<Extension> findByIds(Collection<UUID> ids);

    PageData<ExtensionInfo> findInfosByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Extension> findAll(PageLink pageLink);

    boolean deleteByTenantIdAndId(TenantId tenantId, ExtensionId extensionId);

    int deleteByTenantIdAndIds(TenantId tenantId, Set<UUID> ids);

    // Compare-and-set on state: applies the write in one atomic statement, only while the row still
    // holds expectedState. Returns false when a concurrent transition (a user start/stop, another
    // node's reconcile) already moved it, so a decision made on a stale snapshot is dropped.
    boolean updateStateIf(TenantId tenantId, ExtensionId extensionId, ExtensionState expectedState,
                          ExtensionState newState, String lastError, Long lastErrorAt);

}
