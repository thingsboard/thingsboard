// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.AgentArgumentSubstitutor;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeServicesStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.ConfigType;
import org.thingsboard.server.gen.agent.v1.HelloAck;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StepId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AgentMsgConstructorUtils {

    private static final HelloAck HELLO_SUCCESS_MSG = HelloAck.newBuilder().setSuccess(true).build();

    public static ServerToAgent helloSuccessResponse() {
        return ServerToAgent.newBuilder()
                .setHelloAck(HELLO_SUCCESS_MSG)
                .build();
    }

    public static ServerToAgent buildAppCommand(AgentAppEvent event, AgentApplication application, AgentAppStep step, int totalSteps) {
        if (application.getConfig() == null) {
            throw new IllegalArgumentException("Cannot build app command: application [" + application.getId() + "] has no config");
        }
        AppCommand.Builder builder = baseCommand(event, totalSteps)
                .setAppName(application.getName() != null ? application.getName() : "")
                .setConfigType(mapConfigType(application.getConfig().getType()));
        if (step != null) {
            putStep(builder, step, buildStepMetadata(event, step, application));
        }
        return toServerMsg(builder);
    }

    public static ServerToAgent buildAgentCommand(AgentAppEvent event, AgentAppStep step, int totalSteps) {
        Map<String, String> metadata = getBaseMeta(step);
        if (event.getContextMetadata() != null) {
            metadata.putAll(event.getContextMetadata());
        }
        if (event.getFinalizeDeadlineTs() != null) {
            metadata.put(AgentUpgradeKeys.FINALIZE_DEADLINE_TS, String.valueOf(event.getFinalizeDeadlineTs()));
        }
        AppCommand.Builder builder = baseCommand(event, totalSteps).setAppName("");
        putStep(builder, step, metadata);
        return toServerMsg(builder);
    }

    private static AppCommand.Builder baseCommand(AgentAppEvent event, int totalSteps) {
        return AppCommand.newBuilder()
                .setCommandId(CommandId.newBuilder()
                        .setIdMSB(event.getId().getId().getMostSignificantBits())
                        .setIdLSB(event.getId().getId().getLeastSignificantBits())
                        .build())
                .setAction(mapAction(event.getActionType()))
                .setTotalSteps(totalSteps);
    }

    private static void putStep(AppCommand.Builder builder, AgentAppStep step, Map<String, String> metadata) {
        builder.putAllMetadata(metadata)
                .setStepId(StepId.newBuilder()
                        .setIdMSB(step.getId().getMostSignificantBits())
                        .setIdLSB(step.getId().getLeastSignificantBits())
                        .build());
    }

    private static ServerToAgent toServerMsg(AppCommand.Builder builder) {
        return ServerToAgent.newBuilder()
                .setAppCommand(builder.build())
                .build();
    }

    private static AppCommandAction mapAction(AgentAppEventActionType actionType) {
        return switch (actionType) {
            case INSTALL -> AppCommandAction.APP_INSTALL;
            case UPDATE -> AppCommandAction.APP_UPDATE;
            case DELETE -> AppCommandAction.APP_DELETE;
            case RESTART -> AppCommandAction.APP_RESTART;
            case ROLLBACK -> AppCommandAction.APP_ROLLBACK;
            case UPGRADE -> AppCommandAction.APP_UPGRADE;
            case AGENT_UPGRADE -> AppCommandAction.APP_AGENT_UPGRADE;
        };
    }

    private static ConfigType mapConfigType(AgentAppConfigType appConfigType) {
        return switch (appConfigType) {
            case DOCKER_COMPOSE -> ConfigType.DOCKER_COMPOSE;
        };
    }

    private static Map<String, String> buildStepMetadata(AgentAppEvent event, AgentAppStep step, AgentApplication application) {
        Map<UUID, AgentAppStepState> stepIdToUserStateSteps = event.getStepStates();
        Map<String, String> metadata = getBaseMeta(step);

        if (application.getProjectName() != null) {
            metadata.put("projectName", application.getProjectName());
        }

        AgentAppStepState resolvedUserState = CollectionUtils.isEmpty(stepIdToUserStateSteps)
                ? null : stepIdToUserStateSteps.get(step.getId());
        metadata.putAll(step.getCommandMetadata(application, resolvedUserState));

        var arguments = application.getConfig() != null ? application.getConfig().getArguments() : null;
        metadata.computeIfPresent(ComposeServicesStep.COMPOSE, (_, compose) ->
                AgentArgumentSubstitutor.substitute(compose, event.getResolvedArguments(), arguments));
        // A RUN_JOB copies env/volumes from a compose service, which may carry ${tb.x} args — resolve them here too.
        metadata.computeIfPresent(RunJobStepState.JOB, (_, job) ->
                AgentArgumentSubstitutor.substitute(job, event.getResolvedArguments(), arguments));

        return metadata;
    }

    private static Map<String, String> getBaseMeta(AgentAppStep step) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("stepTitle", step.getTitle() != null ? step.getTitle() : "");
        metadata.put("stepType", step.getType() != null ? step.getType().name() : "");
        return metadata;
    }

}
