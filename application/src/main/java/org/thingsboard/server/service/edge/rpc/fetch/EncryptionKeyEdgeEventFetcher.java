// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class EncryptionKeyEdgeEventFetcher extends BasePageableEdgeEventFetcher<EncryptionKey> {

    private final EncryptionService encryptionService;

    @Override
    PageData<EncryptionKey> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        EncryptionKey key = encryptionService.findByTenantId(tenantId);
        return new PageData<>(List.of(key), 1, 1, false);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, EncryptionKey entity) {
        return EdgeUtils.constructEdgeEvent(
                tenantId,
                edge.getId(),
                EdgeEventType.ENCRYPTION_KEY,
                EdgeEventActionType.ADDED,
                null,
                null
        );
    }

}
