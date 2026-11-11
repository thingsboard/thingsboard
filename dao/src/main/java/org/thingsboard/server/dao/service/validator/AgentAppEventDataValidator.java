// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.StatefulStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventStepsResolver;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class AgentAppEventDataValidator extends DataValidator<AgentAppEvent> {

    private final AgentApplicationDao agentApplicationDao;
    private final AgentDao agentDao;
    private final AgentAppEventStepsResolver stepsResolver;

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppEvent event) {
        if (event.getTenantId() == null) {
            throw new DataValidationException("Agent app event tenantId must not be null!");
        }
        if (event.getActionType() == null) {
            throw new DataValidationException("Agent app event actionType must not be null!");
        }
        if (event.getActionType().isAgentScoped()) {
            if (event.getApplicationId() != null) {
                throw new DataValidationException("Agent-scoped event must not reference an application!");
            }
        } else if (event.getApplicationId() == null) {
            throw new DataValidationException("Agent app event applicationId must not be null!");
        }
        if (event.getAgentId() == null) {
            throw new DataValidationException("Agent app event agentId must not be null!");
        }
        if (event.getStartStatus() == null) {
            throw new DataValidationException("Agent app event startStatus must not be null!");
        }
        if (event.getStartStatus() != ProcessingStartStatus.PENDING && event.getProcessingStatus() == null) {
            throw new DataValidationException("Agent app event processingStatus must not be null!");
        }

        Map<UUID, AgentAppStepState> stepStates = event.getStepStates();
        if (!CollectionUtils.isEmpty(stepStates)) {
            stepStates.values().stream().filter(Objects::nonNull).forEach(AgentAppStepState::validate);
        }

        if (event.getActionType().isAgentScoped()) {
            if (agentDao.findById(tenantId, event.getAgentId().getId()) == null) {
                throw new DataValidationException("Agent app event references non-existent agent!");
            }
            return;
        }

        AgentApplication app = agentApplicationDao.findById(tenantId, event.getApplicationId().getId());
        if (app == null) {
            throw new DataValidationException("Agent app event references non-existent application!");
        }

        List<AgentAppStep> stepsRequiringUserInput = stepsResolver.resolveSteps(app, event.getActionType())
                .stream()
                .filter(AgentAppStep::isStateful)
                .filter(step -> ((StatefulStep<?>) step).getState() != null)
                .filter(step -> ((StatefulStep<?>) step).getState().hasUserChoice())
                .toList();

        if (!CollectionUtils.isEmpty(stepsRequiringUserInput) && CollectionUtils.isEmpty(stepStates)) {
                throw new DataValidationException("Agent app step states must not be null!");
        }

        for (AgentAppStep step : stepsRequiringUserInput) {
            AgentAppStepState templateState = ((StatefulStep<?>) step).getState();
            if (templateState == null) {
                continue;
            }
            AgentAppStepState userSubmittedState = stepStates.get(step.getId());
            List<String> missing = templateState.missingRequiredUserInputs(userSubmittedState);
            if (!missing.isEmpty()) {
                log.trace("Step state is missing required user inputs {} for step {}", missing, step.getId());
                throw new DataValidationException("Step state is missing required user inputs " + missing + "!");
            }
        }
    }
}
