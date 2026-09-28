// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.bulk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkippedApp;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultAgentBulkActionProcessingService implements AgentBulkActionProcessingService {

    private static final Set<AgentAppEventActionType> ALLOWED_BULK_ACTIONS = Set.of(
            AgentAppEventActionType.UPDATE,
            AgentAppEventActionType.DELETE,
            AgentAppEventActionType.RESTART,
            AgentAppEventActionType.ROLLBACK,
            AgentAppEventActionType.UPGRADE
    );

    private static final int BATCH_UPDATE_SIZE = 50;
    private static final int PREVIEW_SAMPLE_PER_REASON = 20;
    private static final int STUCK_ACTION_BATCH_SIZE = 100;
    private static final int MAX_STUCK_ACTION_BATCHES = 100;
    private static final int ELIGIBILITY_BATCH_SIZE = 100;

    private final TbAgentApplicationService tbAgentApplicationService;
    private final AgentAppProfileService profileService;
    private final AgentApplicationDao applicationDao;
    private final AgentAppEventService agentAppEventService;
    private final AgentBulkActionService agentBulkActionService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    @Value("${queue.agent.bulk-ops-stuck-threshold-ms:600000}")
    private long stuckActionThresholdMs;

    @Value("${agents.bulkOps.previewThreads:4}")
    private int previewThreads;
    @Value("${agents.bulkOps.previewQueueSize:16}")
    private int previewQueueSize;

    private ListeningExecutorService previewExecutor;

    @PostConstruct
    public void init() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(previewThreads, previewThreads,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(previewQueueSize),
                ThingsBoardThreadFactory.forName("agent-bulk-preview"));
        executor.allowCoreThreadTimeOut(true);
        previewExecutor = MoreExecutors.listeningDecorator(executor);
    }

    @PreDestroy
    public void shutdown() {
        if (previewExecutor != null) {
            previewExecutor.shutdownNow();
        }
    }

    @Scheduled(initialDelayString = "${queue.agent.bulk-ops-stuck-check-interval-ms:600000}",
            fixedDelayString = "${queue.agent.bulk-ops-stuck-check-interval-ms:600000}")
    public void failStuckBulkActions() {
        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return;
        }
        long threshold = System.currentTimeMillis() - stuckActionThresholdMs;
        PageLink pageLink = new PageLink(STUCK_ACTION_BATCH_SIZE);
        for (int batch = 0; batch < MAX_STUCK_ACTION_BATCHES; batch++) {
            List<AgentBulkAction> stuckActions = agentBulkActionService.findStuckBulkActions(threshold, pageLink).getData();
            if (stuckActions.isEmpty()) {
                return;
            }
            for (AgentBulkAction action : stuckActions) {
                AgentBulkActionStatus originalStatus = action.getStatus();
                log.warn("Failing stuck bulk action {} in status {} (threshold {})",
                        action.getId(), originalStatus, threshold);
                agentBulkActionService.failIfStillStuck(action.getId(), AgentBulkActionStatus.START_FAILED,
                        "Stuck in " + originalStatus + " state, failed by cleanup job");
            }
        }
        log.warn("Stopped failing stuck bulk actions after {} batches", MAX_STUCK_ACTION_BATCHES);
    }

    @Override
    public AgentBulkAction enqueueBulkOperation(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        validateBulkActionType(actionType);
        AgentBulkAction bulkAction = saveBulkAction(tenantId, agentProfileId, applicationProfileId, actionType);
        clusterService.pushMsgToAgentBulkOps(bulkAction, request);

        return bulkAction;
    }

    @Override
    public ListenableFuture<BulkOperationPreview> previewAsync(TenantId tenantId, AgentProfileId agentProfileId,
                                                               AgentAppProfileId applicationProfileId,
                                                               BulkOperationRequest request) throws ThingsboardException {
        validateBulkActionType(request.getActionType());
        try {
            return previewExecutor.submit(() -> preview(tenantId, agentProfileId, applicationProfileId, request));
        } catch (RejectedExecutionException e) {
            log.warn("[{}] Rejecting bulk operation preview for profile pair [{}][{}]: queue is full",
                    tenantId, agentProfileId, applicationProfileId);
            throw new ThingsboardException("Too many bulk operation previews in progress, please retry",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    public BulkOperationPreview preview(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        validateBulkActionType(actionType);
        AgentAppProfileInfo profile = profileService.findProfileInfoById(tenantId, applicationProfileId);

        BulkOperationResult result = new BulkOperationResult();
        result.setMaxRetainedSkippedPerReason(PREVIEW_SAMPLE_PER_REASON);
        filterEligibleApps(tenantId, agentProfileId, result, profile, actionType, _ -> { });

        BulkOperationPreview preview = new BulkOperationPreview();
        preview.setTotal(result.getTotal());
        preview.setEligible(result.getEligible());
        preview.setSkippedCountsByReason(buildSkipCounts(result));
        preview.setSkippedSample(sampleSkippedPerReason(result.getSkipped()));
        return preview;
    }

    @Override
    public void processBulkOperation(AgentBulkOperationMsg msg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        AgentBulkActionId bulkActionId = new AgentBulkActionId(new UUID(msg.getBulkActionIdMSB(), msg.getBulkActionIdLSB()));
        try {
            AgentProfileId agentProfileId = new AgentProfileId(new UUID(msg.getAgentProfileIdMSB(), msg.getAgentProfileIdLSB()));
            AgentAppProfileId applicationProfileId = new AgentAppProfileId(new UUID(msg.getApplicationProfileIdMSB(), msg.getApplicationProfileIdLSB()));
            AgentAppEventActionType actionType = AgentAppEventActionType.valueOf(msg.getActionType());

            Map<UUID, AgentAppStepState> stepInputs = parseStepInputs(msg.getStepInputs());

            AgentBulkAction bulkAction = agentBulkActionService.findById(tenantId, bulkActionId);
            if (bulkAction == null) {
                log.warn("Bulk action {} not found, skipping", bulkActionId);
                return;
            }
            if (bulkAction.getStatus() != AgentBulkActionStatus.QUEUED) {
                log.info("Bulk action {} is already in status {}, skipping redelivered message",
                        bulkActionId, bulkAction.getStatus());
                return;
            }

            bulkAction.setStatus(AgentBulkActionStatus.IN_PROGRESS);
            bulkAction.setProcessingStartedTime(System.currentTimeMillis());
            agentBulkActionService.save(tenantId, bulkAction);

            AgentAppProfileInfo profile = profileService.findProfileInfoById(tenantId, applicationProfileId);

            filterAppsAndExecuteBulkOperations(tenantId, agentProfileId, bulkAction, profile, actionType, stepInputs);
        } catch (Exception e) {
            log.error("Bulk operation {} failed unexpectedly", bulkActionId, e);
            try {
                AgentBulkAction bulkAction = agentBulkActionService.findById(tenantId, bulkActionId);
                if (bulkAction != null) {
                    bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
                    bulkAction.setErrorMsg("Unexpected error: " + e.getMessage());
                    agentBulkActionService.save(tenantId, bulkAction);
                }
            } catch (Exception saveError) {
                log.error("Failed to save FAILED status for bulk action {}", bulkActionId, saveError);
            }
        }
    }

    private void filterAppsAndExecuteBulkOperations(TenantId tenantId, AgentProfileId agentProfileId, AgentBulkAction bulkAction,
                                                    AgentAppProfileInfo profile, AgentAppEventActionType actionType,
                                                    Map<UUID, AgentAppStepState> stepInputs) {
        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplicationInfo> eligibleApps = new ArrayList<>();
        try {
            filterEligibleApps(tenantId, agentProfileId, result, profile, actionType, eligibleApps::add);
        } catch (Exception e) {
            log.error("Bulk operation {} failed during filtering for bulkAction {}", actionType, bulkAction.getId(), e);
            bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
            bulkAction.setErrorMsg(e.getMessage());
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }

        bulkAction.setTotal(result.getTotal());

        if (eligibleApps.isEmpty()) {
            bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
            bulkAction.setErrorMsg("Couldn't find any eligible applications for execution");
            bulkAction.setSkipCounts(buildSkipCounts(result));
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }
        agentBulkActionService.save(tenantId, bulkAction);
        saveErrorMsgsForSkipped(tenantId, bulkAction, actionType, stepInputs, result);
        execBulkOperationForEach(tenantId, bulkAction, actionType, stepInputs, result, eligibleApps);
    }

    private void saveErrorMsgsForSkipped(TenantId tenantId, AgentBulkAction bulkAction,
                                         AgentAppEventActionType actionType, Map<UUID, AgentAppStepState> stepInputs,
                                         BulkOperationResult result) {
        final UUID bulkActionId = bulkAction.getId().getId();
        long now = System.currentTimeMillis();
        List<AgentAppEvent> errEvents = new ArrayList<>(result.getSkipped().size());
        for (SkippedApp s : result.getSkipped()) {
            // save synthetic error event for users to understand the status of the execution
            AgentAppEvent errEvent = new AgentAppEvent();
            errEvent.setTenantId(tenantId);
            errEvent.setApplicationId(s.getApplicationId());
            errEvent.setAgentId(s.getAgentId());
            errEvent.setApplicationName(s.getApplicationName());
            errEvent.setActionType(actionType);
            errEvent.setStartStatus(ProcessingStartStatus.DELIVERY_FAIL);
            errEvent.setProcessingStatus(AgentProcessingStatus.START_FAILED);
            errEvent.setErrorMessage(s.getReason() + " " + s.getMsg());
            errEvent.setUpdatedTime(now);
            errEvent.setStepStates(stepInputs);
            errEvent.setBulkActionId(bulkActionId);
            errEvents.add(errEvent);
        }
        for (int from = 0; from < errEvents.size(); from += BATCH_UPDATE_SIZE) {
            agentAppEventService.saveAll(tenantId, errEvents.subList(from, Math.min(from + BATCH_UPDATE_SIZE, errEvents.size())));
        }
    }

    private void execBulkOperationForEach(TenantId tenantId, AgentBulkAction bulkAction,
                                          AgentAppEventActionType actionType, Map<UUID, AgentAppStepState> stepInputs,
                                          BulkOperationResult result, List<AgentApplicationInfo> eligibleApps) {
        final UUID bulkActionId = bulkAction.getId().getId();
        int processed = 0;
        for (var app : eligibleApps) {
            try {
                AgentAppEvent event = execAppActionEvent(tenantId, app, actionType, bulkActionId, stepInputs);
                if (event != null) {
                    result.incrementSubmitted();
                }
            } catch (Exception e) {
                result.addSkipped(getSkippedOnFailure(e, app, actionType));
            }
            processed++;
            if (processed % BATCH_UPDATE_SIZE == 0 || processed == eligibleApps.size()) {
                bulkAction.setSubmitted(result.getSubmitted());
                bulkAction.setSkipCounts(buildSkipCounts(result));
                if (processed == eligibleApps.size()) {
                    bulkAction.setStatus(AgentBulkActionStatus.STARTED);
                }
                agentBulkActionService.save(tenantId, bulkAction);
            }
        }
    }

    private AgentAppEvent execAppActionEvent(TenantId tenantId, AgentApplication app,
                                             AgentAppEventActionType actionType,
                                             UUID bulkActionId, Map<UUID, AgentAppStepState> stepInputs) throws Exception {
        AgentAppEventRequest eventRequest = new AgentAppEventRequest();
        eventRequest.setActionType(actionType);
        eventRequest.setApplication(app);
        eventRequest.setStepInputs(stepInputs);
        eventRequest.setBulkActionId(bulkActionId);
        // Run the active-event check (under the per-app row lock in execActionEvent) so two concurrent
        // bulk actions on overlapping apps can't both create an active event for the same application.
        return tbAgentApplicationService.execActionEvent(tenantId, app.getId(), eventRequest);
    }

    private Map<UUID, AgentAppStepState> parseStepInputs(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return null;
        }
        return JacksonUtil.fromString(json, new TypeReference<>() {});
    }

    private void validateBulkActionType(AgentAppEventActionType actionType) {
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
    }

    private Map<SkipReason, Integer> buildSkipCounts(BulkOperationResult result) {
        return new EnumMap<>(result.getSkipCounts());
    }

    private List<SkippedApp> sampleSkippedPerReason(Collection<SkippedApp> skipped) {
        Map<SkipReason, Integer> perReason = new EnumMap<>(SkipReason.class);
        List<SkippedApp> sample = new ArrayList<>();
        for (SkippedApp s : skipped) {
            int taken = perReason.getOrDefault(s.getReason(), 0);
            if (taken < DefaultAgentBulkActionProcessingService.PREVIEW_SAMPLE_PER_REASON) {
                sample.add(s);
                perReason.put(s.getReason(), taken + 1);
            }
        }
        return sample;
    }

    private SkippedApp getSkippedOnFailure(Throwable throwable, AgentApplicationInfo app, AgentAppEventActionType actionType) {
        log.warn("Failed to execute bulk {} for app {}: {}", actionType, app.getId(), throwable.getMessage());
        return toSkippedApp(app, resolveSkipReason(throwable), throwable.getMessage());
    }

    private SkipReason resolveSkipReason(Throwable throwable) {
        if (throwable instanceof TbRateLimitsException) {
            return SkipReason.RATE_LIMIT_EXCEEDED;
        }
        if (throwable instanceof ThingsboardException
                && TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG.equals(throwable.getMessage())) {
            return SkipReason.ACTIVE_EVENT;
        }
        return SkipReason.ERROR;
    }

    /**
     * Walks every application of the (agent profile, app profile) pair and classifies it into {@code result}.
     * Eligible applications are handed to {@code eligibleSink} rather than collected here, so the preview path
     * can count them without materializing the fleet while the execute path keeps the list it needs.
     */
    private void filterEligibleApps(TenantId tenantId, AgentProfileId agentProfileId, BulkOperationResult result,
                                    AgentAppProfileInfo profile, AgentAppEventActionType actionType,
                                    Consumer<AgentApplicationInfo> eligibleSink) {
        PageDataIterable<AgentApplicationInfo> apps = new PageDataIterable<>(
                link -> applicationDao.findByApplicationProfileIdAndAgentProfileId(
                        tenantId, profile.getId().getId(), agentProfileId.getId(), link),
                ELIGIBILITY_BATCH_SIZE);

        List<AgentApplicationInfo> batch = new ArrayList<>(ELIGIBILITY_BATCH_SIZE);
        for (AgentApplicationInfo app : apps) {
            batch.add(app);
            if (batch.size() == ELIGIBILITY_BATCH_SIZE) {
                filterBatch(batch, profile, actionType, result, eligibleSink);
                batch.clear();
            }
        }
        filterBatch(batch, profile, actionType, result, eligibleSink);
    }

    private void filterBatch(List<AgentApplicationInfo> batch, AgentAppProfileInfo profile, AgentAppEventActionType actionType,
                             BulkOperationResult result, Consumer<AgentApplicationInfo> eligibleSink) {
        if (batch.isEmpty()) {
            return;
        }
        Set<AgentApplicationId> appsWithEvent = agentAppEventService.findApplicationIdsWithActiveOrPendingEvents(
                batch.stream().map(AgentApplicationInfo::getId).collect(Collectors.toSet()));
        Set<AgentId> agentsWithEvent = agentAppEventService.findAgentIdsWithActiveOrPendingAgentEvents(
                batch.stream().map(AgentApplicationInfo::getAgentId).collect(Collectors.toSet()));

        for (AgentApplicationInfo app : batch) {
            result.incrementTotal();
            boolean hasActiveEvent = appsWithEvent.contains(app.getId()) || agentsWithEvent.contains(app.getAgentId());
            Optional<SkipReason> skipReason = shouldSkipOperation(profile, actionType, app, hasActiveEvent);

            if (skipReason.isPresent()) {
                result.addSkipped(toSkippedApp(app, skipReason.get(), null));
            } else {
                result.incrementEligible();
                eligibleSink.accept(app);
            }
        }
    }

    private SkippedApp toSkippedApp(AgentApplicationInfo app, SkipReason reason, String msg) {
        return new SkippedApp(
                app.getAgentId(),
                app.getAgentName(),
                app.getId(),
                app.getName(),
                reason,
                msg
        );
    }

    private Optional<SkipReason> shouldSkipOperation(AgentAppProfileInfo profile, AgentAppEventActionType actionType,
                                                     AgentApplicationInfo app, boolean hasActiveOrPendingEvent) {
        SkipReason skipReason = null;

        if (templateVersionEqualityRequired(actionType) && !Objects.equals(app.getTemplateVersion(), profile.getTemplateVersion())) {
            skipReason = SkipReason.VERSION_MISMATCH;
        } else if (hasActiveOrPendingEvent) {
            skipReason = SkipReason.ACTIVE_EVENT;
        } else if (isValidForUpgrade(profile, actionType, app)) {
            skipReason = SkipReason.VERSION_MISMATCH;
        }

        return Optional.ofNullable(skipReason);
    }

    private boolean isValidForUpgrade(AgentAppProfileInfo profile, AgentAppEventActionType actionType, AgentApplicationInfo app) {
        return actionType == AgentAppEventActionType.UPGRADE && !profile.getTemplateCurrentVersion().equals(app.getNextVersion());
    }

    private boolean templateVersionEqualityRequired(AgentAppEventActionType actionType) {
        return actionType != AgentAppEventActionType.UPGRADE && actionType != AgentAppEventActionType.RESTART;
    }

    private AgentBulkAction saveBulkAction(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId,
                                           AgentAppEventActionType actionType) {
        AgentBulkAction bulkAction = new AgentBulkAction();
        bulkAction.setTenantId(tenantId);
        bulkAction.setAgentProfileId(agentProfileId.getId());
        bulkAction.setApplicationProfileId(applicationProfileId.getId());
        bulkAction.setActionType(actionType);
        bulkAction.setStatus(AgentBulkActionStatus.QUEUED);
        return agentBulkActionService.save(tenantId, bulkAction);
    }

}
