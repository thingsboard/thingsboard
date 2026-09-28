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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;

@AllArgsConstructor
@Slf4j
public class SecretEdgeEventFetcher extends BasePageableEdgeEventFetcher<Secret> {

    private final SecretService secretService;

    @Override
    PageData<Secret> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return secretService.findSecretsByTenantId(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Secret entity) {
        return EdgeUtils.constructEdgeEvent(
                tenantId,
                edge.getId(),
                EdgeEventType.SECRET,
                EdgeEventActionType.ADDED,
                entity.getId(),
                null
        );
    }

}
