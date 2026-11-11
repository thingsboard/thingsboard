// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface SchedulerEventProcessor extends EdgeProcessor {

    ListenableFuture<Void> processSchedulerEventMsgFromEdge(TenantId tenantId, Edge edge, SchedulerEventUpdateMsg schedulerEventUpdateMsg);
}
