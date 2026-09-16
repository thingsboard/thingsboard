// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.cf;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface CalculatedFieldProcessor extends EdgeProcessor {

    ListenableFuture<Void> processCalculatedFieldMsgFromEdge(TenantId tenantId, Edge edge, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg);

}
