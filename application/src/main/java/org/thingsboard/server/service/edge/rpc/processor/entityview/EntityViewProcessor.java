// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface EntityViewProcessor extends EdgeProcessor {

    ListenableFuture<Void> processEntityViewMsgFromEdge(TenantId tenantId, Edge edge, EntityViewUpdateMsg entityViewUpdateMsg);

}
