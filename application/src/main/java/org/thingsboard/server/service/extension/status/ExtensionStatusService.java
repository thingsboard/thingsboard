// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionStatus;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ExtensionStatusService {

    ExtensionStatus describe(TenantId tenantId, Extension extension);

    /**
     * Snapshots for a whole page, keyed by extension id. Cached entries are served as-is; the rest are
     * resolved in one batched substrate call instead of one {@link #describe} per row, so a list request
     * no longer costs two orchestrator round-trips per extension. A row the substrate cannot answer for
     * is absent from the result, leaving the caller to fall back to what the DB row already says.
     */
    Map<ExtensionId, ExtensionStatus> describeAll(TenantId tenantId, List<? extends Extension> extensions);

    String tailLogs(TenantId tenantId, Extension extension, int lines);

    void register(Extension extension);

    void unregister(ExtensionId extensionId);

    void retainRegistered(Set<ExtensionId> ownedIds);

    /**
     * Ids currently registered for status polling on this node. Lets a caller reason about what it may
     * prune instead of pruning against a snapshot it took before other threads registered anything.
     */
    Set<ExtensionId> registeredIds();

}
