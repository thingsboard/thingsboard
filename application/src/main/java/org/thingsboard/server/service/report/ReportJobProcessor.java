// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.rule.engine.mail.TbMsgToEmailNode;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.ReportGeneratedNotificationInfo;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.job.JobProcessor;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReportJobProcessor implements JobProcessor {

    public static final String REPORT_CREATION_DISABLED = "Report creation is disabled";

    private final ReportTemplateService reportTemplateService;
    private final SystemSecurityService systemSecurityService;
    private final SubscriptionService subscriptionService;
    @Lazy
    private final NotificationCenter notificationCenter;
    private final NotificationRequestService notificationRequestService;
    private final NotificationExecutorService notificationExecutor;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final OwnersCacheService ownersCacheService;
    @Lazy
    private final ActorSystemContext actorSystemContext;
    private final TbApiUsageStateService apiUsageStateService;
    private final TbApiUsageReportClient apiUsageClient;
    private final UserService userService;

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        ReportJobConfiguration configuration = job.getConfiguration();
        if (configuration.getReportTemplateId() == null) {
            throw new IllegalArgumentException("Report template must be specified");
        }
        if (job.getCustomerId() != null) {
            if (CollectionsUtil.isNotEmpty(configuration.getTargets())) {
                throw new IllegalArgumentException("Notification targets are not supported for customer report jobs");
            }
            if (configuration.getNotificationTemplateId() != null) {
                throw new IllegalArgumentException("Notification template is not supported for customer report jobs");
            }
        }
        boolean creationEnabled = apiUsageStateService.getApiUsageState(job.getTenantId()).isReportCreationEnabled();
        if (!creationEnabled) {
            throw new ApiUsageLimitsExceededException(REPORT_CREATION_DISABLED);
        }
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(job.getTenantId(), configuration.getReportTemplateId());
        User user = userService.findUserById(job.getTenantId(), configuration.getUserId());
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(job.getTenantId(), configuration.getUserId());
        EntityId userOwnerId = ownersCacheService.getOwner(job.getTenantId(), configuration.getUserId());

        ReportTask task = ReportTask.builder()
                .tenantId(job.getTenantId())
                .customerId(user.getCustomerId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .reportTemplateId(reportTemplate.getId())
                .reportTemplateConfig(reportTemplate.getConfiguration())
                .timezone(configuration.getTimezone())
                .makePublic(configuration.isMakePublic())
                .userId(configuration.getUserId())
                .userOwnerId(userOwnerId)
                .originator(configuration.getOriginator())
                .accessToken(accessToken.getToken())
                .accessTokenExpirationTs(accessToken.getClaims().getExpiration().getTime())
                .nonProduction(subscriptionService.isDevelopment(job.getTenantId()))
                .build();
        taskConsumer.accept(task);
        return 1;
    }

    @Override
    public void reprocess(Job job, List<TaskResult> failures, Consumer<Task<?>> taskConsumer) throws Exception {
        process(job, taskConsumer);
    }

    @Override
    public void onJobFinished(Job job) {
        ReportJobResult result = (ReportJobResult) job.getResult();
        ReportJobConfiguration configuration = job.getConfiguration();
        TenantId tenantId = job.getTenantId();

        if (configuration.getOutputTbMsgProto() != null) {
            try {
                produceOutputMsg(job, configuration, result);
            } catch (Exception e) {
                log.error("[{}] Failed to produce rule engine output msg for job {}", tenantId, job.getId(), e);
            }
        }
        if (job.getStatus() == JobStatus.COMPLETED) {
            apiUsageClient.report(job.getTenantId(), null, ApiUsageRecordKey.GENERATED_REPORTS_COUNT);

            Report report = result.getReport();
            if (CollectionsUtil.isNotEmpty(configuration.getTargets()) && configuration.getNotificationTemplateId() != null) {
                String reportPublicUrl = buildPublicReportUrl(tenantId, job, report);
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .tenantId(tenantId)
                        .targets(configuration.getTargets())
                        .templateId(configuration.getNotificationTemplateId())
                        .originatorEntityId(report.getUserId())
                        .info(ReportGeneratedNotificationInfo.builder()
                                .tenantId(tenantId)
                                .customerId(report.getCustomerId())
                                .reportFormat(report.getFormat())
                                .reportName(report.getName())
                                .reportPublicUrl(reportPublicUrl)
                                .userId(report.getUserId())
                                .build())
                        .build();
                processNotification(notificationRequest, report);
            } else if (configuration.getNotificationRequests() != null) {
                configuration.getNotificationRequests().forEach(notificationRequest -> {
                    processNotification(notificationRequest, report);
                });
            }
        } else {
            if (configuration.getNotificationRequests() != null) {
                RuntimeException error = new RuntimeException("Failed to generate report: " + job.getError());
                configuration.getNotificationRequests().forEach(notificationRequest -> {
                    NotificationRequestStats stats = notificationRequest.getStats();
                    if (stats == null) {
                        stats = new NotificationRequestStats();
                    }
                    stats.reportGeneralError(error);
                    notificationRequestService.updateNotificationRequest(tenantId, notificationRequest.getId(), NotificationRequestStatus.SENT, stats);
                });
            }

        }
    }

    private String buildPublicReportUrl(TenantId tenantId, Job job, Report report) {
        if (report.getPublicKey() == null) {
            return "";
        }
        Authority authority = job.getCustomerId() != null ? Authority.CUSTOMER_USER : Authority.TENANT_ADMIN;
        String baseUrl = systemSecurityService.getBaseUrl(authority, tenantId, report.getCustomerId(), null);
        return baseUrl + "/api/v2/report/public/" + report.getPublicKey() + "/download";
    }

    private void processNotification(NotificationRequest notificationRequest, Report report) {
        TenantId tenantId = report.getTenantId();
        NotificationRequestConfig requestConfig;
        if (notificationRequest.getAdditionalConfig() != null) {
            requestConfig = notificationRequest.getAdditionalConfig();
        } else {
            requestConfig = new NotificationRequestConfig();
        }
        requestConfig.setReports(List.of(report.getId()));
        notificationRequest.setAdditionalConfig(requestConfig);

        log.debug("[{}] Submitting notification request with report: {}", tenantId, notificationRequest);
        notificationExecutor.executeAsync(() -> {
            try {
                notificationCenter.processNotificationRequest(tenantId, notificationRequest, null);
            } catch (Exception e) {
                log.error("[{}] Failed to process notification request: {}", tenantId, notificationRequest, e);
            }
        });
    }

    private void produceOutputMsg(Job job, ReportJobConfiguration configuration, ReportJobResult result) {
        TenantId tenantId = job.getTenantId();
        TbMsg outputMsg;
        try {
            outputMsg = TbMsg.fromProto(configuration.getQueueName(), MsgProtos.TbMsgProto.parseFrom(
                    Base64.getDecoder().decode(configuration.getOutputTbMsgProto())), null);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        String relationType;
        String error = job.getStatus() == JobStatus.COMPLETED ? null : job.getError();
        if (error != null) {
            relationType = TbNodeConnectionType.FAILURE;
        } else {
            relationType = TbNodeConnectionType.SUCCESS;
            outputMsg.getMetaData().putValue(TbMsgToEmailNode.REPORTS, result.getReport().getId().toString());
        }

        TransportProtos.ToRuleEngineMsg.Builder ruleEngineMsg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsgProto(TbMsg.toProto(outputMsg))
                .addRelationTypes(relationType);
        if (error != null) {
            ruleEngineMsg.setFailureMessage(error);
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, outputMsg.getQueueName(), tenantId, outputMsg.getOriginator());
        clusterService.pushMsgToRuleEngine(tpi, outputMsg.getId(), ruleEngineMsg.build(), new SimpleTbQueueCallback(tbQueueMsgMetadata -> {
            actorSystemContext.persistDebugOutputIfNeeded(tenantId, configuration.getRuleNode(), outputMsg, Set.of(relationType), null, error);
        }, throwable -> {
            log.error("[{}] Failed to send msg {}", tenantId, ruleEngineMsg, throwable);
        }));
    }

    @Override
    public JobType getType() {
        return JobType.REPORT;
    }

}
