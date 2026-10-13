// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.AgentAppEventNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToAgentNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.agent.AgentRpcService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.UUID;

@Service
@TbCoreComponent
public class DefaultTbAgentConsumerService extends AbstractConsumerService<ToAgentNotificationMsg> {

    private static final String CONSUMER_NAME = "tb-agent";

    @Value("${queue.agent.poll-interval:25}")
    private int pollInterval;
    @Value("${queue.agent.pack-processing-timeout:10000}")
    private int packProcessingTimeout;

    private final TbCoreQueueFactory queueFactory;
    private final AgentContextComponent ctx;

    public DefaultTbAgentConsumerService(TbCoreQueueFactory queueFactory,
                                         ActorSystemContext actorContext,
                                         AgentContextComponent ctx,
                                         TbTenantProfileCache tenantProfileCache,
                                         TbDeviceProfileCache deviceProfileCache,
                                         TbAssetProfileCache assetProfileCache,
                                         TbResourceDataCache tbResourceDataCache,
                                         TbApiUsageStateService apiUsageStateService,
                                         PartitionService partitionService,
                                         ApplicationEventPublisher eventPublisher,
                                         JwtSettingsService jwtSettingsService
                                         ) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, tbResourceDataCache, apiUsageStateService, partitionService,
                eventPublisher, jwtSettingsService);
        this.queueFactory = queueFactory;
        this.ctx = ctx;
    }

    @PostConstruct
    public void init() {
        super.init(CONSUMER_NAME);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollInterval;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToAgentNotificationMsg>> createNotificationsConsumer() {
        return queueFactory.createToAgentNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToAgentNotificationMsg> msg, TbCallback callback) {
        try {
            ToAgentNotificationMsg notification = msg.getValue();
            if (notification.hasAgentAppEventNotification()) {
                processAgentEventApp(notification);
            } else if (notification.hasLogStreamRequest()) {
                AgentRpcService agentRpcService = ctx.getAgentRpcService();
                if (agentRpcService != null) {
                    agentRpcService.processLogStreamRequest(notification.getLogStreamRequest());
                } else {
                    log.debug("No AgentRpcService available (agent functionality disabled), ignoring msg: {}", notification);
                }
            }
            callback.onSuccess();
        } catch (Exception e) {
            log.warn("Failed to process agent notification message", e);
            callback.onFailure(e);
        }
    }

    private void processAgentEventApp(ToAgentNotificationMsg notification) {
        AgentAppEventNotificationProto agentAppEventNotification = notification.getAgentAppEventNotification();
        ctx.getAgentEventProcessor().onEventNotification(agentAppEventNotification);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {}
}
