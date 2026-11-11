// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.StatefulStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentAppProvisioner {

    private final AgentApplicationService applicationService;
    private final AgentAppEventService appEventService;
    private final AgentAppEventStepsResolver stepsResolver;

    @Transactional
    public AgentApplication saveWithLifecycleEvent(TenantId tenantId, AgentApplication application, EntityId relatedEntityId,
                                                   AgentAppEventActionType eventActionType) {
        AgentApplication savedApp = applicationService.saveWithRelatedEntity(tenantId, application, relatedEntityId);
        if (eventActionType != null) {
            appEventService.save(tenantId, buildPendingEvent(tenantId, savedApp, eventActionType));
        }
        return savedApp;
    }

    private AgentAppEvent buildPendingEvent(TenantId tenantId, AgentApplication application, AgentAppEventActionType eventActionType) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(application.getId());
        event.setAgentId(application.getAgentId());
        event.setApplicationName(application.getName());
        event.setActionType(eventActionType);
        event.setStartStatus(ProcessingStartStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        event.setStepStates(resolveDefaultStepStates(application, eventActionType));
        return event;
    }

    /**
     * Auto-install has no user to make step choices, so fall back to the template's declared default state for every
     * stateful step whose fields are user choices — mirroring what the UI pre-fills and the manual/bulk paths carry
     * via {@code stepInputs}. Required since {@link org.thingsboard.server.dao.service.validator.AgentAppEventDataValidator}
     * rejects events that omit states for user-choice steps.
     */
    private Map<UUID, AgentAppStepState> resolveDefaultStepStates(AgentApplication application, AgentAppEventActionType eventActionType) {
        Map<UUID, AgentAppStepState> stepStates = new LinkedHashMap<>();
        for (AgentAppStep step : stepsResolver.resolveSteps(application, eventActionType)) {
            if (!step.isStateful()) {
                continue;
            }
            AgentAppStepState state = ((StatefulStep<?>) step).getState();
            if (state != null && state.hasUserChoice()) {
                stepStates.put(step.getId(), state);
            }
        }
        return stepStates;
    }
}
