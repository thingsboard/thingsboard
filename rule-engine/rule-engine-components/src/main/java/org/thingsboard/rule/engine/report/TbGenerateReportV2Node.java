// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.report;

import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Base64;

@RuleNode(
        type = ComponentType.ACTION,
        name = "generate report",
        configClazz = TbGenerateReportV2NodeConfiguration.class,
        nodeDescription = "Generates report",
        nodeDetails = "Generates report, creating a \"Report generation\" task in the task manager. The output metadata of the node contains \"reports\" field with the generated report id.",
        configDirective = "tbActionNodeGenerateReportConfig",
        icon = "description",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/generate-report/"
)
public class TbGenerateReportV2Node extends TbAbstractExternalNode {

    private TbGenerateReportV2NodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbGenerateReportV2NodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg tbMsg) {
        TenantId tenantId = ctx.getTenantId();
        ReportConfig reportConfig;
        if (config.isUseConfigFromMessage()) {
            reportConfig = JacksonUtil.fromString(tbMsg.getData(), ReportConfig.class);
        } else {
            reportConfig = config.getConfig();
        }
        if (reportConfig == null) {
            throw new IllegalArgumentException("Report configuration is missing");
        }

        Job job = Job.newReportJob()
                .tenantId(tenantId)
                .reportTemplateId(reportConfig.getReportTemplateId())
                .userId(reportConfig.getUserId())
                .timezone(reportConfig.getTimezone())
                .makePublic(reportConfig.isMakePublic())
                .originator(tbMsg.getOriginator())
                .targets(reportConfig.getTargets())
                .notificationTemplateId(reportConfig.getNotificationTemplateId())
                .build();
        ReportJobConfiguration configuration = job.getConfiguration();

        var msg = ackIfNeeded(ctx, tbMsg);

        TbMsg outputMsg = TbMsg.newMsg(msg, msg.getQueueName(), ctx.getSelf().getRuleChainId(), ctx.getSelfId());
        configuration.setRuleNode(ctx.getSelf());
        configuration.setOutputTbMsgProto(Base64.getEncoder().encodeToString(TbMsg.toProto(outputMsg).toByteArray()));
        configuration.setQueueName(msg.getQueueName());

        DonAsynchron.withCallback(ctx.getJobManager().submitJob(job), result -> {
            //TODO: implement job completion callback
        }, error -> {
            ctx.tellFailure(tbMsg, error);
        });
    }

}
