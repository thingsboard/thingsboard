// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.report;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface ReportTemplateProcessor extends EdgeProcessor {

    ListenableFuture<Void> processReportTemplateMsgFromEdge(TenantId tenantId, Edge edge, ReportTemplateUpdateMsg reportTemplateUpdateMsg);

}
