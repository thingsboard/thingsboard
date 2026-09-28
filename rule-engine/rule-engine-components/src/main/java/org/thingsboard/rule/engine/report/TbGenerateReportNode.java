// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.report;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.util.UUID;

@RuleNode(
        type = ComponentType.ACTION,
        name = "generate dashboard report",
        configClazz = TbGenerateReportNodeConfiguration.class,
        nodeDescription = "Generates dashboard report",
        nodeDetails = "Generates dashboard based reports.",
        configDirective = "tbActionNodeGenerateDashboardReportConfig",
        icon = "description",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/generate-dashboard-report/"
)
public class TbGenerateReportNode extends TbAbstractExternalNode {

    private static final String ATTACHMENTS = "attachments";

    private TbGenerateReportNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbGenerateReportNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        DashboardReportConfig reportConfig;
        try {
            if (this.config.isUseReportConfigFromMessage()) {
                try {
                    JsonNode msgJson = JacksonUtil.toJsonNode(msg.getData());
                    JsonNode reportConfigJson = msgJson.get("reportConfig");
                    reportConfig = JacksonUtil.treeToValue(reportConfigJson, DashboardReportConfig.class);
                } catch (Exception e) {
                    throw new RuntimeException("Incoming message doesn't contain valid reportConfig JSON configuration!", e);
                }
            } else {
                reportConfig = this.config.getReportConfig();
            }
            String reportsServerEndpointUrl = null;
            if (!this.config.isUseSystemReportsServer()) {
                reportsServerEndpointUrl = this.config.getReportsServerEndpointUrl();
            }

            var tbMsg = ackIfNeeded(ctx, msg);

            ctx.getPeContext().getDashboardReportService().generateReport(
                    ctx.getTenantId(),
                    reportConfig,
                    reportsServerEndpointUrl,
                    reportData -> {
                        User user = ctx.getUserService().findUserById(ctx.getTenantId(), new UserId(UUID.fromString(reportConfig.getUserId())));
                        BlobEntity reportBlobEntity = new BlobEntity();
                        reportBlobEntity.setData(ByteBuffer.wrap(reportData.getData()));
                        reportBlobEntity.setContentType(reportData.getContentType());
                        reportBlobEntity.setName(reportData.getName());
                        reportBlobEntity.setType("report");
                        reportBlobEntity.setTenantId(user.getTenantId());
                        reportBlobEntity.setCustomerId(user.getCustomerId());
                        reportBlobEntity = ctx.getPeContext().getBlobEntityService().saveBlobEntity(reportBlobEntity);
                        TbMsgMetaData metaData = tbMsg.getMetaData().copy();
                        String attachments = metaData.getValue(ATTACHMENTS);
                        if (!StringUtils.isEmpty(attachments)) {
                            attachments += "," + reportBlobEntity.getId().toString();
                        } else {
                            attachments = reportBlobEntity.getId().toString();
                        }
                        metaData.putValue(ATTACHMENTS, attachments);
                        TbMsg newMsg = tbMsg.transform()
                                .metaData(metaData)
                                .build();
                        tellSuccess(ctx, newMsg);
                    },
                    throwable -> tellFailure(ctx, tbMsg, throwable)
            );
        } catch (Throwable t) {
            ctx.tellFailure(msg, t);
        }
    }

}
