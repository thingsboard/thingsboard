/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateExecutor;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.executors.SharedEventLoopGroupService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.script.JsExecutorService;
import org.thingsboard.server.service.script.JsInvokeService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ActorSystemContext {

    protected final ObjectMapper mapper = new ObjectMapper();

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Autowired
    @Getter
    @Setter
    private TbServiceInfoProvider serviceInfoProvider;

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Autowired
    @Getter
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Getter
    private DeviceService deviceService;

    @Autowired
    @Getter
    private AssetService assetService;

    @Autowired
    @Getter
    private DashboardService dashboardService;

    @Autowired
    @Getter
    private TenantService tenantService;

    @Autowired
    @Getter
    private CustomerService customerService;

    @Autowired
    @Getter
    private UserService userService;

    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    @Getter
    private TbClusterService clusterService;

    @Autowired
    @Getter
    private TimeseriesService tsService;

    @Autowired
    @Getter
    private AttributesService attributesService;

    @Autowired
    @Getter
    private EventService eventService;

    @Autowired
    @Getter
    private AlarmService alarmService;

    @Autowired
    @Getter
    private RelationService relationService;

    @Autowired
    @Getter
    private AuditLogService auditLogService;

    @Autowired
    @Getter
    private EntityViewService entityViewService;

    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Getter
    private JsInvokeService jsSandbox;

    @Autowired
    @Getter
    private JsExecutorService jsExecutor;

    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Autowired
    @Getter
    private MailService mailService;

    @Autowired
    @Getter
    private ClaimDevicesService claimDevicesService;

    @Autowired
    @Getter
    private JsInvokeStats jsInvokeStats;

    //TODO: separate context for TbCore and TbRuleEngine
    @Autowired(required = false)
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired(required = false)
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Autowired(required = false)
    @Getter
    private TbCoreToTransportService tbCoreToTransportService;

    /**
     * The following Service will be null if we operate in tb-core mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    /**
     * The following Service will be null if we operate in tb-rule-engine mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.rule.chain.error_persist_frequency}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency}")
    @Getter
    private long statisticsPersistFrequency;


    @Scheduled(fixedDelayString = "${actors.statistics.js_print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeStats.getRequests() > 0 || jsInvokeStats.getResponses() > 0 || jsInvokeStats.getFailures() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeStats.getRequests(), jsInvokeStats.getResponses(), jsInvokeStats.getFailures());
                jsInvokeStats.reset();
            }
        }
    }

    @Value("${actors.tenant.create_components_on_init}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${transport.sessions.inactivity_timeout}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled}")
    @Getter
    private boolean debugPerTenantEnabled;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    @Getter
    @Setter
    private TbActorSystem actorSystem;

    @Setter
    private TbActorRef appActor;

    @Getter
    @Setter
    private TbActorRef statsActor;

    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateExecutor cassandraBufferedRateExecutor;

    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    public ScheduledExecutorService getScheduler() {
        return actorSystem.getScheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.ERROR);
        event.setBody(toBodyJson(serviceInfoProvider.getServiceInfo().getServiceId(), method, toString(e)));
        persistEvent(event);
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.LC_EVENT);
        event.setBody(toBodyJson(serviceInfoProvider.getServiceInfo().getServiceId(), lcEvent, Optional.ofNullable(e)));
        persistEvent(event);
    }

    private void persistEvent(Event event) {
        eventService.save(event);
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private JsonNode toBodyJson(String serviceId, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", serviceId).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private JsonNode toBodyJson(String serviceId, String method, String body) {
        return mapper.createObjectNode().put("server", serviceId).put("method", method).put("error", body);
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, queueName, tenantId, entityId);
    }


    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null);
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null);
    }

    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                Event event = new Event();
                event.setTenantId(tenantId);
                event.setEntityId(entityId);
                event.setType(DataConstants.DEBUG_RULE_NODE);

                String metadata = mapper.writeValueAsString(tbMsg.getMetaData().getData());

                ObjectNode node = mapper.createObjectNode()
                        .put("type", type)
                        .put("server", getServiceId())
                        .put("entityId", tbMsg.getOriginator().getId().toString())
                        .put("entityName", tbMsg.getOriginator().getEntityType().name())
                        .put("msgId", tbMsg.getId().toString())
                        .put("msgType", tbMsg.getType())
                        .put("dataType", tbMsg.getDataType().name())
                        .put("relationType", relationType)
                        .put("data", tbMsg.getData())
                        .put("metadata", metadata);

                if (error != null) {
                    node = node.put("error", toString(error));
                }

                event.setBody(node);
                ListenableFuture<Event> future = eventService.saveAsync(event);
                Futures.addCallback(future, new FutureCallback<Event>() {
                    @Override
                    public void onSuccess(@Nullable Event event) {

                    }

                    @Override
                    public void onFailure(Throwable th) {
                        log.error("Could not save debug Event for Node", th);
                    }
                }, MoreExecutors.directExecutor());
            } catch (IOException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.DEBUG_RULE_CHAIN);

        ObjectNode node = mapper.createObjectNode()
                //todo: what fields are needed here?
                .put("server", getServiceId())
                .put("message", "Reached debug mode rate limit!");

        if (error != null) {
            node = node.put("error", toString(error));
        }

        event.setBody(node);
        ListenableFuture<Event> future = eventService.saveAsync(event);
        Futures.addCallback(future, new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event event) {

            }

            @Override
            public void onFailure(Throwable th) {
                log.error("Could not save debug Event for Rule Chain", th);
            }
        }, MoreExecutors.directExecutor());
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

    public void tell(TbActorMsg tbActorMsg) {
        appActor.tell(tbActorMsg);
    }

    public void tellWithHighPriority(TbActorMsg tbActorMsg) {
        appActor.tellWithHighPriority(tbActorMsg);
    }

    public void schedulePeriodicMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        log.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().scheduleWithFixedDelay(() -> ctx.tell(msg), delayInMs, periodInMs, TimeUnit.MILLISECONDS);
    }

    public void scheduleMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs) {
        log.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        getScheduler().schedule(() -> ctx.tell(msg), delayInMs, TimeUnit.MILLISECONDS);
    }
}
