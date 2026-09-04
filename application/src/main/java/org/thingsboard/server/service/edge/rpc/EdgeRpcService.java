// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;

import java.util.function.Consumer;

public interface EdgeRpcService {

    void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg);

    void updateEdge(TenantId tenantId, Edge edge);

    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    void processSyncRequest(TenantId tenantId, EdgeId edgeId, Consumer<FromEdgeSyncResponse> responseConsumer);

}
