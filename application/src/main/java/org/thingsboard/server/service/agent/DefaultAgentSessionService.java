// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentUpgradeKeys;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.gen.agent.v1.Hello;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionRegistry;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentSessionService implements AgentSessionService {

    public static final String TAKEOVER_DEMOTED = "AGENT_TAKEOVER_DEMOTED";

    private final AgentService agentService;
    private final AgentSessionRegistry sessions;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbTransactionalCache<AgentId, String> agentIdServiceIdCache;
    private final AgentAppEventService appEventService;
    private final AgentStateService agentStateService;

    @Override
    public Optional<Status> onConnected(AgentSession session, Hello msg) {
        AgentSessionState state = session.getState();
        Agent agent;
        try {
            agent = findAgentByRoutingKeyAndSecret(msg.getRoutingKey(), msg.getRoutingSecret());
        } catch (SecurityException e) {
            log.warn("Agent authentication failed, routingKey={}: {}", msg.getRoutingKey(), e.getMessage());
            return Optional.of(Status.UNAUTHENTICATED.withDescription(e.getMessage()));
        }
        if (agent == null) {
            log.trace("Agent not found, routingKey={}", msg.getRoutingKey());
            return Optional.of(Status.NOT_FOUND.withDescription("Failed to find the agent. Routing key: " + msg.getRoutingKey()));
        }
        state.setAgent(agent);
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();

        Optional<Status> demotion = triageTakeoverInstance(tenantId, agentId, msg.getContainerId());
        if (demotion.isPresent()) {
            return demotion;
        }

        sessions.registerOrReplace(agentId, tenantId, session);
        agentIdServiceIdCache.put(agentId, serviceInfoProvider.getServiceId());
        log.info("[{}] agent [{}] connected successfully", tenantId, agentId);
        agentStateService.onAgentConnect(agent, System.currentTimeMillis(), msg.getAgentVersion(), msg.getContainerId());
        return Optional.empty();
    }

    // Hello-time identity triage against the latest agent-scoped event: a copy of a takeover that is still
    // in flight is accepted; the survivor of a swap whose SUCCESS was lost (containerId == winner) is
    // accepted and the swept event is annotated as applied; a never-granted copy of a dead upgrade is denied
    // outright so it never becomes a routing target.
    private Optional<Status> triageTakeoverInstance(TenantId tenantId, AgentId agentId, String containerId) {
        if (StringUtils.isEmpty(containerId)) {
            return Optional.empty();
        }
        Optional<AgentAppEvent> latestOpt = appEventService.findLatestAgentEventByAgentId(agentId);
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }
        AgentAppEvent latest = latestOpt.get();

        // old container deadline timeout is exceeded or agent upgrade is in progress
        if (!isNewContainerFromUpgrade(containerId, latest) || !isTerminal(latest)) {
            return Optional.empty();
        }
        if (containerId.equals(latest.getWinnerContainerId())) {
            annotateSwapAppliedAfterTimeout(tenantId, agentId, latest);
            return Optional.empty();
        }
        log.info("[{}][{}] Denying hello from stale takeover container {} of terminal event {}", tenantId, agentId, containerId, latest.getId());
        return Optional.of(Status.FAILED_PRECONDITION.withDescription(TAKEOVER_DEMOTED));
    }

    private void annotateSwapAppliedAfterTimeout(TenantId tenantId, AgentId agentId, AgentAppEvent event) {
        if (event.getProcessingStatus() != AgentProcessingStatus.ERROR || !isPastFinalizeDeadline(event)) {
            return;
        }
        if (AgentUpgradeKeys.RECONCILED_ACTIVITY.equals(event.getCurrentActivity())) {
            return;
        }
        log.info("[{}][{}] Reconciling agent upgrade event {}: swap completed after timeout", tenantId, agentId, event.getId());
        appEventService.updateActivityUnguarded(event.getId(), AgentUpgradeKeys.RECONCILED_ACTIVITY);
    }

    private static boolean isPastFinalizeDeadline(AgentAppEvent event) {
        Long deadline = event.getFinalizeDeadlineTs();
        return deadline != null && System.currentTimeMillis() > deadline;
    }

    private static boolean isTerminal(AgentAppEvent latest) {
        return latest.getProcessingStatus() != null && latest.getProcessingStatus().isTerminated();
    }

    private static boolean isNewContainerFromUpgrade(String containerId, AgentAppEvent latest) {
        Map<String, String> meta = latest.getContextMetadata();
        String newContainerId = meta != null ? meta.get(AgentUpgradeKeys.NEW_CONTAINER_ID) : null;
        return containerId.equals(newContainerId);
    }

    @Override
    public void onCompleted(AgentSession session) {
        disconnect(session);
    }

    @Override
    public void onError(AgentSession session) {
        disconnect(session);
    }

    private void disconnect(AgentSession session) {
        Agent agent = session.getState().getAgent();
        if (agent == null) {
            return;
        }
        if (!sessions.removeIfSame(session)) {
            return;
        }
        String foreignOwner = findForeignOwner(agent.getId());
        if (foreignOwner != null) {
            // The agent reconnected through another node; this is a stale session — don't clobber its state.
            log.debug("[{}] Skipping disconnect bookkeeping: agent is now owned by {}", agent.getId(), foreignOwner);
            return;
        }
        agentIdServiceIdCache.evict(agent.getId());
        foreignOwner = findForeignOwner(agent.getId());
        if (foreignOwner != null) {
            log.debug("[{}] Skipping disconnect bookkeeping: agent was taken over by {}", agent.getId(), foreignOwner);
            return;
        }
        agentStateService.onAgentDisconnect(agent, System.currentTimeMillis());
    }

    private String findForeignOwner(AgentId agentId) {
        var cached = agentIdServiceIdCache.get(agentId);
        String owner = cached == null ? null : cached.get();
        return owner != null && !owner.equals(serviceInfoProvider.getServiceId()) ? owner : null;
    }

    private Agent findAgentByRoutingKeyAndSecret(String routingKey, String routingSecret) {
        Agent agent = agentService.findAgentByRoutingKey(TenantId.SYS_TENANT_ID, routingKey);
        if (agent == null) {
            return null;
        }
        if (!agent.getSecret().equals(routingSecret)) {
            throw new SecurityException("Failed to validate the agent! Routing key: " + routingKey);
        }
        return agent;
    }

}
