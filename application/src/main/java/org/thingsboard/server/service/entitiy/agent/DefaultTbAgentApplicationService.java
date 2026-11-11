// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.StatefulStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.action.AgentAppActionContext;
import org.thingsboard.server.service.agent.action.AgentAppActionHandler;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentApplicationService extends AbstractTbEntityService implements TbAgentApplicationService {

    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;
    private final AgentApplicationService applicationService;
    private final AgentAppEventService appEventService;
    private final TbClusterService tbClusterService;
    private final AgentEventRateLimiter agentEventRateLimiter;
    private final AgentAppEventStepsResolver eventStepsResolver;
    private Map<AgentAppEventActionType, AgentAppActionHandler> actionHandlers;

    @Autowired
    public DefaultTbAgentApplicationService(AgentAppConfigMergeOrchestrator configMergeOrchestrator,
                                            AgentApplicationService applicationService,
                                            AgentAppEventService appEventService,
                                            TbClusterService tbClusterService,
                                            AgentEventRateLimiter agentEventRateLimiter,
                                            AgentAppEventStepsResolver eventStepsResolver) {
        this.configMergeOrchestrator = configMergeOrchestrator;
        this.applicationService = applicationService;
        this.appEventService = appEventService;
        this.tbClusterService = tbClusterService;
        this.agentEventRateLimiter = agentEventRateLimiter;
        this.eventStepsResolver = eventStepsResolver;
    }

    @Autowired
    public void setActionHandlers(List<AgentAppActionHandler> handlers) {
        this.actionHandlers = handlers.stream()
                .collect(Collectors.toMap(AgentAppActionHandler::getActionType, Function.identity()));
    }

    @Override
    @Transactional
    public AgentApplication update(AgentApplication application, User user) throws Exception {
        if (application.getId() == null) {
            throw new IllegalStateException("Can't update state of the non-existent application!");
        }
        if (!application.isPendingDeletion() && appEventService.hasActiveEventForApplication(application.getId())) {
            throw new DataValidationException("Cannot update application while an event is being processed");
        }
        if (appEventService.hasActiveOrPendingAgentEvent(application.getAgentId())) {
            throw new DataValidationException("Cannot update application while an agent upgrade is in progress");
        }
        TenantId tenantId = user.getTenantId();

        try {
            AgentApplication savedApp = checkNotNull(applicationService.save(tenantId, application));
            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.UPDATED, user);
            return savedApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), application, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public AgentAppInstallResponse install(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception {
        AgentApplication application = request.getApplication();
        if (application == null) {
            throw new ThingsboardException("Install request must include an application", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        application.setId(null);
        application.setTenantId(tenantId);
        application.setOrigin(AgentApplicationOrigin.INSTALLED);
        application.setProjectName(AgentApplication.generateProjectName());

        if (application.getAgentId() != null && appEventService.hasActiveOrPendingAgentEvent(application.getAgentId())) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        try {
            AgentApplication savedApp = checkNotNull(applicationService.saveWithRelatedEntity(tenantId, application, request.getRelatedEntityId()));

            AgentAppEvent event = saveEvent(tenantId, savedApp, AgentAppEventActionType.INSTALL, request);

            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.ADDED, user);
            return new AgentAppInstallResponse(savedApp, event);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT_APPLICATION), application, ActionType.ADDED, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request) throws Exception {
        return execActionEvent(tenantId, applicationId, request, false);
    }

    @Transactional
    @Override
    public AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, boolean skipActiveEventCheck) throws Exception {
        AgentAppEventActionType actionType = request.getActionType();
        AgentApplication application;
        try {
            application = applicationService.findByIdForUpdate(tenantId, applicationId);
        } catch (PessimisticLockingFailureException e) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        checkNotNull(application);
        if (!skipActiveEventCheck && appEventService.hasActiveOrPendingEventForApplication(applicationId)) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (appEventService.hasActiveOrPendingAgentEvent(application.getAgentId())) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        UUID bulkActionId = request.getBulkActionId();
        if (bulkActionId != null && appEventService.existsByApplicationIdAndBulkActionId(applicationId, bulkActionId)) {
            log.info("[{}] Skipping duplicate bulk event for app {} (bulkActionId {})", tenantId, applicationId, bulkActionId);
            return null;
        }

        application.setDesiredTemplateVersion(null);

        AgentAppActionHandler handler = actionHandlers.get(actionType);
        if (handler != null) {
            handler.handle(application, request, new AgentAppActionContext(tenantId));
        }

        applicationService.save(tenantId, application);
        return saveEvent(tenantId, application, actionType, request);
    }

    @Override
    public void cancelEvent(TenantId tenantId, AgentAppEventId eventId) throws Exception {
        AgentAppEvent event = appEventService.findById(tenantId, eventId);
        if (event == null) {
            throw new ThingsboardException("Agent app event not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AgentProcessingStatus processingStatus = event.getProcessingStatus();
        if (processingStatus == AgentProcessingStatus.FINISHED || processingStatus == AgentProcessingStatus.ERROR) {
            throw new ThingsboardException("Cannot cancel event in terminal state: " + processingStatus, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (event.getActionType() != null && event.getActionType().isAgentScoped()) {
            tbClusterService.onAgentAppEventCancelled(tenantId, event.getAgentId(), event);
            return;
        }
        AgentApplication application = checkNotNull(applicationService.findById(tenantId, event.getApplicationId()));
        tbClusterService.onAgentAppEventCancelled(tenantId, application.getAgentId(), event);
    }

    @Override
    public AgentAppEvent upgradeAgent(TenantId tenantId, AgentId agentId, String imageRef) throws Exception {
        if (StringUtils.isEmpty(imageRef)) {
            throw new ThingsboardException("imageRef must not be empty", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (appEventService.hasActiveOrPendingAgentEvent(agentId)
                || appEventService.hasActiveOrPendingAppEventForAgent(agentId)) {
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        agentEventRateLimiter.checkOrThrow(tenantId, agentId);
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setAgentId(agentId);
        event.setActionType(AgentAppEventActionType.AGENT_UPGRADE);
        event.setStartStatus(ProcessingStartStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setContextMetadata(Map.of(AgentUpgradeKeys.IMAGE_REF, imageRef));
        try {
            return appEventService.save(tenantId, event);
        } catch (DataIntegrityViolationException e) {
            // Concurrent create lost the race on the partial unique index (one active agent event per agent).
            throw new ThingsboardException(EVENT_IN_PROGRESS_ERROR_MSG, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    public AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AppConfigMergeCtx ctx) {
        String templateVersion = ctx.getTemplate() != null ? ctx.getTemplate().getCurrentVersion() : null;
        log.trace("Executing mergeForPreview, tenantId [{}], applicationId [{}], templateVersion [{}], composeType [{}], relatedEntityId [{}]",
                tenantId, application.getId(), templateVersion, ctx.getSelectedComposeType(), ctx.getRelatedEntityId());
        configMergeOrchestrator.merge(application, ctx);
        return application;
    }

    @Override
    @Transactional
    public AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId, User user) {
        log.trace("Executing assignRelatedEntity, tenantId [{}], appId [{}], relatedEntityId [{}]", tenantId, agentApplicationId, relatedEntityId);
        try {
            AgentApplication savedApp = applicationService.assignRelatedEntity(tenantId, agentApplicationId, relatedEntityId);
            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.UPDATED, user);
            return savedApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, agentApplicationId, null, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, User user) {
        log.trace("Executing unassignRelatedEntity, tenantId [{}], appId [{}]", tenantId, agentApplicationId);
        try {
            AgentApplication savedApp = applicationService.unassignRelatedEntity(tenantId, agentApplicationId);
            logEntityActionService.logEntityAction(tenantId, savedApp.getId(), savedApp, ActionType.UPDATED, user);
            return savedApp;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, agentApplicationId, null, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    private void rejectServerControlledOverrides(AgentApplication application, AgentAppEventActionType actionType,
                                                 Map<UUID, AgentAppStepState> stepInputs) {
        if (CollectionsUtil.isEmpty(stepInputs)) {
            return;
        }
        for (AgentAppStep step : eventStepsResolver.resolveSteps(application, actionType)) {
            if (!(step instanceof StatefulStep<?> statefulStep)) {
                continue;
            }
            AgentAppStepState templateState = statefulStep.getState();
            if (templateState == null) {
                if (stepInputs.get(step.getId()) != null) {
                    throw new DataValidationException("Step " + step.getId()
                            + " declares no inputs, so step inputs must not be provided for it!");
                }
                continue;
            }
            List<String> overridden = templateState.findServerControlledOverrides(stepInputs.get(step.getId()));
            if (!overridden.isEmpty()) {
                throw new DataValidationException("Step inputs must not set server-controlled fields "
                        + overridden + " of step " + step.getId() + "!");
            }
        }
    }

    private AgentAppEvent saveEvent(TenantId tenantId, AgentApplication application, AgentAppEventActionType actionType, AgentAppEventRequest request) {
        agentEventRateLimiter.checkOrThrow(tenantId, application.getAgentId());
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(application.getId());
        event.setAgentId(application.getAgentId());
        event.setApplicationName(application.getName());
        event.setActionType(actionType);
        event.setStartStatus(ProcessingStartStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        rejectServerControlledOverrides(application, actionType, request.getStepInputs());
        event.setStepStates(request.getStepInputs());
        event.setBulkActionId(request.getBulkActionId());
        return appEventService.save(tenantId, event);
    }
}
