// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface AlarmProcessor extends EdgeProcessor {

    ListenableFuture<Void> processAlarmMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmUpdateMsg alarmUpdateMsg);

}
