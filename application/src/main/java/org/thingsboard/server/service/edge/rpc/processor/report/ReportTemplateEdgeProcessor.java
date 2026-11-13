// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.report;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ReportTemplateEdgeProcessor extends BaseReportTemplateProcessor implements ReportTemplateProcessor {

    @Override
    public ListenableFuture<Void> processReportTemplateMsgFromEdge(TenantId tenantId, Edge edge, ReportTemplateUpdateMsg reportTemplateUpdateMsg) {
        log.trace("[{}] executing processReportTemplateMsgFromEdge [{}] from edge [{}]", tenantId, reportTemplateUpdateMsg, edge.getId());
        ReportTemplateId reportTemplateId = new ReportTemplateId(new UUID(reportTemplateUpdateMsg.getIdMSB(), reportTemplateUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (reportTemplateUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateReportTemplate(tenantId, reportTemplateId, reportTemplateUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteReportTemplate(tenantId, reportTemplateId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(reportTemplateUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process ReportTemplateUpdateMsg from Edge [{}]", tenantId, reportTemplateUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId, ReportTemplateUpdateMsg reportTemplateUpdateMsg, Edge edge) {
        Boolean created = super.saveOrUpdateReportTemplate(tenantId, reportTemplateId, reportTemplateUpdateMsg);
        if (created) {
            pushReportTemplateCreatedEventToRuleEngine(tenantId, edge, reportTemplateId);
        }
    }

    private void pushReportTemplateCreatedEventToRuleEngine(TenantId tenantId, Edge edge, ReportTemplateId reportTemplateId) {
        ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(tenantId, reportTemplateId);
        pushReportTemplateEventToRuleEngine(tenantId, edge, reportTemplate, TbMsgType.ENTITY_CREATED);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        ReportTemplateId reportTemplateId = new ReportTemplateId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(edgeEvent.getTenantId(), reportTemplateId);
                if (reportTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateUpdatedMsg(msgType, reportTemplate);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addReportTemplateUpdateMsg(reportTemplateUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateDeleteMsg(reportTemplateId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addReportTemplateUpdateMsg(reportTemplateUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.REPORT_TEMPLATE;
    }

}
